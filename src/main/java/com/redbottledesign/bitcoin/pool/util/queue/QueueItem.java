package com.redbottledesign.bitcoin.pool.util.queue;

import java.io.File;
import java.util.Date;

import com.redbottledesign.bitcoin.pool.checkpoint.CheckpointItem;
import com.redbottledesign.drupal.Entity;

public class QueueItem<T extends Entity<?>>
implements CheckpointItem
{
    private static volatile long itemIdCounter;

    private final long itemId;
    private final long timestamp;
    private final T entity;
    private final QueueItemCallback<T> callback;
    private int failCount;

    protected static synchronized long getNextIndex()
    {
        return itemIdCounter++;
    }

    public QueueItem(T entity, QueueItemCallback<T> callback)
    {
        this.itemId     = getNextIndex();
        this.timestamp  = new Date().getTime();
        this.entity     = entity;
        this.callback   = callback;
        this.failCount  = 0;
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

    public QueueItemCallback<T> getCallback()
    {
        return this.callback;
    }

    public int getFailCount()
    {
        return this.failCount;
    }

    public boolean hasPreviouslyFailed()
    {
        return (this.failCount != 0);
    }

    public void incrementFailCount()
    {
        ++this.failCount;
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

    @Override
    public String toString()
    {
        return "QueueItem ["    +
               "itemId="        + this.itemId    + ", " +
               "timestamp="     + this.timestamp + ", " +
               "entity="        + this.entity    + ", " +
               "callback="      + this.callback  + ", " +
               "failCount="     + this.failCount +
               "]";
    }
}