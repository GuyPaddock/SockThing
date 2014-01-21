package com.redbottledesign.bitcoin.pool.rpc.stratum.message;

import org.json.JSONObject;

import com.redbottledesign.bitcoin.rpc.stratum.MalformedStratumMessageException;
import com.redbottledesign.bitcoin.rpc.stratum.message.ArrayResult;
import com.redbottledesign.bitcoin.rpc.stratum.message.ResponseMessage;

/**
 * <p>Java representation of a Stratum {@code mining.authorize} response
 * message.</p>
 *
 * <p>� 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (gpaddock@redbottledesign.com)
 */
public class MiningAuthorizeResponse
extends ResponseMessage
{
    /**
     * Constructor for {@link MiningAuthorizeResponse} that creates a new
     * instance with the specified request ID and authorization status.
     *
     * @param   id
     *          The ID of the request to which this response corresponds.
     *
     * @param   authorized
     *          {@code true} if the miner was successfully authorized;
     *          {@code false}, otherwise.
     */
    public MiningAuthorizeResponse(long id, boolean authorized)
    {
        super(id, new ArrayResult(authorized));
    }

    /**
     * Constructor for {@link MiningAuthorizeResponse} that creates a new
     * instance with the specified request ID and error message (for log-in
     * failures).
     *
     * @param   id
     *          The ID of the request to which this response corresponds.
     *
     * @param   error
     *          An error message for why the miner could not be successfully
     *          authorized.
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