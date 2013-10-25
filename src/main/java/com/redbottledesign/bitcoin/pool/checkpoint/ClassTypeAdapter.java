package com.redbottledesign.bitcoin.pool.checkpoint;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class ClassTypeAdapter
extends TypeAdapter<Class<?>>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassTypeAdapter.class);

    @Override
    public void write(JsonWriter outWriter, Class<?> value)
                    throws IOException
    {
        outWriter.value(value.getName());
    }

    @Override
    public Class<?> read(JsonReader inReader)
    throws IOException
    {
        Class<?> result;

        if (inReader.peek() == JsonToken.NULL)
        {
            inReader.nextNull();

            result = null;
        }

        else
        {
            String className = inReader.nextString();

            try
            {
                result = Class.forName(className);
            }

            catch (ClassNotFoundException ex)
            {
                throw new RuntimeException(
                    String.format(
                        "Class '%s' referenced in JSON not found: %s",
                        className,
                        ex.getMessage()),
                    ex);
            }
        }

        return result;
    }
}
