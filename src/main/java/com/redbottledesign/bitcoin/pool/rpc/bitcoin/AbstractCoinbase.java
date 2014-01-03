package com.redbottledesign.bitcoin.pool.rpc.bitcoin;

import java.util.Arrays;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.PoolUser;
import com.github.fireduck64.sockthing.StratumServer;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.ProtocolException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;

public abstract class AbstractCoinbase
implements Coinbase
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCoinbase.class);

    protected static final int TX_HEADER_LENGTH = 42;

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
    public int getTotalCoinbaseTransactionLength()
    {
        return this.coinbaseTransactionBytes.length;
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
            int     coinbase1Offset = this.getCoinbase1Offset(),
                    coinbase1Size   = this.getCoinbase1Size();
            byte[]  buff            = new byte[coinbase1Size];

            for (int i = 0; i < coinbase1Size; i++)
            {
                buff[i] = this.coinbaseTransactionBytes[coinbase1Offset + i];
            }

            return buff;
        }
    }

    @Override
    public byte[] getExtraNonce1()
    {
        byte[]  scriptBytes         = this.getCoinbaseScriptBytes();
        int     extraNonce1Offset   = this.getExtraNonce1Offset(),
                extraNonce1Size     = this.getExtraNonce1Size();

        if (scriptBytes == null)
            throw new IllegalStateException("No coinbase script data is available.");

        return Arrays.copyOfRange(scriptBytes, extraNonce1Offset, extraNonce1Offset + extraNonce1Size);
    }

    public void setExtraNonce1(byte[] extraNonce1)
    {
        byte[]  scriptBytes         = this.getCoinbaseScriptBytes();
        int     extraNonce1Offset   = this.getExtraNonce1Offset(),
                extraNonce1Size     = this.getExtraNonce1Size();

        if (scriptBytes == null)
        {
            throw new IllegalStateException("No coinbase script data is available.");
        }

        else if ((extraNonce1 == null) || (extraNonce1.length != extraNonce1Size))
        {
            throw new IllegalArgumentException(
                String.format("Extra nonce #1 must be exactly %d bytes.", extraNonce1Size));
        }

        for (int i = 0; i < extraNonce1.length; i++)
        {
            scriptBytes[extraNonce1Offset + i] = extraNonce1[i];
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

    @Override
    public byte[] getExtraNonce2()
    {
        byte[]  scriptBytes         = this.getCoinbaseScriptBytes();
        int     extraNonce2Offset   = this.getExtraNonce2Offset(),
                extraNonce2Size     = this.getExtraNonce2Size();

        if (scriptBytes == null)
            throw new IllegalStateException("No coinbase script data is available.");

        return Arrays.copyOfRange(scriptBytes, extraNonce2Offset, extraNonce2Offset + extraNonce2Size);
    }

    @Override
    public void setExtraNonce2(byte[] extraNonce2)
    {
        byte[]  scriptBytes         = this.getCoinbaseScriptBytes();
        int     extraNonce2Offset   = this.getExtraNonce2Offset(),
                extraNonce2Size     = this.getExtraNonce2Size();

        if (scriptBytes == null)
        {
            throw new IllegalStateException("No coinbase script data is available.");
        }

        else if ((extraNonce2 == null) || (extraNonce2.length != extraNonce2Size))
        {
            throw new IllegalArgumentException(
                String.format("Extra nonce #2 must be exactly %d bytes.", extraNonce2Size));
        }

        for (int i = 0; i < extraNonce2.length; i++)
        {
            scriptBytes[extraNonce2Offset + i] = extraNonce2[i];
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
            /* This contains our standard 42 byte transaction header then 4 bytes
             * of block height for block v2.
             */
            int     coinbase2Offset = this.getCoinbase2Offset(),
                    coinbase2Size   = this.getCoinbase2Size();
            byte[]  buff            = new byte[coinbase2Size];

            for (int i = 0; i < coinbase2Size; i++)
            {
                buff[i] = this.coinbaseTransactionBytes[coinbase2Offset + i];
            }

            return buff;
        }
    }

    @Override
    public void regenerateCoinbaseTransaction(PoolUser user)
    {
        Transaction newCoinbase;

        try
        {
            newCoinbase = new Transaction(this.networkParams, this.coinbaseTransactionBytes);
        }

        catch (ProtocolException ex)
        {
            throw new IllegalStateException("Unable to generate coinbase transaction: " + ex.getMessage(), ex);
        }

        /* Clear any inputs that were already in the TX bytes, then rebuild
         * the input from the script bytes.
         */
        newCoinbase.clearInputs();
        newCoinbase.addInput(
            new TransactionInput(
                this.networkParams,
                this.coinbaseTransaction,
                this.coinbaseScriptBytes));

        // Keep TX and script bytes in-sync
        this.setCoinbaseTransaction(newCoinbase);

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(
                String.format(
                    "regenerateCoinbaseTransaction() - Coinbase Part #1: %s",
                    Hex.encodeHexString(this.getCoinbasePart1())));

            LOGGER.debug(
                String.format(
                    "regenerateCoinbaseTransaction() - Coinbase Extra Nonce #1: %s",
                    Hex.encodeHexString(this.getExtraNonce1())));

            LOGGER.debug(
                String.format(
                    "regenerateCoinbaseTransaction() - Coinbase Extra Nonce #2: %s",
                    Hex.encodeHexString(this.getExtraNonce2())));

            LOGGER.debug(
                String.format(
                    "regenerateCoinbaseTransaction() - Coinbase Part #2: %s",
                    Hex.encodeHexString(this.getCoinbasePart2())));
        }
    }

    @Override
    public void markSolved()
    {
        // Default implementation: no-op
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
        this.setCoinbaseScriptBytes(scriptBytes, true);
    }

    protected void setCoinbaseScriptBytes(byte[] scriptBytes, boolean regenerateCoinbaseTransaction)
    {
        this.coinbaseScriptBytes = scriptBytes;

        if (regenerateCoinbaseTransaction)
            this.markCoinbaseForRegenerate();

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
        {
            /* Ensure we don't invalidate the coinbase TX, since it will match
             * the script bytes after this call.
             */
            this.setCoinbaseScriptBytes(this.coinbaseTransaction.getInput(0).getScriptBytes(), false);
        }
    }

    protected void markCoinbaseForRegenerate()
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(
                "markCoinbaseForRegenerate() - Invalidating coinbase TX. regenerateCoinbaseTransaction() will need " +
                "to be called to rebuild it from the new script bytes.");
        }

        // Invalidate the coinbase TX, forcing the caller to regenerate it.
        this.coinbaseTransaction = null;
    }
}