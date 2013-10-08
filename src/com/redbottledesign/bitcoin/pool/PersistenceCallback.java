package com.redbottledesign.bitcoin.pool;

import com.redbottledesign.drupal.Entity;

public interface PersistenceCallback<E extends Entity<?>>
{
  public void onEntitySaved(E savedEntity);
  public void onEntityEvicted(E evictedEntity);
}
