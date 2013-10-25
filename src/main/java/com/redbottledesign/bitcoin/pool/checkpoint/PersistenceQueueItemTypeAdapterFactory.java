package com.redbottledesign.bitcoin.pool.checkpoint;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.redbottledesign.bitcoin.pool.agent.PersistenceAgent.PersistenceQueueItem;
import com.redbottledesign.drupal.Entity;

public class PersistenceQueueItemTypeAdapterFactory
implements TypeAdapterFactory
{
    private static final String JSON_FIELD_ENTITY_TYPE = "entityType";

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public final <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type)
    {
        TypeAdapter<T> result = null;

        if (PersistenceQueueItem.class.isAssignableFrom(type.getRawType()))
        {
            TypeAdapter<T>              defaultDelegateAdapter  = gson.getDelegateAdapter(this, type);
            TypeAdapter<JsonElement>    elementAdapter          = gson.getAdapter(JsonElement.class);

            result = new PersistenceQueueItemTypeAdapter(gson, defaultDelegateAdapter, elementAdapter);
        }

        return result;
    }

    protected class PersistenceQueueItemTypeAdapter<T extends PersistenceQueueItem<?>>
    extends TypeAdapter<T>
    {
        private final Gson gson;
        private final TypeAdapter<T> defaultDelegateAdapter;
        private final TypeAdapter<JsonElement> elementAdapter;

        public PersistenceQueueItemTypeAdapter(Gson gson, TypeAdapter<T> defaultDelegateAdapter,
                                               TypeAdapter<JsonElement> elementAdapter)
        {
            this.gson                   = gson;
            this.defaultDelegateAdapter = defaultDelegateAdapter;
            this.elementAdapter         = elementAdapter;
        }

        @Override
        public void write(JsonWriter outJsonWriter, T value)
        throws IOException
        {
            JsonElement targetElement = this.defaultDelegateAdapter.toJsonTree(value);
            JsonObject  targetJsonObj = targetElement.getAsJsonObject();

            // Add a entity type class name property
            targetJsonObj.addProperty(JSON_FIELD_ENTITY_TYPE, value.getEntity().getClass().getName());

            this.elementAdapter.write(outJsonWriter, targetElement);
        }

        @Override
        public T read(JsonReader inJsonReader)
        throws IOException
        {
            JsonElement readElement         = this.elementAdapter.read(inJsonReader);
            JsonObject  readObject          = readElement.getAsJsonObject();
            String      entityClassName;
            Type        entityType;

            if (!readObject.has(JSON_FIELD_ENTITY_TYPE))
                throw new JsonParseException(String.format("Missing '%s' property in JSON.", JSON_FIELD_ENTITY_TYPE));

            entityClassName = readObject.get(JSON_FIELD_ENTITY_TYPE).getAsString();

            try
            {
                entityType = Class.forName(entityClassName);
            }

            catch (ClassNotFoundException ex)
            {
                throw new JsonParseException(
                    String.format(
                        "Entity class '%s' referenced in JSON was not found: %s",
                        entityClassName,
                        ex.getMessage()),
                    ex);
            }

            // Remove the type field, since it won't deserialize.
            readObject.remove(JSON_FIELD_ENTITY_TYPE);

            return this.gson.fromJson(readElement, new SpecificEntityType(entityType));
        }
    }

    protected static class SpecificEntityType
    implements ParameterizedType
    {
        private static final ParameterizedType ENTITY_TYPE_TOKEN =
            (ParameterizedType)new TypeToken<Entity<?>>(){}.getType();

        private final Type entityType;

        public SpecificEntityType(Type entityType)
        {
            this.entityType = entityType;
        }

        @Override
        public Type getRawType()
        {
            return ENTITY_TYPE_TOKEN.getRawType();
        }

        @Override
        public Type getOwnerType()
        {
            return ENTITY_TYPE_TOKEN.getOwnerType();
        }

        @Override
        public Type[] getActualTypeArguments()
        {
            return new Type[] { this.entityType };
        }
    };
}