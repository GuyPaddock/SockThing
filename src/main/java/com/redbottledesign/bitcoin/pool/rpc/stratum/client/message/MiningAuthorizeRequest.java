package com.redbottledesign.bitcoin.pool.rpc.stratum.client.message;

import java.util.List;

import org.json.JSONObject;

import com.redbottledesign.bitcoin.rpc.stratum.MalformedStratumMessageException;
import com.redbottledesign.bitcoin.rpc.stratum.message.StratumRequestMessage;

/**
 * <p>Java representation of a Stratum {@code mining.authorize} request
 * message.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (gpaddock@redbottledesign.com)
 */
public class MiningAuthorizeRequest
extends StratumRequestMessage
{
    /**
     * The name of this method as it appears in the request.
     */
    public static final String METHOD_NAME = "mining.authorize";

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
        this(StratumRequestMessage.getNextRequestId(), username, password);
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
    public MiningAuthorizeRequest(long id, String username, String password)
    {
        super(id, METHOD_NAME, username, password);
    }

    /**
     * Constructor for {@link MiningAuthorizeRequest} that creates a new
     * instance from information in the provided JSON message.
     *
     * @param   jsonMessage
     *          The message in JSON format.
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

        if (params.size() < 2)
        {
            throw new MalformedStratumMessageException(
                METHOD_NAME,
                "both a username and password are required",
                jsonMessage);
        }

        if (!(params.get(0) instanceof String))
        {
            throw new MalformedStratumMessageException(
                METHOD_NAME,
                "username must be a String",
                jsonMessage);
        }

        if (!(params.get(1) instanceof String))
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