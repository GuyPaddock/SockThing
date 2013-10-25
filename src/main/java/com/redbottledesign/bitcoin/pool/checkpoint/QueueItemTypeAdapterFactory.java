package com.redbottledesign.bitcoin.pool.checkpoint;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.redbottledesign.bitcoin.pool.agent.PersistenceAgent.QueueItem;
import com.redbottledesign.util.LateBoundParameterizedType;

public class QueueItemTypeAdapterFactory
implements TypeAdapterFactory
{
    private static final String JSON_FIELD_ENTITY_TYPE = "entityType";

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public final <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type)
    {
        TypeAdapter<T> result           = null;
        Type           underlyingType   = type.getType();

        if (QueueItem.class.isAssignableFrom(type.getRawType()) && (underlyingType instanceof ParameterizedType))
        {
            Type[] typeArguments = ((ParameterizedType)underlyingType).getActualTypeArguments();

            /* This is a bit of a hack, but this is the only foolproof way to
             * know we're dealing with the unbound version.
             */
            if ((typeArguments.length == 1) && (typeArguments[0] instanceof WildcardType))
            {
                TypeAdapter<T>              defaultDelegateAdapter  = gson.getDelegateAdapter(this, type);
                TypeAdapter<JsonElement>    elementAdapter          = gson.getAdapter(JsonElement.class);

                result = new WildcardQueueItemTypeAdapter(gson, defaultDelegateAdapter, elementAdapter);
            }
        }

        return result;
    }

    protected class WildcardQueueItemTypeAdapter<T extends QueueItem<?>>
    extends TypeAdapter<T>
    {
        private final Gson gson;
        private final TypeAdapter<T> defaultDelegateAdapter;
        private final TypeAdapter<JsonElement> elementAdapter;

        public WildcardQueueItemTypeAdapter(Gson gson, TypeAdapter<T> defaultDelegateAdapter,
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
            JsonElement readQueueItemElement    = this.elementAdapter.read(inJsonReader);
            JsonObject  readQueueItemObject     = readQueueItemElement.getAsJsonObject();
            String      entityClassName;
            Type        entityType;

            if (!readQueueItemObject.has(JSON_FIELD_ENTITY_TYPE))
                throw new JsonParseException(String.format("Missing '%s' property in JSON.", JSON_FIELD_ENTITY_TYPE));

            entityClassName = readQueueItemObject.get(JSON_FIELD_ENTITY_TYPE).getAsString();

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

            // Remove the type field, since it won't de-serialize.
            readQueueItemObject.remove(JSON_FIELD_ENTITY_TYPE);

            return this.gson.fromJson(
                readQueueItemElement,
                new LateBoundParameterizedType(
                    (ParameterizedType)(new TypeToken<QueueItem<?>>(){}.getType()),
                    entityType));
        }
    }
}