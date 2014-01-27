package com.redbottledesign.bitcoin.pool.rpc.bitcoin;

import java.util.Arrays;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.StratumServer;
import com.google.bitcoin.core.ProtocolException;
import com.google.bitcoin.core.Transaction;

public class PrefabCoinbase
extends AbstractCoinbase
{
    protected static final int EXTRA1_SCRIPT_RELATIVE_OFFSET = 0;
    protected static final int EXTRA1_DEFAULT_BYTE_LENGTH    = 4;
    protected static final int EXTRA2_DEFAULT_BYTE_LENGTH    = 4;
    protected static final int COINBASE_LENGTH_BYTE_OFFSET   = TX_HEADER_LENGTH - 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(PrefabCoinbase.class);

    private int originalScriptLength;
    private int extraNonce1ByteLength;
    private int extraNonce2ByteLength;

    private byte[] coinbase2;

    public PrefabCoinbase(StratumServer server, byte[] coinbasePart1, byte[] extraNonce1, int extraNonce2Length,
                          byte[] coinbasePart2)
    throws ProtocolException
    {
        super(server);

        this.setExtraNonce1Size(extraNonce1.length);
        this.setExtraNonce2Size(extraNonce2Length);

        this.setCoinbaseTransactionBytes(coinbasePart1, extraNonce1, coinbasePart2);
    }

    public PrefabCoinbase(StratumServer server, Transaction coinbaseTransaction, byte[] extraNonce1)
    {
        this(server, coinbaseTransaction, extraNonce1, EXTRA1_DEFAULT_BYTE_LENGTH, EXTRA2_DEFAULT_BYTE_LENGTH);
    }

    public PrefabCoinbase(StratumServer server, Transaction coinbaseTransaction, byte[] extraNonce1,
                          int extraNonce1Length, int extraNonce2Length)
    {
        super(server);

        this.setExtraNonce1Size(extraNonce1Length);
        this.setExtraNonce2Size(extraNonce2Length);

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
    public int getCoinbase1Length()
    {
        return TX_HEADER_LENGTH + this.originalScriptLength;
    }

    @Override
    public int getExtraNonce1Offset()
    {
        return this.originalScriptLength + EXTRA1_SCRIPT_RELATIVE_OFFSET;
    }

    @Override
    public int getExtraNonce1Length()
    {
        return this.extraNonce1ByteLength;
    }

    @Override
    public int getExtraNonce2Offset()
    {
        return this.originalScriptLength + EXTRA1_SCRIPT_RELATIVE_OFFSET + this.getExtraNonce1Length();
    }

    @Override
    public int getExtraNonce2Length()
    {
        return this.extraNonce2ByteLength;
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
        return this.getCoinbase1Offset() + this.getCoinbase1Length() + this.getExtraNonce1Length() + this.getExtraNonce2Length();
    }

    @Override
    public int getCoinbase2Length()
    {
        return this.coinbase2.length;
    }

    protected void setExtraNonce1Size(int extraNonce1Length)
    {
        this.extraNonce1ByteLength = extraNonce1Length;
    }

    protected void setExtraNonce2Size(int extraNonce2Length)
    {
        this.extraNonce2ByteLength = extraNonce2Length;
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
        int     newScriptLength = scriptBytes.length + this.getExtraNonce1Length() + this.getExtraNonce2Length();

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

    protected void setCoinbaseTransactionBytes(byte[] coinbasePart1, byte[] extraNonce1, byte[] coinbasePart2)
    throws ProtocolException
    {
        int     coinbasePart1Length         = coinbasePart1.length,
                extraNonce1Offset           = coinbasePart1Length,
                extraNonce1Length           = this.getExtraNonce1Length(),
                extraNonce2Offset           = extraNonce1Offset + extraNonce1Length,
                extraNonce2Length           = this.getExtraNonce2Length(),
                coinbasePart2Offset         = extraNonce2Offset + extraNonce2Length,
                coinbasePart2Length         = coinbasePart2.length,
                fullCoinbaseLength          = coinbasePart2Offset + coinbasePart2Length;
        byte[]  coinbaseTransactionBytes    = new byte[fullCoinbaseLength];

        if (extraNonce1.length != extraNonce1Length)
        {
            throw new IllegalArgumentException(
                String.format(
                    "extraNonce1 is not of the expected size (expected: %d, actual: %d).",
                    extraNonce1Length,
                    extraNonce1.length));
        }

        for (int byteIndex = 0; byteIndex < coinbasePart1Length; ++byteIndex)
        {
            coinbaseTransactionBytes[byteIndex] = coinbasePart1[byteIndex];
        }

        for (int byteIndex = 0; byteIndex < extraNonce1Length; ++byteIndex)
        {
            coinbaseTransactionBytes[extraNonce1Offset + byteIndex] = extraNonce1[byteIndex];
        }

        for (int byteIndex = 0; byteIndex < extraNonce2Length; ++byteIndex)
        {
            coinbaseTransactionBytes[extraNonce2Offset + byteIndex] = 0;
        }

        for (int byteIndex = 0; byteIndex < coinbasePart2.length; ++byteIndex)
        {
            coinbaseTransactionBytes[coinbasePart2Offset + byteIndex] = coinbasePart2[byteIndex];
        }

        this.setCoinbaseTransactionBytes(coinbaseTransactionBytes);
    }
}