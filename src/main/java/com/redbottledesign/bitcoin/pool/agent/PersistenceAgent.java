package com.redbottledesign.bitcoin.pool.agent;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.Config;
import com.github.fireduck64.sockthing.StratumServer;
import com.google.gson.reflect.TypeToken;
import com.redbottledesign.bitcoin.pool.PersistenceCallback;
import com.redbottledesign.bitcoin.pool.RequestorRegistry;
import com.redbottledesign.bitcoin.pool.checkpoint.CheckpointItem;
import com.redbottledesign.drupal.Entity;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;
import com.redbottledesign.drupal.gson.requestor.EntityRequestor;
import com.redbottledesign.drupal.gson.requestor.HttpClientFactory;
import com.redbottledesign.util.QueueUtils;

public class PersistenceAgent
extends CheckpointableAgent
implements EvictableQueue<Long>
{
    private static final String CONFIG_VALUE_PERSISTENCE_THREAD_COUNT = "persistence_threads";
    private static final int MAX_EXTRA_HTTP_CONNECTIONS = 5;
    private static final int DEFAULT_PERSISTENCE_THREAD_COUNT = 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceAgent.class);

    private final StratumServer server;
    private final RequestorRegistry requestorRegistry;
    private final BlockingQueue<QueueItem<? extends Entity<?>>> persistenceQueue;

    private int numPersistenceThreads;

    public PersistenceAgent(StratumServer server)
    {
        this.server                 = server;
        this.requestorRegistry      = new RequestorRegistry(this.server);
        this.persistenceQueue       = new LinkedBlockingQueue<>();

        this.setNumPersistenceThreads(DEFAULT_PERSISTENCE_THREAD_COUNT);
    }

    @Override
    public synchronized void start()
    {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Starting persistence threads...");

        // Not loadConfig(), since that's called when each thread starts.
        this.loadThreadConfig();

        /* We actually don't start ourselves as a single thread. We spin off
         * multiple threads on this same object.
         */
        for (int threadIndex = 0; threadIndex < this.numPersistenceThreads; ++threadIndex)
        {
            String threadName        = this.getName() + " #" + (threadIndex + 1);
            Thread persistenceThread = new Thread(this, threadName);

            if (LOGGER.isInfoEnabled())
                LOGGER.info(String.format("  Persistence thread %s starting.", threadName));

            persistenceThread.start();
        }

        if (LOGGER.isInfoEnabled())
            LOGGER.info("Persistence threads started.");
    }

    protected void setNumPersistenceThreads(int numPersistenceThreads)
    {
        PoolingHttpClientConnectionManager connectionManager = HttpClientFactory.getInstance().getConnectionManager();
        int                                maxConnections    = numPersistenceThreads + MAX_EXTRA_HTTP_CONNECTIONS;

        this.numPersistenceThreads = numPersistenceThreads;

        // Scale HTTP connection pool size accordingly.
        connectionManager.setMaxTotal(maxConnections);
        connectionManager.setDefaultMaxPerRoute(maxConnections);
    }

    protected void loadThreadConfig()
    {
        Config config = this.server.getConfig();

        if (config.isSet(CONFIG_VALUE_PERSISTENCE_THREAD_COUNT))
        {
            int configThreadCount = config.getInt(CONFIG_VALUE_PERSISTENCE_THREAD_COUNT);

            if ((configThreadCount < 1) || (configThreadCount > 16))
            {
                if (LOGGER.isErrorEnabled())
                {
                    LOGGER.error(
                        String.format(
                            "Config %s is invalid (was %d but must be between 1 and 16). Number of persistence " +
                            "threads is using the default of %d.",
                            CONFIG_VALUE_PERSISTENCE_THREAD_COUNT,
                            configThreadCount,
                            DEFAULT_PERSISTENCE_THREAD_COUNT));
                }

                this.setNumPersistenceThreads(DEFAULT_PERSISTENCE_THREAD_COUNT);
            }

            else
            {
                this.setNumPersistenceThreads(configThreadCount);
            }
        }

        else
        {
            if (LOGGER.isErrorEnabled())
            {
                LOGGER.error(
                    String.format(
                        "Config %s not found. Number of persistence threads is using the default of %d.",
                        CONFIG_VALUE_PERSISTENCE_THREAD_COUNT,
                        DEFAULT_PERSISTENCE_THREAD_COUNT));

                this.setNumPersistenceThreads(DEFAULT_PERSISTENCE_THREAD_COUNT);
            }
        }
    }

    public void queueForSave(Entity<?> entity)
    {
        this.queueForSave(entity, null);
    }

    public <T extends Entity<?>> void queueForSave(T entity, PersistenceCallback<T> callback)
    {
        QueueItem<T> queueItem = new QueueItem<T>(entity, callback);

        this.notifyCheckpointListenersOnItemCreated(queueItem);
        QueueUtils.ensureQueued(this.persistenceQueue, queueItem);
    }

    @Override
    public synchronized boolean evictQueueItem(Long itemId)
    {
        return this.evictQueueItems(Collections.singleton(itemId));
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public synchronized boolean evictQueueItems(Set<Long> itemIds)
    {
        boolean                 atLeastOneItemEvicted = false;
        Iterator<QueueItem<?>>  queueIterator;

        this.interruptQueueProcessing();

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Evicting items from persistence queue: " + itemIds);

        queueIterator = this.persistenceQueue.iterator();

        while (queueIterator.hasNext())
        {
            QueueItem           queueItem       = queueIterator.next();
            long                itemId          = queueItem.getItemId();
            Entity              itemEntity      = queueItem.getEntity();
            PersistenceCallback itemCallback    = queueItem.getCallback();

            if (itemIds.contains(itemId))
            {
                if (LOGGER.isInfoEnabled())
                {
                    LOGGER.info(
                        String.format(
                            "Evicting persistence queue item upon request (queue item ID #: %d): %s",
                            itemId,
                            itemEntity));
                }

                queueIterator.remove();

                if (itemCallback != null)
                    itemCallback.onEntityEvicted(itemEntity);

                atLeastOneItemEvicted = true;
                break;
            }
        }

        if (LOGGER.isInfoEnabled())
        {
            if (atLeastOneItemEvicted)
                LOGGER.info("evictQueueItems() was called and at least one item was evicted.");

            else
                LOGGER.info("evictQueueItems() was called, but no items were evicted.");
        }

        return atLeastOneItemEvicted;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public synchronized boolean evictAllQueueItems()
    {
        boolean queueHasItems;

        this.interruptQueueProcessing();

        queueHasItems = !this.persistenceQueue.isEmpty();

        if (queueHasItems)
        {
            Iterator<QueueItem<?>> queueIterator = this.persistenceQueue.iterator();

            if (LOGGER.isInfoEnabled())
                LOGGER.info("Evicting all persistence queue item upon request.");

            while (queueIterator.hasNext())
            {
                QueueItem           queueItem       = queueIterator.next();
                PersistenceCallback itemCallback    = queueItem.getCallback();

                queueIterator.remove();

                if (itemCallback != null)
                    itemCallback.onEntityEvicted(queueItem.getEntity());
            }
        }

        return queueHasItems;
    }

    public synchronized boolean hasQueuedItemMatchingSieve(PersistenceAgent.QueueItemSieve sieve)
    {
        boolean result = false;

        for (QueueItem<? extends Entity<?>> queueItem : this.persistenceQueue)
        {
            if (sieve.matches(queueItem))
            {
                result = true;
                break;
            }
        }

        return result;
    }

    @Override
    public Type getCheckpointItemType()
    {
        return new TypeToken<QueueItem<?>>(){}.getType();
    }

    @Override
    public synchronized Collection<? extends CheckpointItem> captureCheckpoint()
    {
        this.interruptQueueProcessing();

        if (LOGGER.isInfoEnabled())
        {
            LOGGER.info(
                String.format("Capturing checkpoint of %d persistence queue items.", this.persistenceQueue.size()));
        }

        return new ArrayList<>(this.persistenceQueue);
    }

    @Override
    public synchronized void restoreFromCheckpoint(Collection<? extends CheckpointItem> checkpoint)
    {
        Set<Long>           checkpointItemIds   = new HashSet<>();
        List<QueueItem<?>>  newQueueItems       = new ArrayList<>(checkpoint.size());

        if (LOGGER.isInfoEnabled())
            LOGGER.info(String.format("Restoring %d items from a checkpoint.", checkpoint.size()));

        this.interruptQueueProcessing();

        for (CheckpointItem checkpointItem : checkpoint)
        {
            QueueItem<?> queueItem;

            if (!(checkpointItem instanceof QueueItem))
            {
                if (LOGGER.isErrorEnabled())
                {
                    LOGGER.error(
                        "A checkpoint item was provided that is not a PersistenceQueueItem (ignoring): " +
                        checkpointItem);
                }
            }

            else
            {
                queueItem = (QueueItem<?>)checkpointItem;

                checkpointItemIds.add(queueItem.getItemId());
                newQueueItems.add(queueItem);
            }
        }

        // Only evict items we're replacing from the checkpoint.
        this.evictQueueItems(checkpointItemIds);

        this.persistenceQueue.addAll(newQueueItems);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void runPeriodicTask()
    throws Exception
    {
        QueueItem queueItem;

        while ((queueItem = this.persistenceQueue.take()) != null)
        {
            long    queueItemId     = queueItem.getItemId();
            Entity  queueItemEntity = queueItem.getEntity();

            try
            {
                if (LOGGER.isInfoEnabled())
                {
                    LOGGER.info(
                        String.format(
                            "Attempting to persist entity (queue item ID #: %d, entity type: %s, bundle type: %s).",
                            queueItemId,
                            queueItemEntity.getEntityType(),
                            queueItemEntity.getBundleType()));
                }

                if (LOGGER.isDebugEnabled())
                {
                    LOGGER.debug(
                        String.format("Entity details (queue item ID #: %d): %s", queueItemId, queueItemEntity));
                }

                this.attemptToPersistItem(queueItem);
            }

            catch (Throwable ex)
            {
                String error;

                // Re-queue entity for persistence
                QueueUtils.ensureQueued(this.persistenceQueue, queueItem);

                error =
                    String.format(
                        "Failed to persist entity (queue item ID #: %d, entity type: %s, bundle type: %s; " +
                        "has been re-queued): %s",
                        queueItemId,
                        queueItemEntity.getEntityType(),
                        queueItemEntity.getBundleType(),
                        ex.getMessage());

                if (LOGGER.isErrorEnabled())
                {
                    LOGGER.error(error);
                }

                if (LOGGER.isDebugEnabled())
                {
                    LOGGER.debug(
                        String.format(
                            "Contents of entity failing persistence (queue item ID #: %d): %s",
                            queueItemId,
                            queueItemEntity));
                }

                throw new RuntimeException(error, ex);
            }
        }
    }

    protected <T extends Entity<?>> void attemptToPersistItem(QueueItem<T> queueItem)
    throws IOException, DrupalHttpException
    {
        T                       queueEntity = queueItem.getEntity();
        PersistenceCallback<T>  callback    = queueItem.getCallback();
        EntityRequestor<T>      requestor   = requestorRegistry.getRequestorForEntity(queueEntity);

        /// FIXME: REMOVE. THIS IS FOR TESTING.
//        Object object = null;
//
//        object.toString();
        /// END FIXME

        if (queueEntity.isNew())
        {
            requestor.saveNew(queueEntity);
        }

        else
        {
            requestor.update(queueEntity);
        }

        if (callback != null)
            callback.onEntitySaved(queueEntity);

        /* BUG BUG: If the entity saved but the callback failed, we're going to
         *          end up trying to save the entity all over again...
         */
        this.notifyCheckpointListenersOnItemExpired(queueItem);
    }

    protected synchronized void interruptQueueProcessing()
    {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Queue processing has been temporarily interrupted.");

        /*
         * Interrupt to break out of persistenceQueue.take(); the persistence
         * thread should block in the run() method of Agent since it doesn't
         * hold the lock on this object.
         */
        this.interrupt();
    }

    public static class QueueItem<T extends Entity<?>>
    implements CheckpointItem
    {
        private static volatile long itemIdCounter;

        private final long itemId;
        private final long timestamp;
        private final T entity;
        private final PersistenceCallback<T> callback;

        protected static synchronized long getNextIndex()
        {
            return itemIdCounter++;
        }

        public QueueItem(T entity, PersistenceCallback<T> callback)
        {
            this.itemId     = getNextIndex();
            this.timestamp  = new Date().getTime();
            this.entity     = entity;
            this.callback   = callback;
        }

        public long getItemId()
        {
            return this.itemId;
        }

        public long getTimestamp()
        {
            return this.timestamp;
        }

        public T getEntity()
        {
            return this.entity;
        }

        public PersistenceCallback<T> getCallback()
        {
            return this.callback;
        }

        @Override
        public String getCheckpointId()
        {
            String entityIdString = "new";

            if (this.entity != null)
            {
                Integer entityId = this.entity.getId();

                if (entityId != null)
                    entityIdString = entityId.toString();
            }

            return String.format("%d-%d-%s", this.getTimestamp(), this.getItemId(), entityIdString);
        }

        @Override
        public String getCheckpointType()
        {
            return this.entity.getEntityType() + File.separator + this.entity.getBundleType();
        }
    }

    public interface QueueItemSieve
    {
        public boolean matches(QueueItem<? extends Entity<?>> queueItem);
    }
}