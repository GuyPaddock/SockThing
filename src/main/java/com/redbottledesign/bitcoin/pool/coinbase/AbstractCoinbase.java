package com.redbottledesign.bitcoin.pool.coinbase;

import java.util.Arrays;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.ProtocolException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;

/**
 * <p>Abstract base implementation for {@link Coinbase} implementations.</p>
 *
 * <p>This base implementation is optional. {@linkplain Coinbase} consumers should
 * only refer to the {@linkplain Coinbase} interface and never make a direct
 * reference to this class.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 */
public abstract class AbstractCoinbase
implements Coinbase
{
    /**
     * The length, in bytes, of a bitcoin transaction header.
     */
    protected static final int TX_HEADER_LENGTH = 42;

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCoinbase.class);

    /**
     * The parameters of the Bitcoin network the coinbase is being generated for.
     */
    private final NetworkParameters networkParameters;

    /**
     * The raw bytes that represent the coinbase transaction.
     */
    private byte[] coinbaseTransactionBytes;

    /**
     * <p>The coinbase transaction represented as a {@link Transaction} object.</p>
     *
     * <p>For performance reasons, this representation of the transaction is
     * not automatically kept in sync as the raw bytes of the coinbase
     * transaction are modified. Instead, this representation will be
     * invalidated (i.e. set to {@code null}) whenever it becomes stale.
     * This forces consumers to have to call
     * {@link #regenerateCoinbaseTransaction()} whenever they are ready to
     * regenerate this representation after modifying the coinbase transaction
     * bytes.</p>
     */
    private Transaction coinbaseTransaction;

    /**
     * <p>The bytes of the coinbase "script".</p>
     *
     * <p>The "script" is the arbitrary data that a mining pool inserts
     * as the input of the coinbase transaction.</p>
     */
    private byte[] coinbaseScriptBytes;

    /**
     * <p>Constructor for {@link AbstractCoinbase} that initializes a new
     * abstract coinbase, configured for the network with the specified
     * parameters.</p>
     *
     * @param   networkParameters
     *          The parameters of the Bitcoin network the coinbase is being
     *          generated for.
     */
    public AbstractCoinbase(NetworkParameters networkParameters)
    {
        this.networkParameters = networkParameters;
    }

    /**
     * {@inheritDoc}
     *
     * @throws  IllegalStateException
     *          If the coinbase transaction bytes have not yet been supplied.
     */
    @Override
    public byte[] getCoinbasePart1()
    throws IllegalStateException
    {
        int     coinbase1Offset,
                coinbase1Size;
        byte[]  coinbasePart1;

        this.assertTransactionBytesAreAvailable();

        coinbase1Offset = this.getCoinbase1Offset();
        coinbase1Size   = this.getCoinbase1Length();
        coinbasePart1   = new byte[coinbase1Size];

        for (int byteIndex = 0; byteIndex < coinbase1Size; byteIndex++)
        {
            coinbasePart1[byteIndex] = this.coinbaseTransactionBytes[coinbase1Offset + byteIndex];
        }

        return coinbasePart1;
    }

    /**
     * {@inheritDoc}
     *
     * @throws  IllegalStateException
     *          If the coinbase script bytes have not yet been supplied.
     */
    @Override
    public byte[] getExtraNonce1()
    {
        byte[]  scriptBytes;
        int     extraNonce1Offset,
                extraNonce1Size;

        this.assertScriptBytesAreAvailable();

        scriptBytes         = this.getCoinbaseScriptBytes();
        extraNonce1Offset   = this.getExtraNonce1Offset();
        extraNonce1Size     = this.getExtraNonce1Length();

        return Arrays.copyOfRange(scriptBytes, extraNonce1Offset, extraNonce1Offset + extraNonce1Size);
    }

    /**
     * {@inheritDoc}
     *
     * @throws  IllegalStateException
     *          If the coinbase script bytes have not yet been supplied.
     */
    @Override
    public byte[] getExtraNonce2()
    {
        byte[]  scriptBytes;
        int     extraNonce2Offset,
                extraNonce2Size;

        this.assertScriptBytesAreAvailable();

        scriptBytes         = this.getCoinbaseScriptBytes();
        extraNonce2Offset   = this.getExtraNonce2Offset();
        extraNonce2Size     = this.getExtraNonce2Length();

        return Arrays.copyOfRange(scriptBytes, extraNonce2Offset, extraNonce2Offset + extraNonce2Size);
    }

    /**
     * {@inheritDoc}
     *
     * @throws  IllegalStateException
     *          If the coinbase script bytes have not yet been supplied.
     */
    @Override
    public void setExtraNonce2(byte[] extraNonce2)
    {
        byte[]  scriptBytes;
        int     extraNonce2Offset,
                extraNonce2Size;

        this.assertScriptBytesAreAvailable();

        scriptBytes         = this.getCoinbaseScriptBytes();
        extraNonce2Offset   = this.getExtraNonce2Offset();
        extraNonce2Size     = this.getExtraNonce2Length();

        if ((extraNonce2 == null) || (extraNonce2.length != extraNonce2Size))
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

    /**
     * {@inheritDoc}
     *
     * @throws  IllegalStateException
     *          If the coinbase transaction bytes have not yet been supplied.
     */
    @Override
    public byte[] getCoinbasePart2()
    throws IllegalStateException
    {
        int     coinbase2Offset,
                coinbase2Size;
        byte[]  coinbasePart2;

        this.assertTransactionBytesAreAvailable();

        coinbase2Offset = this.getCoinbase2Offset();
        coinbase2Size   = this.getCoinbase2Length();
        coinbasePart2   = new byte[coinbase2Size];

        for (int byteIndex = 0; byteIndex < coinbase2Size; byteIndex++)
        {
            coinbasePart2[byteIndex] = this.coinbaseTransactionBytes[coinbase2Offset + byteIndex];
        }

        return coinbasePart2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void regenerateCoinbaseTransaction()
    {
        Transaction newCoinbase;

        try
        {
            newCoinbase = new Transaction(this.networkParameters, this.coinbaseTransactionBytes);
        }

        catch (ProtocolException ex)
        {
            throw new RuntimeException("Unable to generate coinbase transaction: " + ex.getMessage(), ex);
        }

        /* Clear any inputs that were already in the TX bytes, then rebuild
         * the input from the script bytes.
         */
        newCoinbase.clearInputs();
        newCoinbase.addInput(
            new TransactionInput(
                this.networkParameters,
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Transaction getCoinbaseTransaction()
    throws IllegalStateException
    {
        this.assertCoinbaseTransactionIsAvailable();

        return this.coinbaseTransaction;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getCoinbaseTransactionBytes()
    {
        return this.coinbaseTransactionBytes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTotalCoinbaseTransactionLength()
    {
        return this.coinbaseTransactionBytes.length;
    }

    /**
     * Gets the parameters of the network for which this coinbase is being
     * generated.
     *
     * @return  The parameters of the network.
     */
    protected NetworkParameters getNetworkParameters()
    {
        return this.networkParameters;
    }

    /**
     * <p>Sets the first extra nonce value.</p>
     *
     * <p>This value is typically assigned by a mining pool and should not be
     * modified by workers.</p>
     *
     * <p>This method will modify the coinbase transaction bytes and invalidate
     * the coinbase {@link Transaction} returned by
     * {@link #getCoinbaseTransaction()}. You must call
     * {@link #regenerateCoinbaseTransaction()} after calling this method if you
     * will be calling {@linkplain #getCoinbaseTransaction()}.</p>
     *
     * @param   extraNonce1
     *          The new first extra nonce value.
     */
    protected void setExtraNonce1(byte[] extraNonce1)
    {
        byte[]  scriptBytes         = this.getCoinbaseScriptBytes();
        int     extraNonce1Offset   = this.getExtraNonce1Offset(),
                extraNonce1Size     = this.getExtraNonce1Length();

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

    /**
     * <p>Populates this object with information from the provided coinbase
     * {@link Transaction}.</p>
     *
     * <p>The provided transaction is serialized out to raw bytes that are then
     * used to populate the state of this object.</p>
     *
     * <p>The same coinbase transaction instance that is provided to this
     * method will be returned by subsequent calls to
     * {@link #getCoinbaseTransaction()} until such time as the transaction is
     * invalidated by a call to a method on this object that modifies the raw
     * bytes of the coinbase transaction.</p>
     *
     * @param   coinbaseTransaction
     *          The transaction from which to populate this object's state.
     */
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

    /**
     * <p>Sets the raw bytes of the coinbase transaction and regenerates the
     * corresponding {@link Transaction} object representation.</p>
     *
     * <p>This method automatically generates a new coinbase
     * {@link Transaction} object for {@link #getCoinbaseTransaction()}. You do
     * not need to call {@link #regenerateCoinbaseTransaction()} after calling
     * this method until you later modify the coinbase transaction bytes.</p>
     *
     * @param   coinbaseTransactionBytes
     *          The new bytes for the coinbase transaction.
     *
     * @throws  ProtocolException
     *          If the transaction bytes cannot be interpreted as a valid
     *          transaction on the network for which this coinbase has been
     *          configured.
     */
    protected void setCoinbaseTransactionBytes(byte[] coinbaseTransactionBytes)
    throws ProtocolException
    {
        this.setCoinbaseTransactionBytes(coinbaseTransactionBytes, true);
    }

    /**
     * <p>Sets the raw bytes of the coinbase transaction and optionally
     * regenerates the corresponding {@link Transaction} object
     * representation.</p>
     *
     * <p>Depending upon whether {@code regenerateTransaction} is {@code true}
     * or {@code false}, this method can either generates a new coinbase
     * {@link Transaction} object for {@link #getCoinbaseTransaction()}, or
     * invalidate the existing coinbase. In the latter case, a subsequent call
     * to {@link #regenerateCoinbaseTransaction()} will be required before
     * {@link #getCoinbaseTransaction()} is called.</p>
     *
     * <p>
     *
     * @param   coinbaseTransactionBytes
     *          The new bytes for the coinbase transaction.
     *
     * @param   regenerateTransaction
     *          {@code true} if the coinbase {@link Transaction} object that is
     *          returned by {@link #getCoinbaseTransaction()} should be
     *          regenerated automatically. If {@code false}, the object will be
     *          invalidated without being regenerated.
     *
     * @throws  ProtocolException
     *          If the transaction bytes cannot be interpreted as a valid
     *          transaction on the network for which this coinbase has been
     *          configured.
     */
    protected void setCoinbaseTransactionBytes(byte[] coinbaseTransactionBytes, boolean regenerateTransaction)
    throws ProtocolException
    {
        this.coinbaseTransactionBytes = coinbaseTransactionBytes;

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(
                String.format(
                    "setCoinbaseTransactionBytes() - Coinbase TX bytes (%d): %s",
                    coinbaseTransactionBytes.length,
                    Hex.encodeHexString(coinbaseTransactionBytes)));
        }

        if (regenerateTransaction)
        {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("setCoinbaseTransactionBytes() - Regenerating coinbase TX.");

            this.coinbaseTransaction = new Transaction(this.networkParameters, coinbaseTransactionBytes);

            this.refreshCoinbaseScriptBytes();
        }

        else
        {
            this.invalidateCoinbaseTransaction();
        }
    }

    /**
     * <p>Gets the raw bytes that represent the coinbase "script".</p>
     *
     * <p>The "script" is the arbitrary data that a mining pool inserts
     * as the input of the coinbase transaction.</p>
     *
     * @return  The script bytes.
     */
    protected byte[] getCoinbaseScriptBytes()
    {
        return this.coinbaseScriptBytes;
    }

    /**
     * <p>Sets the raw bytes that represent the coinbase "script".</p>
     *
     * <p>The "script" is the arbitrary data that a mining pool inserts
     * as the input of the coinbase transaction.</p>
     *
     * <p>This method will modify the coinbase script bytes and invalidate
     * the coinbase {@link Transaction} returned by
     * {@link #getCoinbaseTransaction()}. You must call
     * {@link #regenerateCoinbaseTransaction()} after calling this method if you
     * will be calling {@linkplain #getCoinbaseTransaction()}.</p>
     *
     * @param   scriptBytes
     *          The new script bytes.
     */
    protected void setCoinbaseScriptBytes(byte[] scriptBytes)
    {
        this.setCoinbaseScriptBytes(scriptBytes, true);
    }

    /**
     * <p>Sets the raw bytes that represent the coinbase "script", optionally
     * invalidating the coinbase {@link Transaction} in the process.</p>
     *
     * <p>The "script" is the arbitrary data that a mining pool inserts
     * as the input of the coinbase transaction.</p>
     *
     * <p>This method will modify the coinbase script bytes and can optionally
     * invalidate the coinbase {@link Transaction} returned by
     * {@link #getCoinbaseTransaction()}. Unless you know what you are doing,
     * it is recommended that you call {@link #setCoinbaseScriptBytes(byte[])}
     * instead of calling this method directly.</p>
     *
     * @param   scriptBytes
     *          The new script bytes.
     *
     * @param   invalidateCoinbaseTransaction
     *          If {@code true}, the coinbase transaction is invalidated.
     *          If {@code false}, the coinbase transaction is not invalidated.
     */
    protected void setCoinbaseScriptBytes(byte[] scriptBytes, boolean invalidateCoinbaseTransaction)
    {
        this.coinbaseScriptBytes = scriptBytes;

        if (invalidateCoinbaseTransaction)
            this.invalidateCoinbaseTransaction();

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(
                String.format(
                    "setCoinbaseScriptBytes() - Coinbase script bytes (%d): %s",
                    scriptBytes.length,
                    Hex.encodeHexString(scriptBytes)));
        }
    }

    /**
     * <p>Extracts the raw bytes of the coinbase "script" from the coinbase
     * {@link Transaction} object.</p>
     *
     * @throws  IllegalStateException
     *          If the coinbase transaction is not available.
     */
    protected void refreshCoinbaseScriptBytes()
    throws IllegalStateException
    {
        this.assertCoinbaseTransactionIsAvailable();

        if (!this.coinbaseTransaction.getInputs().isEmpty())
        {
            /* Ensure we don't invalidate the coinbase TX, since it will match
             * the script bytes after this call.
             */
            this.setCoinbaseScriptBytes(this.coinbaseTransaction.getInput(0).getScriptBytes(), false);
        }
    }

    /**
     * Invalidates the current {@link Transaction} object representation of the
     * coinbase bytes.
     */
    protected void invalidateCoinbaseTransaction()
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(
                "invalidateCoinbaseTransaction() - Invalidating coinbase TX. regenerateCoinbaseTransaction() will " +
                "need to be called to rebuild it from the new script bytes.");
        }

        // Invalidate the coinbase TX, forcing the caller to regenerate it.
        this.coinbaseTransaction = null;
    }

    /**
     * Convenience method for asserting that the coinbase {@link Transaction}
     * representation of the coinbase bytes is valid and available.
     *
     * @throws  IllegalStateException
     *          If the coinbase transaction is not available / has not been
     *          regenerated.
     */
    protected void assertCoinbaseTransactionIsAvailable()
    throws IllegalStateException
    {
        if (this.coinbaseTransaction == null)
        {
            throw new IllegalStateException(
                "No valid coinbase transaction is available. It must be refreshed by calling " +
                "regenerateCoinbaseTransaction().");
        }
    }

    /**
     * Convenience method for asserting that raw transaction bytes have been
     * populated in this instance.
     *
     * @throws  IllegalStateException
     *          If the coinbase transaction bytes have not yet been supplied.
     */
    protected void assertTransactionBytesAreAvailable()
    throws IllegalStateException
    {
        if (this.coinbaseTransactionBytes == null)
            throw new IllegalStateException("No coinbase transaction data is available.");
    }

    /**
     * Convenience method for asserting that raw script bytes have been
     * populated in this instance.
     *
     * @throws  IllegalStateException
     *          If the coinbase script bytes have not yet been supplied.
     */
    protected void assertScriptBytesAreAvailable()
    throws IllegalStateException
    {
        if (this.coinbaseScriptBytes == null)
            throw new IllegalStateException("No coinbase script data is available.");
    }
}
