package com.redbottledesign.bitcoin.pool.rpc.stratum.client.message;

import org.json.JSONObject;

import com.redbottledesign.bitcoin.rpc.stratum.MalformedStratumMessageException;
import com.redbottledesign.bitcoin.rpc.stratum.message.StratumRequestMessage;

/**
 * <p>Java representation of a Stratum {@code mining.subscribe} request
 * message.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (gpaddock@redbottledesign.com)
 */
public class MiningSubscribeRequest
extends StratumRequestMessage
{
    /**
     * The name of this method as it appears in the request.
     */
    public static final String METHOD_NAME = "mining.subscribe";

    /**
     * <p>Constructor for {@link MiningSubscribeRequest}.</p>
     *
     * <p>The request is automatically assigned a unique ID.</p>
     */
    public MiningSubscribeRequest()
    {
        this(StratumRequestMessage.getNextRequestId());
    }

    /**
     * Constructor for {@link MiningSubscribeRequest} that initializes the
     * request to have the specified message ID.
     *
     * @param   id
     *          The message ID.
     */
    public MiningSubscribeRequest(long id)
    {
        super(id, METHOD_NAME);
    }

    /**
     * Constructor for {@link MiningAuthorizeRequest} that creates a new
     * instance from information in the provided JSON message.
     *
     * @param   jsonMessage
     *          The message in JSON format.
     */
    public MiningSubscribeRequest(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        super(jsonMessage);
    }
}
