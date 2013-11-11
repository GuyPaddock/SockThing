package com.redbottledesign.bitcoin.pool.util.queue;

import com.redbottledesign.drupal.Entity;

public interface QueueItemCallback<E extends Entity<?>>
{
  public void onEntityProcessed(E savedEntity);
  public void onEntityEvicted(E evictedEntity);
}
