package com.redbottledesign.bitcoin.pool.util.queue;

import com.redbottledesign.drupal.Entity;

public interface QueueItemSieve
{
    public boolean matches(QueueItem<? extends Entity<?>> queueItem);
}