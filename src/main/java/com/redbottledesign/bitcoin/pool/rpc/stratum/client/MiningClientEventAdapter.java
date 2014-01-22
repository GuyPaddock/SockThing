package com.redbottledesign.bitcoin.pool.rpc.stratum.client;

import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningAuthorizeResponse;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningNotifyRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSetDifficultyRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubmitResponse;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubscribeResponse;

public class MiningClientEventAdapter
implements MiningClientEventListener
{
    @Override
    public void onMinerSubscribed(MiningSubscribeResponse response)
    {
    }

    @Override
    public void onNewWorkReceived(MiningNotifyRequest request)
    {
    }

    @Override
    public void onWorkerAuthenticated(MiningAuthorizeResponse response)
    {
    }

    @Override
    public void onDifficultySet(MiningSetDifficultyRequest request)
    {
    }

    @Override
    public void onWorkSubmitted(MiningSubmitResponse response)
    {
    }
}
