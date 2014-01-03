package com.redbottledesign.bitcoin.pool.rpc.bitcoin;

import java.util.Arrays;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.StratumServer;
import com.google.bitcoin.core.Transaction;

public class StaticCoinbase
extends AbstractCoinbase
{
    protected static final int EXTRA1_SCRIPT_RELATIVE_OFFSET    = 0;
    protected static final int EXTRA1_BYTE_LENGTH               = 4;

    protected static final int EXTRA2_SCRIPT_RELATIVE_OFFSET    = EXTRA1_SCRIPT_RELATIVE_OFFSET + EXTRA1_BYTE_LENGTH;
    protected static final int EXTRA2_BYTE_LENGTH               = 4;

    protected static final int COINBASE_LENGTH_BYTE_OFFSET  = TX_HEADER_LENGTH - 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(StaticCoinbase.class);

    private int originalScriptLength;

    private byte[] coinbase2;

    public StaticCoinbase(StratumServer server, Transaction coinbaseTransaction, byte[] extraNonce1)
    {
        super(server);

        this.setCoinbaseTransaction(coinbaseTransaction);

        this.extractCoinbase2();
        this.resizeCoinbaseScript();

        this.setExtraNonce1(extraNonce1);
    }

    @Override
    public int getCoinbase1Offset()
    {
        return 0;
    }

    @Override
    public int getCoinbase1Size()
    {
        return TX_HEADER_LENGTH + this.originalScriptLength;
    }

    @Override
    public int getExtraNonce1Offset()
    {
        return this.originalScriptLength + EXTRA1_SCRIPT_RELATIVE_OFFSET;
    }

    @Override
    public int getExtraNonce1Size()
    {
        return EXTRA1_BYTE_LENGTH;
    }

    @Override
    public int getExtraNonce2Offset()
    {
        return this.originalScriptLength + EXTRA2_SCRIPT_RELATIVE_OFFSET;
    }

    @Override
    public int getExtraNonce2Size()
    {
        return EXTRA2_BYTE_LENGTH;
    }

    @Override
    public byte[] getCoinbasePart2()
    throws IllegalStateException
    {
        return this.coinbase2;
    }

    @Override
    public int getCoinbase2Offset()
    {
        return this.getCoinbase1Offset() + this.getCoinbase1Size() + this.getExtraNonce1Size() + this.getExtraNonce2Size();
    }

    @Override
    public int getCoinbase2Size()
    {
        return coinbase2.length;
    }

    protected void extractCoinbase2()
    {
        byte[]  scriptBytes  = this.getCoinbaseScriptBytes(),
                txBytes      = this.getCoinbaseTransactionBytes();

        this.coinbase2 = Arrays.copyOfRange(txBytes, TX_HEADER_LENGTH + scriptBytes.length, txBytes.length);
    }

    protected void resizeCoinbaseScript()
    {
        byte[]  scriptBytes     = this.getCoinbaseScriptBytes();
        int     newScriptLength = scriptBytes.length + EXTRA1_BYTE_LENGTH + EXTRA2_BYTE_LENGTH;

        this.originalScriptLength = scriptBytes.length;

        scriptBytes = Arrays.copyOfRange(scriptBytes, 0, newScriptLength);

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(
                String.format(
                    "resizeCoinbaseScript() - Size of coinbase script is %d before adding extra nonces.",
                    this.originalScriptLength));

            LOGGER.debug(
                String.format(
                    "resizeCoinbaseScript() - Size of coinbase script is %d after adding extra nonces.",
                    newScriptLength));

            LOGGER.debug(
                String.format(
                    "resizeCoinbaseScript() - Expanded coinbase script: %s.",
                    Hex.encodeHexString(scriptBytes)));
        }

        this.setCoinbaseScriptBytes(scriptBytes);
    }
}