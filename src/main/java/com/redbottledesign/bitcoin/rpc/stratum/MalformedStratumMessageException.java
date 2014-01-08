package com.redbottledesign.bitcoin.rpc.stratum;

import org.json.JSONArray;
import org.json.JSONObject;

public class MalformedStratumMessageException
extends Exception
{
    /**
     * Serial version ID.
     */
    private static final long serialVersionUID = -5169028037089628215L;

    public MalformedStratumMessageException(JSONObject jsonMessage)
    {
        this(jsonMessage.toString());
    }

    public MalformedStratumMessageException(String jsonMessage)
    {
        super("Unknown or malformed Stratum JSON message received: " + jsonMessage);
    }

    public MalformedStratumMessageException(JSONObject message, String error)
    {
        super(
            String.format(
                "Unknown or malformed Stratum JSON message received (%s): %s",
                error,
                message.toString()));
    }

    public MalformedStratumMessageException(JSONObject jsonMessage, Throwable t)
    {
        this(jsonMessage.toString(), t);
    }

    public MalformedStratumMessageException(String jsonMessage, Throwable t)
    {
        super(
            String.format(
                "Unknown or malformed Stratum JSON message received (%s): %s",
                t.getMessage(),
                jsonMessage),
            t);
    }
    public MalformedStratumMessageException(String method, JSONObject message)
    {
        super(
            String.format(
                "Unknown or malformed \"%s\" Stratum JSON message received: %s",
                method,
                message.toString()));
    }

    public MalformedStratumMessageException(String method, String error, JSONObject message)
    {
        super(
            String.format(
                "Unknown or malformed \"%s\" Stratum JSON message received (%s): %s",
                method,
                error,
                message.toString()));
    }

    public MalformedStratumMessageException(String method, String error, Throwable t, JSONObject message)
    {
        super(
            String.format(
                "Unknown or malformed \"%s\" Stratum JSON message received (%s): %s",
                method,
                error,
                message.toString()),
            t);
    }

    public MalformedStratumMessageException(JSONArray message)
    {
        super("Unknown or malformed Stratum JSON message received: " + message.toString());
    }

    public MalformedStratumMessageException(JSONArray message, String error)
    {
        super(
            String.format(
                "Unknown or malformed Stratum JSON message received (%s): %s",
                error,
                message.toString()));
    }

    public MalformedStratumMessageException(JSONArray message, Throwable t)
    {
        super(
            String.format(
                "Unknown or malformed Stratum JSON message received (%s): %s",
                t.getMessage(),
                message.toString()),
            t);
    }

    public MalformedStratumMessageException(String method, JSONArray message)
    {
        super(
            String.format(
                "Unknown or malformed \"%s\" Stratum JSON message received: %s",
                method,
                message.toString()));
    }

    public MalformedStratumMessageException(String method, String error, JSONArray message)
    {
        super(
            String.format(
                "Unknown or malformed \"%s\" Stratum JSON message received (%s): %s",
                method,
                error,
                message.toString()));
    }

    public MalformedStratumMessageException(String method, String error, Throwable t, JSONArray message)
    {
        super(
            String.format(
                "Unknown or malformed \"%s\" Stratum JSON message received (%s): %s",
                method,
                error,
                message.toString()),
            t);
    }
}