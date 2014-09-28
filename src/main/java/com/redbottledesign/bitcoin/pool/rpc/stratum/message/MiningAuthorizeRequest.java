package com.redbottledesign.bitcoin.pool.rpc.stratum.message;

import java.util.List;

import org.json.JSONObject;

import com.redbottledesign.bitcoin.rpc.stratum.MalformedStratumMessageException;
import com.redbottledesign.bitcoin.rpc.stratum.message.RequestMessage;

/**
 * <p>Java representation of a Stratum {@code mining.authorize} request
 * message, which is used by a worker to authenticate as a particular
 * user.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (gpaddock@redbottledesign.com)
 */
public class MiningAuthorizeRequest
extends RequestMessage
{
    /**
     * The name of this method as it appears in the request.
     */
    public static final String METHOD_NAME = "mining.authorize";

    /**
     * The number of required parameters for this request.
     */
    public static final int PARAM_REQUIRED_COUNT = 1;

    /**
     * The offset of the parameter that specifies the worker user name.
     */
    private static final int PARAM_OFFSET_USERNAME = 0;

    /**
     * The offset of the parameter that specifies the worker password.
     */
    private static final int PARAM_OFFSET_PASSWORD = 1;

    /**
     * <p>Constructor for {@link MiningAuthorizeRequest} that creates a new
     * instance with the specified username and password.</p>
     *
     * <p>The request is automatically assigned a unique ID.</p>
     *
     * @param   username
     *          The username of the worker.
     *
     * @param   password
     *          The password of the worker.
     */
    public MiningAuthorizeRequest(String username, String password)
    {
        this(RequestMessage.getNextRequestId(), username, password);
    }

    /**
     * Constructor for {@link MiningAuthorizeRequest} that creates a new
     * instance with the specified message ID, username, and password.
     *
     * @param   id
     *          The message ID.
     *
     * @param   username
     *          The username of the worker.
     *
     * @param   password
     *          The password of the worker.
     */
    public MiningAuthorizeRequest(String id, String username, String password)
    {
        super(id, METHOD_NAME, username, password);
    }

    /**
     * Constructor for {@link MiningAuthorizeRequest} that creates a new
     * instance from information in the provided JSON message.
     *
     * @param   jsonMessage
     *          The message in JSON format.
     *
     * @throws  MalformedStratumMessageException
     *          If the provided JSON message object is not a properly-formed
     *          Stratum message or cannot be understood.
     */
    public MiningAuthorizeRequest(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        super(jsonMessage);
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
                "at least a username is required",
                jsonMessage);
        }

        if (!(params.get(PARAM_OFFSET_USERNAME) instanceof String))
        {
            throw new MalformedStratumMessageException(
                METHOD_NAME,
                "username must be a String",
                jsonMessage);
        }

        if (!(params.get(PARAM_OFFSET_PASSWORD) instanceof String))
        {
            throw new MalformedStratumMessageException(
                METHOD_NAME,
                "password must be a String",
                jsonMessage);
        }
    }

    /**
     * Gets the username of the worker.
     *
     * @return  The username.
     */
    public String getUsername()
    {
        return this.getParams().get(PARAM_OFFSET_USERNAME).toString();
    }

    /**
     * Gets the password of the worker.
     *
     * @return  The password.
     */
    public String getPassword()
    {
        return this.getParams().get(PARAM_OFFSET_PASSWORD).toString();
    }
}