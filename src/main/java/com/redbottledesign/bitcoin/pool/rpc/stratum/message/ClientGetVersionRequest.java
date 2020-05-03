package com.redbottledesign.bitcoin.pool.rpc.stratum.message;

import org.json.JSONObject;

import com.redbottledesign.bitcoin.rpc.stratum.MalformedStratumMessageException;
import com.redbottledesign.bitcoin.rpc.stratum.message.RequestMessage;

/**
 * <p>Java representation of a Stratum {@code client.get_version} request
 * message, which is used to ask what software a particular mining
 * client is using.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (gpaddock@redbottledesign.com)
 */
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
    {
        super(id, METHOD_NAME);
    }

    /**
     * Constructor for {@link ClientGetVersionRequest} that creates a new
     * instance from information in the provided JSON message.
     *
     * @param   jsonMessage
     *          The message in JSON format.
     *
     * @throws  MalformedStratumMessageException
     *          If the provided JSON message object is not a properly-formed
     *          Stratum message or cannot be understood.
     */
    public ClientGetVersionRequest(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        super(jsonMessage);
    }
}
