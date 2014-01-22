package com.redbottledesign.bitcoin.pool.rpc.stratum.client;

public interface MiningClientEventNotifier
{
    public abstract void notifyListener(MiningClientEventListener listener);
}
