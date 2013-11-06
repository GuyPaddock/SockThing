package com.redbottledesign.bitcoin.pool.util.queue;

import java.util.Set;

public interface EvictableQueue<I>
{
    public abstract boolean evictQueueItem(I itemId);
    public abstract boolean evictQueueItems(Set<I> queueItemIds);
    public abstract boolean evictAllQueueItems();
}