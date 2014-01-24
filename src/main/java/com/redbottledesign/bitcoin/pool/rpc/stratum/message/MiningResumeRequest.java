package com.redbottledesign.bitcoin.pool.rpc.stratum.message;

import java.util.List;

import org.json.JSONObject;

import com.redbottledesign.bitcoin.rpc.stratum.MalformedStratumMessageException;
import com.redbottledesign.bitcoin.rpc.stratum.message.RequestMessage;

/**
 * <p>Java representation of a Stratum {@code mining.resume} request
 * message.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (gpaddock@redbottledesign.com)
 */
public class MiningResumeRequest
extends RequestMessage
{
    /**
     * The name of this method as it appears in the request.
     */
    public static final String METHOD_NAME = "mining.resume";

    /**
     * The number of required parameters for this request.
     */
    public static final int PARAM_REQUIRED_COUNT = 1;

    /**
     * The offset of the parameter that specifies the session to resume.
     */
    private static final int PARAM_OFFSET_SESSION_ID = 0;

    /**
     * <p>Constructor for {@link MiningResumeRequest} that creates a new
     * instance with the specified session ID.</p>
     *
     * <p>The request is automatically assigned a unique ID.</p>
     *
     * @param   sessionId
     *          The unique identifier for the session to resume.
     */
    public MiningResumeRequest(String sessionId)
    {
        this(RequestMessage.getNextRequestId(), sessionId);
    }

    /**
     * Constructor for {@link MiningResumeRequest} that creates a new
     * instance with the specified message ID and session ID.
     *
     * @param   id
     *          The message ID.
     *
     * @param   sessionId
     *          The unique identifier for the session to resume.
     */
    public MiningResumeRequest(String id, String sessionId)
    {
        super(id, METHOD_NAME, sessionId);
    }

    /**
     * Constructor for {@link MiningResumeRequest} that creates a new
     * instance from information in the provided JSON message.
     *
     * @param   jsonMessage
     *          The message in JSON format.
     *
     * @throws  MalformedStratumMessageException
     *          If the provided JSON message object is not a properly-formed
     *          Stratum message or cannot be understood.
     */
    public MiningResumeRequest(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        super(jsonMessage);
    }

    /**
     * Gets the name of the worker.
     *
     * @return  The name of the worker.
     */
    public String getSessionId()
    {
        return this.getParams().get(PARAM_OFFSET_SESSION_ID).toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateParsedData(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        List<Object> params = this.getParams();

        super.validateParsedData(jsonMessage);

        if (params.size() < PARAM_REQUIRED_COUNT)
        {
            throw new MalformedStratumMessageException(
                METHOD_NAME,
                PARAM_REQUIRED_COUNT + " parameters are required",
                jsonMessage);
        }

        /* The rest of validation will happen when data is requested by the getters.
         * To repeat that same code here would be redundant.
         */
    }
}