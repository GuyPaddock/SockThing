package com.redbottledesign.bitcoin.pool.rpc.stratum.message;

import org.json.JSONObject;

import com.redbottledesign.bitcoin.rpc.stratum.MalformedStratumMessageException;
import com.redbottledesign.bitcoin.rpc.stratum.message.ResponseMessage;
import com.redbottledesign.bitcoin.rpc.stratum.message.ValueResult;

/**
 * <p>Java representation of a Stratum {@code mining.resume} response
 * message, which is used to respond to a worker's request to resume work from
 * a previous connection.</p>
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
     *
     * @param   error
     *          An error message for why the session could not be resumed.
     *          This must be {@code null} if the session was resumed without
     *          issue.
     */
    public MiningResumeResponse(MiningResumeRequest request, boolean resumed, String error)
    {
        this(request.getId(), resumed, error);
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
     *
     * @param   error
     *          An error message for why the session could not be resumed.
     *          This must be {@code null} if the session was resumed without
     *          issue.
     */
    public MiningResumeResponse(String id, boolean resumed, String error)
    {
        super(id, new ValueResult<>(resumed), error);

        if (resumed && (error != null))
            throw new IllegalArgumentException("error must be null if resumed is not 'false'.");
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
        final ValueResult<Boolean> result = this.getResult();

        return ((result != null) && result.getValue());
    }
}
