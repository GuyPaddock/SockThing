package com.redbottledesign.bitcoin.pool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.github.fireduck64.sockthing.EventLog;
import com.github.fireduck64.sockthing.StratumServer;
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
  private final Set<Long> vacatedItemIds;

  public PersistenceAgent(StratumServer server)
  {
    this.server             = server;
    this.logger             = server.getEventLog();
    this.requestorRegistry  = new RequestorRegistry(this.server);
    this.persistenceQueue   = new LinkedBlockingQueue<>();
    this.vacatedItemIds     = new HashSet<Long>();
  }

  public void queueForSave(Entity<?> entity)
  {
    this.queueForSave(entity, null);
  }

  public <T extends Entity<?>> void queueForSave(T entity, PersistenceCallback<T> callback)
  {
    QueueUtils.ensureQueued(this.persistenceQueue, new PersistenceQueueItem<T>(entity, callback));
  }

  public void vacateItem(long queueItemId)
  {
    synchronized (this.vacatedItemIds)
    {
      this.vacatedItemIds.add(queueItemId);
    }
  }

  @Override
  public List<PersistenceQueueItem<? extends Entity<?>>> captureCheckpoint()
  {
    return new ArrayList<>(this.persistenceQueue);
  }

  @Override
  public void restoreFromCheckpoint(List<PersistenceQueueItem<? extends Entity<?>>> checkpoint)
  {
    this.persistenceQueue.addAll(checkpoint);
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
            "Attempting to persist entity (queue item ID #: %d): %s.",
            queueItemId,
            queueItemEntity.getClass().getSimpleName());

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
    if (!this.wasItemVacated(queueItem))
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
  }

  protected boolean wasItemVacated(PersistenceQueueItem<?> queueItem)
  {
    return this.wasItemVacated(queueItem, true);
  }

  protected boolean wasItemVacated(PersistenceQueueItem<?> queueItem, boolean clearStatus)
  {
    long    itemId          = queueItem.getItemId();
    boolean itemWasVacated;

    synchronized (this.vacatedItemIds)
    {
      if (this.vacatedItemIds.contains(queueItem.getItemId()))
      {
        itemWasVacated = true;

        if (clearStatus)
          this.vacatedItemIds.remove(itemId);
      }

      else
      {
        itemWasVacated = false;
      }
    }

    return itemWasVacated;
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
}