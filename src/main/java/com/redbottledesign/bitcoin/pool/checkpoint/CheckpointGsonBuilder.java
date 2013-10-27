package com.redbottledesign.bitcoin.pool.checkpoint;

import java.util.Collection;
import java.util.LinkedList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.redbottledesign.bitcoin.pool.PersistenceCallback;
import com.redbottledesign.bitcoin.pool.PersistenceCallbackFactory;
import com.redbottledesign.bitcoin.pool.agent.PersistenceAgent.QueueItem;
import com.redbottledesign.bitcoin.pool.checkpoint.gson.adapter.PersistenceCallbackAdapter;
import com.redbottledesign.bitcoin.pool.checkpoint.gson.adapter.QueueItemCreator;
import com.redbottledesign.bitcoin.pool.checkpoint.gson.adapter.QueueItemTypeAdapterFactory;

public final class CheckpointGsonBuilder
{
    private static final CheckpointGsonBuilder INSTANCE = new CheckpointGsonBuilder();

    private final Collection<PersistenceCallbackFactory<?>> factories;
    private final Gson gson;

    public static CheckpointGsonBuilder getInstance()
    {
        return INSTANCE;
    }

    private CheckpointGsonBuilder()
    {
        this.factories  = new LinkedList<PersistenceCallbackFactory<?>>();
        this.gson       = this.createGson();
    }

    public Gson getGson()
    {
        return this.gson;
    }

    public void registerCallbackFactory(PersistenceCallbackFactory<?> callbackFactory)
    {
        this.factories.add(callbackFactory);
    }

    public void unregisterCallbackFactory(PersistenceCallbackFactory<?> callbackFactory)
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
                .registerTypeAdapterFactory(new QueueItemTypeAdapterFactory())
                .registerTypeAdapter(QueueItem.class, new QueueItemCreator())
                .registerTypeAdapter(PersistenceCallback.class, persistenceCallbackAdapter)
                .create();

        persistenceCallbackAdapter.setGson(gson);

        return gson;
    }
}