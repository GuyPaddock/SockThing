package com.redbottledesign.bitcoin.pool.coinbase;
import java.util.Arrays;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.ProtocolException;
import com.google.bitcoin.core.Transaction;

/**
 * <p>A {@link Coinbase} for constructing a Bitcoin coinbase transaction
 * according to specifications provided by an upstream mining pool.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 */
public class PrefabCoinbase
extends AbstractCoinbase
{
    /**
     * The offset, in bytes, from the start of the coinbase transaction to the
     * first part of the coinbase.
     */
    protected static final int COINBASE1_TX_OFFSET = 0;

    /**
     * The offset, in bytes, from the start of the coinbase transaction to the
     * byte that specifies the length, in bytes of the coinbase transaction.
     */
    protected static final int COINBASE_LENGTH_BYTE_OFFSET = TX_HEADER_LENGTH - 1;

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PrefabCoinbase.class);

    /**
     * The length, in bytes, of extra nonce #1.
     */
    private final int extraNonce1ByteLength;

    /**
     * The length, in bytes, of extra nonce #2.
     */
    private final int extraNonce2ByteLength;

    /**
     * <p>The original length of the coinbase "script", as it was originally
     * provided by the upstream pool.</p>
     *
     * <p>The "script" is the arbitrary data that a mining pool inserts
     * as the input of the coinbase transaction.</p>
     */
    private int originalScriptLength;

    /**
     * <p>The raw bytes of coinbase part #2, as they were provided by the
     * upstream pool.</p>
     *
     * <p>This data must be used verbatim in the coinbase; it cannot be
     * modified without resulting in the coinbase being rejected by the
     * upstream pool.</p>
     */
    private byte[] coinbasePart2;

    /**
     * <p>Constructor for {@link PrefabCoinbase} that initializes a new
     * coinbase for the specified network; having the specified bytes for
     * coinbase parts #1 and #2; with the specified bytes for extra nonce
     * #1; and accommodating the specified number of bytes for extra nonce
     * #2.</p>
     *
     * <p>This constructor is preferred when working with a coinbase that has
     * been specified by a pool via the Stratum mining protocol.</p>
     *
     * @param   networkParameters
     *          The parameters of the Bitcoin network the coinbase is being
     *          generated for.
     *
     * @param   coinbasePart1
     *          The first part of the coinbase transaction, which contains any
     *          extra data that has been added by the mining pool, but before
     *          extra nonces #1 and #2 have been inserted.
     *
     * @param   extraNonce1
     *          The pool-supplied extra nonce value for the user to which this
     *          coinbase corresponds.
     *
     * @param   extraNonce2Length
     *          The number of bytes to accommodate for extra nonce #2.
     *
     * @param   coinbasePart2
     *          The raw bytes of coinbase part #2, as provided by the upstream
     *          pool.
     *
     * @throws  ProtocolException
     *          If the transaction bytes cannot be interpreted as a valid
     *          transaction on the network for which this coinbase has been
     *          configured.
     */
    public PrefabCoinbase(NetworkParameters networkParameters, byte[] coinbasePart1, byte[] extraNonce1,
                          int extraNonce2Length, byte[] coinbasePart2)
    throws ProtocolException
    {
        super(networkParameters);

        this.extraNonce1ByteLength  = extraNonce1.length;
        this.extraNonce2ByteLength  = extraNonce2Length;

        this.setCoinbaseTransactionBytes(coinbasePart1, extraNonce1, coinbasePart2);
    }

    /**
     * <p>Constructor for {@link PrefabCoinbase} that initializes a new
     * coinbase for the specified network, initialized from the contents of
     * the provided, pool-supplied coinbase transaction.</p>
     *
     * <p>The bytes of the transaction will be customized accordingly in order
     * to accommodate extra nonce #1 and #2 in the transaction "script"
     * bytes.</p>
     *
     * <p>The "script" is the arbitrary data that a mining pool inserts
     * as the input of the coinbase transaction.</p>
     *
     * <p>This constructor is preferred when working with a coinbase that has
     * been specified by a pool via the older Bitcoin
     * {@code getblocktemplate} protocol.</p>
     *
     * @param   networkParameters
     *          The parameters of the Bitcoin network the coinbase is being
     *          generated for.
     *
     * @param   coinbaseTransaction
     *          The transaction from which the coinbase will be initialized.
     *
     * @param   extraNonce1
     *          The pool-supplied extra nonce value for the user to which this
     *          coinbase corresponds.
     *
     * @param   extraNonce2Length
     *          The number of bytes to accommodate for extra nonce #2.
     */
    public PrefabCoinbase(NetworkParameters networkParameters, Transaction coinbaseTransaction, byte[] extraNonce1,
                          int extraNonce2Length)
    {
        super(networkParameters);

        this.extraNonce1ByteLength = extraNonce1.length;
        this.extraNonce2ByteLength = extraNonce2Length;

        this.setCoinbaseTransaction(coinbaseTransaction);

        this.extractCoinbase2();
        this.resizeCoinbaseScript();

        this.setExtraNonce1(extraNonce1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCoinbase1Offset()
    {
        return COINBASE1_TX_OFFSET;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCoinbase1Length()
    {
        return TX_HEADER_LENGTH + this.originalScriptLength;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getExtraNonce1Offset()
    {
        return this.originalScriptLength;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getExtraNonce1Length()
    {
        return this.extraNonce1ByteLength;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getExtraNonce2Offset()
    {
        return this.originalScriptLength + this.getExtraNonce1Length();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getExtraNonce2Length()
    {
        return this.extraNonce2ByteLength;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getCoinbasePart2()
    throws IllegalStateException
    {
        return this.coinbasePart2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCoinbase2Offset()
    {
        return this.getCoinbase1Offset() + this.getCoinbase1Length() + this.getExtraNonce1Length()
               + this.getExtraNonce2Length();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCoinbase2Length()
    {
        return this.coinbasePart2.length;
    }

    /**
     * Extracts the bytes of coinbase part #2 out of the script bytes of the
     * coinbase transaction, and then stores them away in this instance for
     * safe keeping.
     */
    protected void extractCoinbase2()
    {
        byte[]  scriptBytes  = this.getCoinbaseScriptBytes(),
                txBytes      = this.getCoinbaseTransactionBytes();

        this.coinbasePart2 = Arrays.copyOfRange(txBytes, TX_HEADER_LENGTH + scriptBytes.length, txBytes.length);
    }

    /**
     * <p>Takes note of the original size of the coinbase "script", and then
     * resizes the script buffer to accommodate extra nonces #1 and #2.</p>
     *
     * <p>The "script" is the arbitrary data that a mining pool inserts
     * as the input of the coinbase transaction.</p>
     */
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

    /**
     * <p>Sets the raw bytes of the coinbase transaction to the combination of
     * the provided coinbase parts #1 and #2, the provided extra nonce #1
     * value, and sufficient zero-padded space for extra nonce #2.</p>
     *
     * <p>The bytes of coinbase part #2 in this coinbase are also updated to
     * match what's provided for {@code coinbasePart2}.</p>
     *
     * @param   coinbasePart1
     *          The first part of the coinbase transaction, which contains any
     *          extra data that has been added by the mining pool, but before
     *          extra nonces #1 and #2 have been inserted.
     *
     * @param   extraNonce1
     *          The pool-supplied extra nonce value.
     *
     * @param   coinbasePart2
     *          The raw bytes of coinbase part #2.
     *
     * @throws  ProtocolException
     *          If the transaction bytes cannot be interpreted as a valid
     *          transaction on the network for which this coinbase has been
     *          configured.
     */
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

        // Coinbase part #1
        for (int byteIndex = 0; byteIndex < coinbasePart1Length; ++byteIndex)
        {
            coinbaseTransactionBytes[byteIndex] = coinbasePart1[byteIndex];
        }

        // Extra nonce #1
        for (int byteIndex = 0; byteIndex < extraNonce1Length; ++byteIndex)
        {
            coinbaseTransactionBytes[extraNonce1Offset + byteIndex] = extraNonce1[byteIndex];
        }

        // Zero-fill extra nonce #2
        for (int byteIndex = 0; byteIndex < extraNonce2Length; ++byteIndex)
        {
            coinbaseTransactionBytes[extraNonce2Offset + byteIndex] = 0;
        }

        // Coinbase part #2
        for (int byteIndex = 0; byteIndex < coinbasePart2.length; ++byteIndex)
        {
            coinbaseTransactionBytes[coinbasePart2Offset + byteIndex] = coinbasePart2[byteIndex];
        }

        this.setCoinbaseTransactionBytes(coinbaseTransactionBytes);

        this.coinbasePart2 = coinbasePart2;
    }
}