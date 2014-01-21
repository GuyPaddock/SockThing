package com.redbottledesign.bitcoin.rpc.stratum.message;

import org.json.JSONObject;

import com.redbottledesign.bitcoin.rpc.stratum.MalformedStratumMessageException;

public class PollRequestMessage
extends RequestMessage
{
    public PollRequestMessage(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        super(jsonMessage);
    }

    public PollRequestMessage()
    {
        super(0, null);
    }

    @Override
    protected void setMethodName(String methodName)
    {
        // Do nothing; defeat exception in base class
    }

    @Override
    public JSONObject toJson()
    {
        // Empty object
        return new JSONObject();
    }
}