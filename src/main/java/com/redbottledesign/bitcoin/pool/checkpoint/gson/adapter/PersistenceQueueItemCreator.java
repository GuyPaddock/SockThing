package com.redbottledesign.bitcoin.pool.checkpoint.gson.adapter;

import java.lang.reflect.Type;

import com.google.gson.InstanceCreator;
import com.redbottledesign.bitcoin.pool.agent.PersistenceAgent;
import com.redbottledesign.drupal.Entity;

public class PersistenceQueueItemCreator
implements InstanceCreator<PersistenceAgent.QueueItem<Entity<?>>>
{
    @Override
    public PersistenceAgent.QueueItem<Entity<?>> createInstance(Type type)
    {
        return new PersistenceAgent.QueueItem<Entity<?>>(null, null);
    }
}
