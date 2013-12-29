package com.redbottledesign.bitcoin.pool.rpc.bitcoin;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.PoolUser;
import com.github.fireduck64.sockthing.StratumServer;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;

public class StaticCoinbase
extends AbstractCoinbase
{
    private static final Logger LOGGER = LoggerFactory.getLogger(StaticCoinbase.class);

    public StaticCoinbase(StratumServer server, PoolUser poolUser, byte[] coinbaseTransactionData)
    {
        super(server, poolUser);

        this.setCoinbaseTransactionBytes(coinbaseTransactionData);
    }

    @Override
    public void regenerateCoinbaseTransaction()
    {
        StratumServer       server          = this.getServer();
        NetworkParameters   networkParams   = server.getNetworkParameters();
        Transaction         tx              = this.getCoinbaseTransaction();
        byte[]              scriptBytes     = this.getCoinbaseScriptBytes();

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Coinbase() - Script bytes: " + scriptBytes.length);
            LOGGER.debug("Coinbase() - Script: " + Hex.encodeHexString(scriptBytes));
        }

        tx.clearInputs();
        tx.addInput(new TransactionInput(networkParams, tx, scriptBytes));

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Coinbase() - Transaction: ");

            for (TransactionOutput out : tx.getOutputs())
            {
                LOGGER.debug("  " + out);
            }
        }

        this.setCoinbaseTransactionBytes(tx.bitcoinSerialize());
        this.setCoinbaseTransaction(tx);
    }
}
