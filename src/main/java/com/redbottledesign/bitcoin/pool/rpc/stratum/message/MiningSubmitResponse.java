package com.redbottledesign.bitcoin.pool.rpc.stratum.message;

import org.json.JSONObject;

import com.redbottledesign.bitcoin.rpc.stratum.MalformedStratumMessageException;
import com.redbottledesign.bitcoin.rpc.stratum.message.ResponseMessage;
import com.redbottledesign.bitcoin.rpc.stratum.message.ValueResult;

/**
 * <p>Java representation of a Stratum {@code mining.submit} response
 * message, which is used to respond to a worker's submission of work.</p>
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
     *
     * @param   error
     *          An error message for why the submission could not be accepted.
     *          This must be {@code null} if the submission was accepted
     *          without issue.
     */
    public MiningSubmitResponse(MiningSubmitRequest request, boolean accepted, String error)
    {
        this(request.getId(), accepted, error);
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
     *
     * @param   error
     *          An error message for why the submission could not be accepted.
     *          This must be {@code null} if the submission was accepted
     *          without issue.
     */
    public MiningSubmitResponse(String id, boolean accepted, String error)
    {
        super(id, new ValueResult<>(accepted), error);

        if (accepted && (error != null))
            throw new IllegalArgumentException("error must be null if accepted is not 'false'.");
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
        final ValueResult<Boolean> result = this.getResult();

        return ((result != null) && result.getValue());
    }
}
