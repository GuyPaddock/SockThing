package com.redbottledesign.util.gson;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class PolymorphicSerializerDeserializer<T>
implements JsonSerializer<T>, JsonDeserializer<T>
{
    public static final String JSON_FIELD_CLASS_NAME = "className";

    private final Class<?> taggingInterface;

    public PolymorphicSerializerDeserializer(Class<?> taggingInterface)
    {
        if (taggingInterface == null)
            throw new IllegalArgumentException("taggingInterface cannot be null.");

        this.taggingInterface = taggingInterface;
    }

    @Override
    public JsonElement serialize(T sourceObject, Type typeOfSource, JsonSerializationContext context)
    {
        JsonElement result = JsonNull.INSTANCE;

        if (sourceObject != null)
        {
            JsonObject  resultObject;
            String      className    = sourceObject.getClass().getName();

            result       = context.serialize(sourceObject);
            resultObject = result.getAsJsonObject();

            resultObject.addProperty(JSON_FIELD_CLASS_NAME, className);
        }

        return result;
    }

    @Override
    public T deserialize(JsonElement json, Type typeOfObject, JsonDeserializationContext context)
    throws JsonParseException
    {
        T result = null;

        if ((json != null) && (json.isJsonObject()))
        {
            JsonObject  jsonObject      = json.getAsJsonObject();
            String      objectClassName;
            Class<?>    objectType;

            if (!jsonObject.has(JSON_FIELD_CLASS_NAME))
                throw new JsonParseException(String.format("Missing '%s' property in JSON.", JSON_FIELD_CLASS_NAME));

            objectClassName = jsonObject.get(JSON_FIELD_CLASS_NAME).getAsString();

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

            if (!this.taggingInterface.isAssignableFrom(objectType))
            {
                throw new JsonParseException(
                    String.format(
                        "Entity class '%s' referenced in JSON is not of the expected type '%s'.",
                        objectClassName,
                        this.taggingInterface.getName()));
            }

            result = context.deserialize(jsonObject, objectType);
        }

        return result;
    }
}