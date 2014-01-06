package com.redbottledesign.bitcoin.pool.rpc.stratum.client.message;

import java.util.List;

import org.json.JSONObject;

import com.redbottledesign.bitcoin.rpc.stratum.MalformedStratumMessageException;
import com.redbottledesign.bitcoin.rpc.stratum.message.StratumRequestMessage;

public class MiningAuthorizeRequest
extends StratumRequestMessage
{
    public static final String METHOD_NAME = "mining.authorize";

    public MiningAuthorizeRequest(String username, String password)
    {
        this(StratumRequestMessage.getNextRequestId(), username, password);
    }

    public MiningAuthorizeRequest(long id, String username, String password)
    {
        super(id, METHOD_NAME, username, password);
    }

    public MiningAuthorizeRequest(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        super(jsonMessage);
    }

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

    public String getUsername()
    {
        return this.getParams().get(0).toString();
    }

    public String getPassword()
    {
        return this.getParams().get(1).toString();
    }
}