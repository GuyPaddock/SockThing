package com.redbottledesign.bitcoin.pool.rpc.stratum.message;

import org.json.JSONObject;

import com.redbottledesign.bitcoin.rpc.stratum.MalformedStratumMessageException;
import com.redbottledesign.bitcoin.rpc.stratum.message.ResponseMessage;
import com.redbottledesign.bitcoin.rpc.stratum.message.ValueResult;

/**
 * <p>Java representation of a Stratum {@code mining.resume} response
 * message.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (gpaddock@redbottledesign.com)
 */
public class MiningResumeResponse
extends ResponseMessage
{
    /**
     * Constructor for {@link MiningResumeResponse} that creates a new
     * instance for the specified request, with the specified resume status.
     *
     * @param   request
     *          The request to which this response corresponds.
     *
     * @param   resumed
     *          {@code true} if the session was resumed;
     *          {@code false}, otherwise.
     */
    public MiningResumeResponse(MiningResumeRequest request, boolean resumed)
    {
        this(request.getId(), resumed);
    }

    /**
     * Constructor for {@link MiningResumeResponse} that creates a new
     * instance with the specified request ID and resume status.
     *
     * @param   id
     *          The ID of the request to which this response corresponds.
     *
     * @param   resumed
     *          {@code true} if the session was resumed;
     *          {@code false}, otherwise.
     */
    public MiningResumeResponse(String id, boolean resumed)
    {
        super(id, new ValueResult<>(resumed));
    }

    /**
     * Constructor for {@link MiningResumeResponse} that creates a new
     * instance with the specified request ID and error message (for resume
     * failures).
     *
     * @param   id
     *          The ID of the request to which this response corresponds.
     *
     * @param   error
     *          An error message for why the session could not be resumed.
     */
    public MiningResumeResponse(String id, String error)
    {
        super(id, error);
    }

    /**
     * Constructor for {@link MiningResumeResponse} that creates a new
     * instance from information in the provided JSON message.
     *
     * @param   jsonMessage
     *          The message in JSON format.
     *
     * @throws  MalformedStratumMessageException
     *          If the provided JSON message object is not a properly-formed
     *          Stratum message or cannot be understood.
     */
    public MiningResumeResponse(JSONObject jsonMessage)
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
     * Returns whether or not the session was resumed.
     *
     * @return  {@code true} if the session was resumed;
     *          {@code false}, otherwise.
     */
    public boolean wasResumed()
    {
        return this.getResult().getValue();
    }
}