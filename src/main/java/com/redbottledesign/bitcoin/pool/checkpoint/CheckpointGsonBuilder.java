package com.redbottledesign.bitcoin.pool.checkpoint;

import java.util.Collection;
import java.util.LinkedList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.redbottledesign.bitcoin.pool.checkpoint.gson.adapter.PersistenceCallbackAdapter;
import com.redbottledesign.bitcoin.pool.checkpoint.gson.adapter.PersistenceQueueItemCreator;
import com.redbottledesign.bitcoin.pool.checkpoint.gson.adapter.PersistenceQueueItemTypeAdapterFactory;
import com.redbottledesign.bitcoin.pool.util.queue.QueueItem;
import com.redbottledesign.bitcoin.pool.util.queue.QueueItemCallback;
import com.redbottledesign.bitcoin.pool.util.queue.QueueItemCallbackFactory;

public final class CheckpointGsonBuilder
{
    private static final CheckpointGsonBuilder INSTANCE = new CheckpointGsonBuilder();

    private final Collection<QueueItemCallbackFactory<?>> factories;
    private final Gson gson;

    public static CheckpointGsonBuilder getInstance()
    {
        return INSTANCE;
    }

    private CheckpointGsonBuilder()
    {
        this.factories  = new LinkedList<QueueItemCallbackFactory<?>>();
        this.gson       = this.createGson();
    }

    public Gson getGson()
    {
        return this.gson;
    }

    public void registerPersistenceCallbackFactory(QueueItemCallbackFactory<?> callbackFactory)
    {
        this.factories.add(callbackFactory);
    }

    public void unregisterPersistenceCallbackFactory(QueueItemCallbackFactory<?> callbackFactory)
    {
        this.factories.remove(callbackFactory);
    }

    private Gson createGson()
    {
        Gson                        gson;
        PersistenceCallbackAdapter  persistenceCallbackAdapter = new PersistenceCallbackAdapter(this.factories);

        gson =
            new GsonBuilder()
                .serializeNulls()
                .setPrettyPrinting()
                .registerTypeAdapterFactory(new PersistenceQueueItemTypeAdapterFactory())
                .registerTypeAdapter(QueueItem.class, new PersistenceQueueItemCreator())
                .registerTypeAdapter(QueueItemCallback.class, persistenceCallbackAdapter)
                .create();

        persistenceCallbackAdapter.setGson(gson);

        return gson;
    }
}