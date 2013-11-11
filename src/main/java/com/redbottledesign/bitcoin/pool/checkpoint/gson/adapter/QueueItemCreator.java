package com.redbottledesign.bitcoin.pool.checkpoint.gson.adapter;

import java.lang.reflect.Type;

import com.google.gson.InstanceCreator;
import com.redbottledesign.bitcoin.pool.util.queue.QueueItem;
import com.redbottledesign.drupal.Entity;

public class QueueItemCreator
implements InstanceCreator<QueueItem<Entity<?>>>
{
    @Override
    public QueueItem<Entity<?>> createInstance(Type type)
    {
        return new QueueItem<Entity<?>>(null, null);
    }
}
