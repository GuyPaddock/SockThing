package com.redbottledesign.bitcoin.pool.coinbase;
import com.google.bitcoin.core.Transaction;

/**
 * <p>An interface for representing mining pool coinbase transactions.</p>
 *
 * <p>The coinbase transaction is the first transaction in a bitcoin block, and
 * is the transaction that credits the worker who solves the block with the
 * block reward and block transaction fees.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 */
public interface Coinbase
{
    /**
     * <p>Gets the first part of the coinbase transaction.</p>
     *
     * <p>This part of the coinbase contains the transaction header and
     * coinbase "script", which contains any extra data that has been added by
     * the mining pool along with extra nonces #1 and #2 that were inserted by
     * the miner.</p>
     *
     * @return  The first part of the coinbase transaction.
     */
    public abstract byte[] getCoinbasePart1();

    /**
     * <p>Gets the offset, in bytes, from the start of the coinbase
     * transaction to the first part of the coinbase transaction.</p>
     *
     * <p>In sane implementations, this will always be {@code 0}.</p>
     *
     * @return  The offset from the start of the coinbase transaction to the
     *          first part of the coinbase.
     */
    public abstract int getCoinbase1Offset();

    /**
     * <p>Gets the length, in bytes, of the first part of the coinbase
     * transaction.</p>
     *
     * @return  The length of the first part of the coinbase.
     */
    public abstract int getCoinbase1Length();

    /**
     * <p>Gets the bytes that represent the first extra nonce value.</p>
     *
     * <p>This value is assigned by the mining pool and cannot be modified by
     * workers.</p>
     *
     * @return  The first extra nonce value.
     */
    public abstract byte[] getExtraNonce1();

    /**
     * <p>Gets the offset, in bytes, from the start of the coinbase "script"
     * to the first extra nonce.</p>
     *
     * <p>The "script" is the arbitrary data that a mining pool inserts
     * as the input of the coinbase transaction.</p>
     *
     * @return  The offset from the start of the coinbase script to the first
     *          extra nonce.
     */
    public abstract int getExtraNonce1Offset();

    /**
     * <p>Gets the length, in bytes, of the first extra nonce value.</p>
     *
     * @return  The length of the first extra nonce.
     */
    public abstract int getExtraNonce1Length();

    /**
     * <p>Gets the bytes that represent the second extra nonce value.</p>
     *
     * <p>This value is set and incremented by each worker during hashing.</p>
     *
     * @return  The second extra nonce value.
     */
    public abstract byte[] getExtraNonce2();

    /**
     * <p>Gets the offset, in bytes, from the start of the coinbase "script"
     * to the first extra nonce.</p>
     *
     * <p>The "script" is the arbitrary data that a mining pool inserts
     * as the input of the coinbase transaction.</p>
     *
     * @return  The offset from the start of the coinbase transaction to the
     *          second extra nonce.
     */
    public abstract int getExtraNonce2Offset();

    /**
     * <p>Gets the length, in bytes, of the second extra nonce value.</p>
     *
     * <p>This length is prescribed by the mining pool and cannot be modified
     * by workers.</p>
     *
     * @return  The length of the second extra nonce.
     */
    public abstract int getExtraNonce2Length();

    /**
     * <p>Sets the second extra nonce value.</p>
     *
     * <p>This value is set and incremented by each worker during hashing.</p>
     *
     * <p>This method will modify the coinbase transaction bytes and invalidate
     * the coinbase {@link Transaction} returned by
     * {@link #getCoinbaseTransaction()}. You must call
     * {@link #regenerateCoinbaseTransaction()} after calling this method if you
     * will be calling {@linkplain #getCoinbaseTransaction()}.</p>
     *
     * @param   extraNonce2
     *          The new extra nonce value.
     */
    public abstract void setExtraNonce2(byte[] extraNonce2);

    /**
     * <p>Gets the second part of the coinbase transaction.</p>
     *
     * <p>This part of the coinbase follows the "extra" nonces and contains the
     * outputs of the transaction, which at a minimum must include a payout to
     * the mining pool's wallet address.</p>
     *
     * @return  The second part of the coinbase transaction.
     */
    public abstract byte[] getCoinbasePart2();

    /**
     * <p>Gets the offset, in bytes, from the start of the coinbase
     * transaction to the second part of the coinbase transaction.</p>
     *
     * @return  The offset from the start of the coinbase transaction to the
     *          second part of the coinbase.
     */
    public abstract int getCoinbase2Offset();

    /**
     * <p>Gets the length, in bytes, of the second part of the coinbase
     * transaction.</p>
     *
     * @return  The length of the second part of the coinbase.
     */
    public abstract int getCoinbase2Length();

    /**
     * <p>Regenerates the coinbase {@link Transaction} object from the coinbase
     * transaction bytes, which may have been modified by prior calls to
     * {@link #setExtraNonce2(byte[])} and other setters on this object.</p>
     *
     * <p>This method should always be called before calling
     * {@link #getCoinbaseTransaction()} if coinbase transaction bytes have
     * been modified.</p>
     */
    public abstract void regenerateCoinbaseTransaction();

    /**
     * <p>Gets a {@link Transaction} object that represents the complete
     * coinbase transaction, which has been built through this interface.</p>
     *
     * @return  The coinbase transaction.
     *
     * @throws  IllegalStateException
     *          If {@link #regenerateCoinbaseTransaction()} has not been called
     *          before calling this method after modifying modifying coinbase
     *          transaction bytes through this interface.
     */
    public abstract Transaction getCoinbaseTransaction()
    throws IllegalStateException;

    /**
     * Gets the bytes that represent the coinbase transaction.
     *
     * @return  The coinbase transaction bytes.
     */
    public abstract byte[] getCoinbaseTransactionBytes();

    /**
     * Gets the total length, in bytes, of the coinbase transaction.
     *
     * @return  The length of the coinbase transaction in number of bytes.
     */
    public abstract int getTotalCoinbaseTransactionLength();
}
