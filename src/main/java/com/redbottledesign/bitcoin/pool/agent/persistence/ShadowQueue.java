package com.redbottledesign.bitcoin.pool.agent.persistence;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redbottledesign.bitcoin.pool.util.queue.QueryableQueue;
import com.redbottledesign.bitcoin.pool.util.queue.QueueItem;
import com.redbottledesign.bitcoin.pool.util.queue.QueueItemSieve;
import com.redbottledesign.drupal.Entity;

class ShadowQueue
implements QueryableQueue
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ShadowQueue.class);

    private final LinkedHashSet<QueueItem<? extends Entity<?>>> queueItems;
    private final ReadWriteLock locks;

    public ShadowQueue()
    {
        this.queueItems = new LinkedHashSet<>();
        this.locks      = new ReentrantReadWriteLock(true);
    }

    public void addItem(QueueItem<? extends Entity<?>> item)
    {
        this.locks.writeLock().lock();

        try
        {
            if (LOGGER.isTraceEnabled())
                LOGGER.trace("addItem(): item being added to shadow copy of the queue: " + item);

            this.queueItems.add(item);
        }

        finally
        {
            this.locks.writeLock().unlock();
        }
    }

    public void addAllItems(Collection<QueueItem<? extends Entity<?>>> items)
    {
        this.locks.writeLock().lock();

        try
        {
            if (LOGGER.isTraceEnabled())
                LOGGER.trace("addAllItems(): items being added to shadow copy of the queue: " + items);

            this.queueItems.addAll(items);
        }

        finally
        {
            this.locks.writeLock().unlock();
        }
    }

    public void removeItem(QueueItem<? extends Entity<?>> item)
    {
        this.locks.writeLock().lock();

        try
        {
            if (LOGGER.isTraceEnabled())
                LOGGER.trace("removeItem(): item being removed from shadow copy of the queue: " + item);

            this.queueItems.remove(item);
        }

        finally
        {
            this.locks.writeLock().unlock();
        }
    }

    public void removeAllItems(List<QueueItem<?>> items)
    {
        this.locks.writeLock().lock();

        try
        {
            if (LOGGER.isTraceEnabled())
                LOGGER.trace("removeAllItems(): items being removed from shadow copy of the queue: " + items);

            this.queueItems.removeAll(items);
        }

        finally
        {
            this.locks.writeLock().unlock();
        }
    }

    @Override
    public boolean hasItemMatchingSieve(QueueItemSieve sieve)
    {
        boolean result = false;

        this.locks.readLock().lock();

        try
        {
            if (LOGGER.isTraceEnabled())
            {
                LOGGER.trace(
                    "hasItemMatchingSieve(): Checking shadow queue for items matching sieve: " +
                    sieve.getClass().getName());

                LOGGER.trace("hasItemMatchingSieve(): Shadow queue contains: " + this.queueItems);
            }

            for (QueueItem<? extends Entity<?>> queueItem : this.queueItems)
            {
                if (sieve.matches(queueItem))
                {
                    result = true;
                    break;
                }
            }

            if (LOGGER.isTraceEnabled())
                LOGGER.trace("hasItemMatchingSieve(): Result: " + result);
        }

        finally
        {
            this.locks.readLock().unlock();
        }

        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity<?>> Collection<T> getItemsMatchingSieve(Class<T> entityType,
                                                                     QueueItemSieve sieve)
    {
        Collection<T> results = new LinkedList<T>();

        this.locks.readLock().lock();

        try
        {
            if (LOGGER.isTraceEnabled())
            {
                LOGGER.trace(
                    "getItemsMatchingSieve(): Selecting items from shadow queue that match sieve: " +
                    sieve.getClass().getName());

                LOGGER.trace("getItemsMatchingSieve(): Shadow queue contains: " + this.queueItems);
            }

            for (QueueItem<? extends Entity<?>> queueItem : this.queueItems)
            {
                if (sieve.matches(queueItem))
                    results.add((T)queueItem.getEntity());
            }

            if (LOGGER.isTraceEnabled())
                LOGGER.trace("getItemsMatchingSieve(): Matching item results: " + results);
        }

        finally
        {
            this.locks.readLock().unlock();
        }

        return results;
    }
}