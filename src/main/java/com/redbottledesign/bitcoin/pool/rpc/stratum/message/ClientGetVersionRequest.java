package com.redbottledesign.bitcoin.pool.rpc.stratum.message;

import org.json.JSONObject;

import com.redbottledesign.bitcoin.rpc.stratum.MalformedStratumMessageException;
import com.redbottledesign.bitcoin.rpc.stratum.message.RequestMessage;

public class ClientGetVersionRequest
extends RequestMessage
{
    /**
     * The name of this method as it appears in the request.
     */
    public static final String METHOD_NAME = "client.get_version";

    /**
     * <p>Constructor for {@link ClientGetVersionRequest} that creates a new
     * instance.</p>
     *
     * <p>The request is automatically assigned a unique ID.</p>
     */
    public ClientGetVersionRequest()
    throws IllegalArgumentException
    {
        this(RequestMessage.getNextRequestId());
    }

    /**
     * <p>Constructor for {@link ClientGetVersionRequest} that creates a new
     * instance with the specified message ID.</p>
     *
     * @param   id
     *          The message ID.
     */
    public ClientGetVersionRequest(String id)
    throws IllegalArgumentException
    {
        super(id, METHOD_NAME);
    }

    /**
     * Constructor for {@link ClientGetVersionRequest} that creates a new
     * instance from information in the provided JSON message.
     *
     * @param   jsonMessage
     *          The message in JSON format.
     */
    public ClientGetVersionRequest(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        super(jsonMessage);
    }
}