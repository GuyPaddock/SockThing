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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.StratumServer;
import com.google.gson.reflect.TypeToken;
import com.redbottledesign.bitcoin.pool.PersistenceCallback;
import com.redbottledesign.bitcoin.pool.RequestorRegistry;
import com.redbottledesign.bitcoin.pool.checkpoint.CheckpointItem;
import com.redbottledesign.drupal.Entity;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;
import com.redbottledesign.drupal.gson.requestor.EntityRequestor;
import com.redbottledesign.util.QueueUtils;

public class PersistenceAgent
extends CheckpointableAgent
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceAgent.class);

    private final StratumServer server;
    private final RequestorRegistry requestorRegistry;
    private final BlockingQueue<QueueItem<? extends Entity<?>>> persistenceQueue;

    public PersistenceAgent(StratumServer server)
    {
        this.server             = server;
        this.requestorRegistry  = new RequestorRegistry(this.server);
        this.persistenceQueue   = new LinkedBlockingQueue<>();
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

    public synchronized boolean evictQueueItem(long itemId)
    {
        return this.evictQueueItems(Collections.singleton(itemId));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public synchronized boolean evictQueueItems(Set<Long> itemIds)
    {
        boolean                 atLeastOneItemVacated = false;
        Iterator<QueueItem<?>>  queueIterator;

        this.interruptQueueProcessing();

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Vacating items from persistence queue: " + itemIds);
        }

        queueIterator = this.persistenceQueue.iterator();

        while (queueIterator.hasNext())
        {
            QueueItem    queueItem       = queueIterator.next();
            long                    itemId          = queueItem.getItemId();
            Entity                  itemEntity      = queueItem.getEntity();
            PersistenceCallback     itemCallback    = queueItem.getCallback();

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

                atLeastOneItemVacated = true;
                break;
            }
        }

        if (LOGGER.isInfoEnabled())
        {
            if (atLeastOneItemVacated)
                LOGGER.info("evictQueueItems() was called and at least one item was vacated.");

            else
                LOGGER.info("evictQueueItems() was called, but no items were vacated.");
        }

        return atLeastOneItemVacated;
    }

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
                QueueItem    queueItem       = queueIterator.next();
                PersistenceCallback     itemCallback    = queueItem.getCallback();

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

        return new ArrayList<>(this.persistenceQueue);
    }

    @Override
    public synchronized void restoreFromCheckpoint(Collection<? extends CheckpointItem> checkpointItems)
    {
        Set<Long>           checkpointItemIds   = new HashSet<>();
        List<QueueItem<?>>  newQueueItems       = new ArrayList<>(checkpointItems.size());

        this.interruptQueueProcessing();

        for (CheckpointItem checkpointItem : checkpointItems)
        {
            QueueItem<?> queueItem;

            if (!(checkpointItem instanceof QueueItem))
            {
                if (LOGGER.isErrorEnabled())
                {
                    LOGGER.error(
                        "A checkpoint was provided that is not a PersistenceQueueItem (ignoring): " + checkpointItem);
                }
            }

            else
            {
                queueItem = (QueueItem<?>)checkpointItem;

                checkpointItemIds.add(queueItem.getItemId());
                newQueueItems.add(queueItem);
            }
        }

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

        this.notifyCheckpointListenersOnItemExpired(queueItem);

        if (callback != null)
            callback.onEntitySaved(queueEntity);
    }

    protected synchronized void interruptQueueProcessing()
    {
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