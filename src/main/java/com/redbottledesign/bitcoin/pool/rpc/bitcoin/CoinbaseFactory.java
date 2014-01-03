package com.redbottledesign.bitcoin.pool.rpc.bitcoin;

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

    public Coinbase generateCoinbase(StratumServer server, BlockTemplate blockTemplate, byte[] extraNonce1)
    {
        Coinbase result;

        if (!blockTemplate.hasCoinbaseTransaction())
        {
            result =
                new GeneratedCoinbase(
                    server,
                    blockTemplate.getHeight(),
                    blockTemplate.getReward(),
                    blockTemplate.getTotalFees(),
                    extraNonce1);
        }

        else
        {
            result = new StaticCoinbase(server, blockTemplate.getCoinbaseTransaction(), extraNonce1);
        }

        return result;
    }
}
