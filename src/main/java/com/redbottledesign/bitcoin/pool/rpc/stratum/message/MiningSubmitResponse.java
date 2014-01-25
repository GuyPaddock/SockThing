package com.redbottledesign.bitcoin.pool.rpc.stratum.message;

import org.json.JSONObject;

import com.redbottledesign.bitcoin.rpc.stratum.MalformedStratumMessageException;
import com.redbottledesign.bitcoin.rpc.stratum.message.ResponseMessage;
import com.redbottledesign.bitcoin.rpc.stratum.message.ValueResult;

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
     * instance for the specified request, with the specified submission
     * status.
     *
     * @param   request
     *          The request to which this response corresponds.
     *
     * @param   accepted
     *          {@code true} if the submission was accepted;
     *          {@code false}, otherwise.
     */
    public MiningSubmitResponse(MiningSubmitRequest request, boolean accepted)
    {
        this(request.getId(), accepted);
    }

    /**
     * Constructor for {@link MiningSubmitResponse} that creates a new
     * instance with the specified request ID and submission status.
     *
     * @param   id
     *          The ID of the request to which this response corresponds.
     *
     * @param   accepted
     *          {@code true} if the submission was accepted;
     *          {@code false}, otherwise.
     */
    public MiningSubmitResponse(String id, boolean accepted)
    {
        super(id, new ValueResult<>(accepted));
    }

    /**
     * Constructor for {@link MiningSubmitResponse} that creates a new
     * instance with the specified request ID and error message (for submission
     * failures).
     *
     * @param   id
     *          The ID of the request to which this response corresponds.
     *
     * @param   error
     *          An error message for why the submission could not be accepted.
     */
    public MiningSubmitResponse(String id, String error)
    {
        super(id, error);
    }

    /**
     * Constructor for {@link MiningSubmitResponse} that creates a new
     * instance from information in the provided JSON message.
     *
     * @param   jsonMessage
     *          The message in JSON format.
     *
     * @throws  MalformedStratumMessageException
     *          If the provided JSON message object is not a properly-formed
     *          Stratum message or cannot be understood.
     */
    public MiningSubmitResponse(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        super(jsonMessage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public ValueResult<Boolean> getResult()
    {
        return (ValueResult<Boolean>)super.getResult();
    }

    /**
     * Returns whether or not the work was accepted.
     *
     * @return  {@code true} if the work was successfully accepted;
     *          {@code false}, otherwise.
     */
    public boolean wasAccepted()
    {
        return this.getResult().getValue();
    }
}