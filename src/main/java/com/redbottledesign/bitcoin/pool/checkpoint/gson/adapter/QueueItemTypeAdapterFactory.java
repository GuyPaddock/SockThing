package com.redbottledesign.bitcoin.pool.checkpoint.gson.adapter;

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
import com.redbottledesign.bitcoin.pool.util.queue.QueueItem;
import com.redbottledesign.util.LateBoundParameterizedType;

public class QueueItemTypeAdapterFactory
implements TypeAdapterFactory
{
    private static final String JSON_FIELD_ENTITY_TYPE = "entityType";

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public final <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type)
    {
        TypeAdapter<T> result = null;

        if (QueueItem.class.isAssignableFrom(type.getRawType()))
        {
            TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);

            result = new QueueItemTypeAdapter(gson, type, elementAdapter);
        }

        return result;
    }

    protected class QueueItemTypeAdapter<T extends QueueItem<?>>
    extends TypeAdapter<T>
    {
        private final Gson gson;
        private final TypeToken<T> elementType;
        private final TypeAdapter<JsonElement> elementAdapter;

        public QueueItemTypeAdapter(Gson gson, TypeToken<T> elementType, TypeAdapter<JsonElement> elementAdapter)
        {
            this.gson           = gson;
            this.elementType    = elementType;
            this.elementAdapter = elementAdapter;
        }

        @Override
        public void write(JsonWriter jsonWriter, T value)
        throws IOException
        {
            TypeAdapter<T>  delegatingAdapter   = this.gson.getDelegateAdapter(QueueItemTypeAdapterFactory.this, this.elementType);
            JsonElement     targetElement       = delegatingAdapter.toJsonTree(value);
            JsonObject      targetJsonObj       = targetElement.getAsJsonObject();

            // Add a entity type class name property
            targetJsonObj.addProperty(JSON_FIELD_ENTITY_TYPE, value.getEntity().getClass().getName());

            this.elementAdapter.write(jsonWriter, targetElement);
        }

        @Override
        @SuppressWarnings("unchecked")
        public T read(JsonReader jsonReader)
        throws IOException
        {
            JsonElement     readQueueItemElement    = this.elementAdapter.read(jsonReader);
            JsonObject      readQueueItemObject     = readQueueItemElement.getAsJsonObject();
            String          entityClassName;
            Type            entityType;
            TypeAdapter<T>  delegatingAdapter;

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

            /* Get the appropriate adapter for the specific type of entity
             * we're de-serializing, wrapped in a QueueItem.
             */
            delegatingAdapter =
                (TypeAdapter<T>)this.gson.getDelegateAdapter(
                    QueueItemTypeAdapterFactory.this,
                    TypeToken.get(
                        new LateBoundParameterizedType(
                            (ParameterizedType)(new TypeToken<QueueItem<?>>(){}.getType()),
                            entityType)));

            // Remove the type field, since it won't de-serialize.
            readQueueItemObject.remove(JSON_FIELD_ENTITY_TYPE);

            return delegatingAdapter.fromJsonTree(readQueueItemElement);
        }
    }
}