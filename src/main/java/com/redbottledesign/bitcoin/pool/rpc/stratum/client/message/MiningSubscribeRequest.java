package com.redbottledesign.bitcoin.pool.rpc.stratum.client.message;

import org.json.JSONObject;

import com.redbottledesign.bitcoin.rpc.stratum.MalformedStratumMessageException;
import com.redbottledesign.bitcoin.rpc.stratum.message.StratumRequestMessage;

public class MiningSubscribeRequest
extends StratumRequestMessage
{
    public static final String METHOD_NAME = "mining.subscribe";

    public MiningSubscribeRequest()
    {
        this(StratumRequestMessage.getNextRequestId());
    }

    public MiningSubscribeRequest(long id)
    {
        super(id, METHOD_NAME);
    }

    public MiningSubscribeRequest(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        super(jsonMessage);
    }
}
