package com.redbottledesign.bitcoin.pool.rpc.stratum.client.message;

import org.json.JSONObject;

import com.redbottledesign.bitcoin.rpc.stratum.MalformedStratumMessageException;
import com.redbottledesign.bitcoin.rpc.stratum.message.StratumArrayResult;
import com.redbottledesign.bitcoin.rpc.stratum.message.StratumResponseMessage;

/**
 * <p>Java representation of a Stratum {@code mining.authorize} response
 * message.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (gpaddock@redbottledesign.com)
 */
public class MiningAuthorizeResponse
extends StratumResponseMessage
{
    /**
     * Constructor for {@link MiningAuthorizeResponse} that creates a new
     * instance with the specified request ID and authorization status.
     *
     * @param   id
     *          The ID of the request to which this response corresponds.
     */
    public MiningAuthorizeResponse(long id, boolean authorized)
    {
        super(id, new StratumArrayResult(authorized));
    }

    /**
     * Constructor for {@link MiningAuthorizeResponse} that creates a new
     * instance with the specified request ID and error message (for log-in
     * failures).
     *
     * @param   id
     *          The ID of the request to which this response corresponds.
     */
    public MiningAuthorizeResponse(long id, String error)
    {
        super(id, error);
    }

    /**
     * Constructor for {@link MiningAuthorizeResponse} that creates a new
     * instance from information in the provided JSON message.
     *
     * @param   jsonMessage
     *          The message in JSON format.
     */
    public MiningAuthorizeResponse(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        super(jsonMessage);
    }
}