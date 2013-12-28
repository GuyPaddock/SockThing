package com.redbottledesign.bitcoin.pool.rpc.stratum.client;

import org.json.JSONObject;

public class MalformedStratumMessageException
extends Exception
{
    /**
     * Serial version ID.
     */
    private static final long serialVersionUID = -5169028037089628215L;

    public MalformedStratumMessageException(JSONObject message)
    {
        super("Unknown or malformed Stratum JSON message received: " + message.toString());
    }

    public MalformedStratumMessageException(JSONObject message, String method)
    {
        super(
            String.format(
                "Unknown or malformed \"%s\" Stratum JSON message received: %s",
                method,
                message.toString()));
    }
}