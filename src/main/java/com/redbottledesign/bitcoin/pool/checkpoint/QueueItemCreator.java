package com.redbottledesign.bitcoin.pool.checkpoint;

import java.lang.reflect.Type;

import com.google.gson.InstanceCreator;
import com.redbottledesign.bitcoin.pool.agent.PersistenceAgent.QueueItem;
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
