package com.redbottledesign.bitcoin.pool.rpc.bitcoin;

import com.github.fireduck64.sockthing.PoolUser;
import com.github.fireduck64.sockthing.StratumServer;

public class CoinbaseFactory
{
    protected static CoinbaseFactory INSTANCE = new CoinbaseFactory();

    public static CoinbaseFactory getInstance()
    {
        return INSTANCE;
    }

    protected CoinbaseFactory()
    {
    }

    public Coinbase generateCoinbase(StratumServer server, PoolUser poolUser, BlockTemplate blockTemplate,
                                     byte[] extraNonce1)
    {
        Coinbase result;

        if (!blockTemplate.hasCoinbaseTransactionBytes())
        {
            result =
                new GeneratedCoinbase(
                    server,
                    poolUser,
                    blockTemplate.getHeight(),
                    blockTemplate.getReward(),
                    blockTemplate.getTotalFees(),
                    extraNonce1);
        }

        else
        {
            result = new StaticCoinbase(server, poolUser, blockTemplate.getCoinbaseTransactionBytes());
        }

        return result;
    }
}
