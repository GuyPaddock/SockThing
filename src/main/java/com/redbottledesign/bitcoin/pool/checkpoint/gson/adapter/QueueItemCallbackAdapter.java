package com.redbottledesign.bitcoin.pool.checkpoint.gson.adapter;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.Excluder;
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.redbottledesign.bitcoin.pool.util.queue.QueueItemCallback;
import com.redbottledesign.bitcoin.pool.util.queue.QueueItemCallbackFactory;
import com.redbottledesign.util.gson.PolymorphicSerializerDeserializer;

public class QueueItemCallbackAdapter
extends PolymorphicSerializerDeserializer<QueueItemCallback<?>>
implements InstanceCreator<QueueItemCallback<?>>
{
    private final Collection<QueueItemCallbackFactory<?>> factories;
    private Gson gson;

    public QueueItemCallbackAdapter(Collection<QueueItemCallbackFactory<?>> factories)
    {
        super(QueueItemCallback.class);

        this.factories  = factories;
    }

    public void setGson(Gson gson)
    {
        this.gson = gson;
    }

    @Override
    public QueueItemCallback<?> deserialize(JsonElement json, Type typeOfObject, JsonDeserializationContext context)
    throws JsonParseException
    {
        QueueItemCallback<?> result = null;

        this.assertInitialized();

        if ((json != null) && (json.isJsonObject()))
        {
            JsonObject  jsonObject      = json.getAsJsonObject();
            String      objectClassName;
            Type        objectType;

            if (!jsonObject.has(PolymorphicSerializerDeserializer.JSON_FIELD_CLASS_NAME))
                throw new JsonParseException(String.format("Missing '%s' property in JSON.", JSON_FIELD_CLASS_NAME));

            objectClassName = jsonObject.get(PolymorphicSerializerDeserializer.JSON_FIELD_CLASS_NAME).getAsString();

            try
            {
                objectType = Class.forName(objectClassName);
            }

            catch (ClassNotFoundException ex)
            {
                throw new JsonParseException(
                    String.format(
                        "Entity class '%s' referenced in JSON was not found: %s",
                        objectClassName,
                        ex.getMessage()),
                    ex);
            }

            result = this.createAndPopulate(json, objectType);
        }

        return result;
    }

    @Override
    public QueueItemCallback<?> createInstance(Type type)
    {
        QueueItemCallback<?> result = null;

        for (QueueItemCallbackFactory<?> factory : this.factories)
        {
            result = factory.createCallback(type);

            if (result != null)
                break;
        }

        return result;
    }

    protected QueueItemCallback<?> createAndPopulate(JsonElement json, Type type)
    {
        QueueItemCallback<?>            result              = null;
        TypeAdapter<?>                  typeAdapter;
        Map<Type, InstanceCreator<?>>   instanceCreators    = new HashMap<>();
        ConstructorConstructor          constructor         = new ConstructorConstructor(instanceCreators);
        ReflectiveTypeAdapterFactory    typeAdapterFactory  =
            new ReflectiveTypeAdapterFactory(constructor, FieldNamingPolicy.IDENTITY, Excluder.DEFAULT);


        instanceCreators.put(type, this);

        typeAdapter = typeAdapterFactory.create(this.gson, TypeToken.get(type));
        result      = (QueueItemCallback<?>)typeAdapter.fromJsonTree(json);

        return result;
    }

    protected void assertInitialized()
    {
        if (this.gson == null)
        {
            throw new IllegalStateException(
                "This object must be provided a Gson object with setGson() before calling this method.");
        }
    }
}