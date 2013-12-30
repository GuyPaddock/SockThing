package com.redbottledesign.bitcoin.pool.rpc.bitcoin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.PoolUser;
import com.github.fireduck64.sockthing.StratumServer;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;

public class StaticCoinbase
extends AbstractCoinbase
{
    private static final Logger LOGGER = LoggerFactory.getLogger(StaticCoinbase.class);

    public StaticCoinbase(StratumServer server, Transaction coinbaseTransaction)
    {
        super(server);

        this.setCoinbaseTransaction(coinbaseTransaction);
    }

    @Override
    public void regenerateCoinbaseTransaction(PoolUser user)
    {
        StratumServer       server          = this.getServer();
        NetworkParameters   networkParams   = server.getNetworkParameters();
        Transaction         tx              = this.getCoinbaseTransaction();
        byte[]              scriptBytes     = this.getCoinbaseScriptBytes();

        tx.clearInputs();
        tx.addInput(new TransactionInput(networkParams, tx, scriptBytes));

        this.setCoinbaseTransaction(tx);
    }
}