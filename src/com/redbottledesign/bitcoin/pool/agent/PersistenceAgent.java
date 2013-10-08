package com.redbottledesign.bitcoin.pool.agent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.github.fireduck64.sockthing.EventLog;
import com.github.fireduck64.sockthing.StratumServer;
import com.redbottledesign.bitcoin.pool.PersistenceCallback;
import com.redbottledesign.bitcoin.pool.RequestorRegistry;
import com.redbottledesign.drupal.Entity;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;
import com.redbottledesign.drupal.gson.requestor.EntityRequestor;
import com.redbottledesign.util.QueueUtils;

public class PersistenceAgent
extends CheckpointableAgent<List<PersistenceAgent.PersistenceQueueItem<? extends Entity<?>>>>
{
  private final StratumServer server;
  private final EventLog logger;
  private final RequestorRegistry requestorRegistry;
  private final BlockingQueue<PersistenceQueueItem<? extends Entity<?>>> persistenceQueue;

  public PersistenceAgent(StratumServer server)
  {
    this.server             = server;
    this.logger             = server.getEventLog();
    this.requestorRegistry  = new RequestorRegistry(this.server);
    this.persistenceQueue   = new LinkedBlockingQueue<>();
  }

  public void queueForSave(Entity<?> entity)
  {
    this.queueForSave(entity, null);
  }

  public <T extends Entity<?>> void queueForSave(T entity, PersistenceCallback<T> callback)
  {
    QueueUtils.ensureQueued(this.persistenceQueue, new PersistenceQueueItem<T>(entity, callback));
  }

  public synchronized boolean evictQueueItem(long itemId)
  {
    return this.evictQueueItems(Collections.singleton(itemId));
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public synchronized boolean evictQueueItems(Set<Long> itemIds)
  {
    boolean                           atLeastOneItemVacated = false;
    Iterator<PersistenceQueueItem<?>> queueIterator;

    this.interruptQueueProcessing();

    queueIterator = this.persistenceQueue.iterator();

    while (queueIterator.hasNext())
    {
      PersistenceQueueItem  queueItem     = queueIterator.next();
      long                  itemId        = queueItem.getItemId();
      Entity                itemEntity    = queueItem.getEntity();
      PersistenceCallback   itemCallback  = queueItem.getCallback();

      if (itemIds.contains(itemId))
      {
        String logMessage =
          String.format(
            "Evicting persistence queue item upon request (queue item ID #: %d): %s",
            itemId,
            itemEntity);

        System.out.println(logMessage);
        this.logger.log(logMessage);

        queueIterator.remove();

        if (itemCallback != null)
          itemCallback.onEntityEvicted(itemEntity);

        atLeastOneItemVacated = true;
        break;
      }
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
      Iterator<PersistenceQueueItem<?>> queueIterator = this.persistenceQueue.iterator();
      String                            logMessage    = "Evicting all persistence queue item upon request.";

      System.out.println(logMessage);
      this.logger.log(logMessage);

      while (queueIterator.hasNext())
      {
        PersistenceQueueItem  queueItem     = queueIterator.next();
        PersistenceCallback   itemCallback  = queueItem.getCallback();

        queueIterator.remove();

        if (itemCallback != null)
          itemCallback.onEntityEvicted(queueItem.getEntity());
      }
    }

    return queueHasItems;
  }

  @Override
  public synchronized List<PersistenceQueueItem<? extends Entity<?>>> captureCheckpoint()
  {
    this.interruptQueueProcessing();

    return new ArrayList<>(this.persistenceQueue);
  }

  @Override
  public synchronized void restoreFromCheckpoint(List<PersistenceQueueItem<? extends Entity<?>>> checkpointItems)
  {
    Set<Long> checkpointItemIds = new HashSet<>();

    this.interruptQueueProcessing();

    for (PersistenceQueueItem<?> checkpointItem : checkpointItems)
    {
      checkpointItemIds.add(checkpointItem.getItemId());
    }

    this.evictQueueItems(checkpointItemIds);
    this.persistenceQueue.addAll(checkpointItems);
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  protected void runPeriodicTask()
  throws Exception
  {
    PersistenceQueueItem  queueItem;

    while ((queueItem = this.persistenceQueue.take()) != null)
    {
      long    queueItemId     = queueItem.getItemId();
      Entity  queueItemEntity = queueItem.getEntity();

      try
      {
        String logMessage =
          String.format(
            "Attempting to persist entity (queue item ID #: %d): %s",
            queueItemId,
            queueItemEntity);

        System.out.println(logMessage);
        this.logger.log(logMessage);

        this.attemptToPersistItem(queueItem);
      }

      catch (Throwable ex)
      {
        String error;

        // Re-queue entity for persistence
        QueueUtils.ensureQueued(this.persistenceQueue, queueItem);

        error =
          String.format(
            "Failed to persist entity (queue item ID #: %d, contents: '%s') (re-queued): %s",
            queueItemId,
            queueItemEntity,
            ex.getMessage());

        this.logger.log(error);

        throw new RuntimeException(error, ex);
      }
    }
  }

  protected <T extends Entity<?>> void attemptToPersistItem(PersistenceQueueItem<T> queueItem)
  throws IOException, DrupalHttpException
  {
    T                       queueEntity = queueItem.getEntity();
    PersistenceCallback<T>  callback    = queueItem.getCallback();
    EntityRequestor<T>      requestor   = requestorRegistry.getRequestorForEntity(queueEntity);

    if (queueEntity.isNew()) {
      requestor.saveNew(queueEntity);
    }
    else {
      requestor.update(queueEntity);
    }

    if (callback != null)
      callback.onEntitySaved(queueEntity);
  }

  protected static class PersistenceQueueItem<T extends Entity<?>>
  {
    private static volatile long itemIdCounter;

    private final long itemId;
    private final T entity;
    private final PersistenceCallback<T> callback;

    protected static synchronized long getNextIndex()
    {
      return itemIdCounter++;
    }

    public PersistenceQueueItem(T entity, PersistenceCallback<T> callback)
    {
      this.itemId   = getNextIndex();
      this.entity   = entity;
      this.callback = callback;
    }

    public long getItemId()
    {
      return this.itemId;
    }

    public T getEntity()
    {
      return this.entity;
    }

    public PersistenceCallback<T> getCallback()
    {
      return this.callback;
    }
  }

  protected synchronized void interruptQueueProcessing()
  {
    /* Interrupt to break out of persistenceQueue.take(); the persistence
     * thread should block in the run() method of Agent since it doesn't hold
     * the lock on this object.
     */
    this.interrupt();
  }
}