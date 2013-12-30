package com.redbottledesign.bitcoin.pool.rpc.bitcoin;

import java.util.Arrays;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.StratumServer;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.ProtocolException;
import com.google.bitcoin.core.Transaction;

public abstract class AbstractCoinbase
implements Coinbase
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCoinbase.class);

    protected static final int TX_HEADER_LENGTH = 42;

    protected static final int BLOCK_HEIGHT_BYTE_LENGTH = 4;
    protected static final int BLOCK_HEIGHT_OFF = 0;

    protected static final int EXTRA1_OFF = 4;
    protected static final int EXTRA1_BYTE_LENGTH = 4;

    protected static final int EXTRA2_OFF = 8;
    protected static final int EXTRA2_BYTE_LENGTH = 4;

    private final StratumServer server;
    private final NetworkParameters networkParams;
    private byte[] coinbaseTransactionBytes;
    private Transaction coinbaseTransaction;
    private byte[] coinbaseScriptBytes;

    public AbstractCoinbase(StratumServer server)
    {
        this.server         = server;
        this.networkParams  = server.getNetworkParameters();
    }

    public StratumServer getServer()
    {
        return this.server;
    }

    @Override
    public Transaction getCoinbaseTransaction()
    {
        return this.coinbaseTransaction;
    }

    @Override
    public byte[] getCoinbaseTransactionBytes()
    {
        return this.coinbaseTransactionBytes;
    }

    @Override
    public byte[] getCoinbasePart1()
    throws IllegalStateException
    {
        if (this.coinbaseTransactionBytes == null)
        {
            throw new IllegalStateException("No coinbase transaction data is available.");
        }

        else
        {
            /* This contains our standard 42 byte transaction header then 4 bytes
             * of block height for block v2.
             */
            int     coinbase1Size   = TX_HEADER_LENGTH + BLOCK_HEIGHT_BYTE_LENGTH;
            byte[]  buff            = new byte[coinbase1Size];

            for (int i = 0; i < coinbase1Size; i++)
            {
                buff[i] = this.coinbaseTransactionBytes[i];
            }

            return buff;
        }
    }

    @Override
    public byte[] getExtraNonce1()
    {
        byte[] scriptBytes = this.getCoinbaseScriptBytes();

        if (scriptBytes == null)
            throw new IllegalStateException("No coinbase script data is available.");

        return Arrays.copyOfRange(scriptBytes, EXTRA1_OFF, EXTRA1_OFF + EXTRA1_BYTE_LENGTH);
    }

    @Override
    public byte[] getExtraNonce2()
    {
        byte[] scriptBytes = this.getCoinbaseScriptBytes();

        if (scriptBytes == null)
            throw new IllegalStateException("No coinbase script data is available.");

        return Arrays.copyOfRange(scriptBytes, EXTRA2_OFF, EXTRA2_OFF + EXTRA2_BYTE_LENGTH);
    }

    @Override
    public void setExtraNonce2(byte[] extraNonce2)
    {
        byte[] scriptBytes = this.getCoinbaseScriptBytes();

        if (scriptBytes == null)
        {
            throw new IllegalStateException("No coinbase script data is available.");
        }

        else if ((extraNonce2 == null) || (extraNonce2.length != EXTRA2_BYTE_LENGTH))
        {
            throw new IllegalArgumentException(
                String.format("Extra nonce #2 must be exactly %d bytes.", EXTRA2_BYTE_LENGTH));
        }

        for (int i = 0; i < extraNonce2.length; i++)
        {
            scriptBytes[EXTRA2_OFF + i] = extraNonce2[i];
        }

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(
                String.format(
                    "setExtraNonce2() - extraNonce2 bytes (%d): %s",
                    extraNonce2.length,
                    Hex.encodeHexString(extraNonce2)));
        }

        this.setCoinbaseScriptBytes(scriptBytes);
    }

    @Override
    public byte[] getCoinbasePart2()
    throws IllegalStateException
    {
        if (this.coinbaseTransactionBytes == null)
        {
            throw new IllegalStateException("No coinbase transaction data is available.");
        }

        else
        {
            //So coinbase1 size - extranonce(1+8)
            int     sz      = this.coinbaseTransactionBytes.length - TX_HEADER_LENGTH - 4 - 8;
            byte[]  buff    = new byte[sz];

            for (int i = 0; i < sz; i++)
            {
                buff[i] = this.coinbaseTransactionBytes[TX_HEADER_LENGTH + 8 + 4 + i];
            }

            return buff;
        }
    }

    @Override
    public void markSolved()
    {
        // Default implementation: no-op
    }

    protected void setExtraNonce1(byte[] extraNonce1)
    {
        byte[] scriptBytes = this.getCoinbaseScriptBytes();

        if (scriptBytes == null)
        {
            throw new IllegalStateException("No coinbase script data is available.");
        }

        else if ((extraNonce1 == null) || (extraNonce1.length != EXTRA1_BYTE_LENGTH))
        {
            throw new IllegalArgumentException(
                String.format("Extra nonce #1 must be exactly %d bytes.", EXTRA1_BYTE_LENGTH));
        }

        for (int i = 0; i < extraNonce1.length; i++)
        {
            scriptBytes[EXTRA1_OFF + i] = extraNonce1[i];
        }

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(
                String.format(
                    "setExtraNonce1() - extraNonce1 bytes (%d): %s",
                    extraNonce1.length,
                    Hex.encodeHexString(extraNonce1)));
        }

        this.setCoinbaseScriptBytes(scriptBytes);
    }

    protected void setCoinbaseTransaction(Transaction coinbaseTransaction)
    {
        this.coinbaseTransaction        = coinbaseTransaction;
        this.coinbaseTransactionBytes   = coinbaseTransaction.bitcoinSerialize();

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(
                String.format(
                    "setCoinbaseTransactionBytes() - Coinbase TX bytes (%d): %s",
                    this.coinbaseTransactionBytes.length,
                    Hex.encodeHexString(this.coinbaseTransactionBytes)));
        }

        this.refreshCoinbaseScriptBytes();
    }

    protected void setCoinbaseTransactionBytes(byte[] coinbaseTransactionBytes)
    throws ProtocolException
    {
        this.coinbaseTransactionBytes   = coinbaseTransactionBytes;
        this.coinbaseTransaction        = new Transaction(this.networkParams, coinbaseTransactionBytes);

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(
                String.format(
                    "setCoinbaseTransactionBytes() - Coinbase TX bytes (%d): %s",
                    coinbaseTransactionBytes.length,
                    Hex.encodeHexString(coinbaseTransactionBytes)));
        }

        this.refreshCoinbaseScriptBytes();
    }

    protected byte[] getCoinbaseScriptBytes()
    {
        return this.coinbaseScriptBytes;
    }

    protected void setCoinbaseScriptBytes(byte[] scriptBytes)
    {
        this.coinbaseScriptBytes = scriptBytes;

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(
                String.format(
                    "setCoinbaseScriptBytes() - Coinbase script bytes (%d): %s",
                    scriptBytes.length,
                    Hex.encodeHexString(scriptBytes)));
        }
    }

    protected void refreshCoinbaseScriptBytes()
    {
        if (!this.coinbaseTransaction.getInputs().isEmpty())
            this.setCoinbaseScriptBytes(this.coinbaseTransaction.getInput(0).getScriptBytes());

        if (LOGGER.isDebugEnabled() && (this.coinbaseTransactionBytes != null))
        {
            LOGGER.debug(
                String.format(
                    "setCoinbaseTransactionBytes() - Coinbase Part #1: %s",
                    Hex.encodeHexString(this.getCoinbasePart1())));

            LOGGER.debug(
                String.format(
                    "setCoinbaseTransactionBytes() - Coinbase Extra Nonce #1: %s",
                    Hex.encodeHexString(this.getExtraNonce1())));

            LOGGER.debug(
                String.format(
                    "setCoinbaseTransactionBytes() - Coinbase Extra Nonce #2: %s",
                    Hex.encodeHexString(this.getExtraNonce2())));

            LOGGER.debug(
                String.format(
                    "setCoinbaseTransactionBytes() - Coinbase Part #2: %s",
                    Hex.encodeHexString(this.getCoinbasePart2())));
        }
    }
}