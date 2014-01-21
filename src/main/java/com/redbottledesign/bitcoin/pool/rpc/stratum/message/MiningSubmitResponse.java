package com.redbottledesign.bitcoin.pool.rpc.stratum.message;

import org.json.JSONObject;

import com.redbottledesign.bitcoin.rpc.stratum.MalformedStratumMessageException;
import com.redbottledesign.bitcoin.rpc.stratum.message.ArrayResult;
import com.redbottledesign.bitcoin.rpc.stratum.message.ResponseMessage;

/**
 * <p>Java representation of a Stratum {@code mining.submit} response
 * message.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (gpaddock@redbottledesign.com)
 */
public class MiningSubmitResponse
extends ResponseMessage
{
    /**
     * Constructor for {@link MiningSubmitResponse} that creates a new
     * instance with the specified request ID and submission status.
     *
     * @param   id
     *          The ID of the request to which this response corresponds.
     */
    public MiningSubmitResponse(long id, boolean submitted)
    {
        super(id, new ArrayResult(submitted));
    }

    /**
     * Constructor for {@link MiningSubmitResponse} that creates a new
     * instance with the specified request ID and error message (for submission
     * failures).
     *
     * @param   id
     *          The ID of the request to which this response corresponds.
     */
    public MiningSubmitResponse(long id, String error)
    {
        super(id, error);
    }

    /**
     * Constructor for {@link MiningSubmitResponse} that creates a new
     * instance from information in the provided JSON message.
     *
     * @param   jsonMessage
     *          The message in JSON format.
     */
    public MiningSubmitResponse(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        super(jsonMessage);
    }
}