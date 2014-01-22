package com.redbottledesign.bitcoin.pool.rpc.stratum.client;

import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningAuthorizeResponse;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningNotifyRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSetDifficultyRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubmitResponse;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubscribeResponse;

public interface MiningClientEventListener
{
    public void onMinerSubscribed(MiningSubscribeResponse response);
    public void onNewWorkReceived(MiningNotifyRequest request);
    public void onWorkerAuthenticated(MiningAuthorizeResponse response);
    public void onDifficultySet(MiningSetDifficultyRequest request);
    public void onWorkSubmitted(MiningSubmitResponse response);
}
