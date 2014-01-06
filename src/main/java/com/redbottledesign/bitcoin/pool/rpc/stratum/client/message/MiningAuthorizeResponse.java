package com.redbottledesign.bitcoin.pool.rpc.stratum.client.message;

import org.json.JSONObject;

import com.redbottledesign.bitcoin.rpc.stratum.MalformedStratumMessageException;
import com.redbottledesign.bitcoin.rpc.stratum.message.StratumArrayResult;
import com.redbottledesign.bitcoin.rpc.stratum.message.StratumResponseMessage;

public class MiningAuthorizeResponse
extends StratumResponseMessage
{
    public MiningAuthorizeResponse(long id, boolean authorized)
    {
        super(id, new StratumArrayResult(authorized));
    }

    public MiningAuthorizeResponse(long id, String error)
    {
        super(id, error);
    }

    public MiningAuthorizeResponse(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        super(jsonMessage);
    }
}
