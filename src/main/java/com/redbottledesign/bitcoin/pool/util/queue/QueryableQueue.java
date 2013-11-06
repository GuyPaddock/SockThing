package com.redbottledesign.bitcoin.pool.util.queue;

import java.util.Collection;

import com.redbottledesign.drupal.Entity;

public interface QueryableQueue
{
    public abstract boolean hasItemMatchingSieve(QueueItemSieve sieve);
    public <T extends Entity<?>> Collection<T> getItemsMatchingSieve(Class<T> entityType, QueueItemSieve sieve);
}