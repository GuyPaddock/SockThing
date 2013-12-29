package com.redbottledesign.bitcoin.pool.rpc.bitcoin;

import com.github.fireduck64.sockthing.PoolUser;
import com.github.fireduck64.sockthing.StratumServer;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Transaction;

public abstract class AbstractCoinbase
implements Coinbase
{
    protected static final int TX_HEADER_LENGTH = 42;

    protected static final int BLOCK_HEIGHT_BYTE_LENGTH = 4;
    protected static final int BLOCK_HEIGHT_OFF = 0;

    protected static final int EXTRA1_OFF = 4;
    protected static final int EXTRA1_BYTE_LENGTH = 4;

    protected static final int EXTRA2_OFF = 8;
    protected static final int EXTRA2_BYTE_LENGTH = 4;

    private final StratumServer server;
    private final PoolUser poolUser;
    private final NetworkParameters networkParams;
    private byte[] coinbaseTransactionBytes;
    private Transaction coinbaseTransaction;
    private byte[] coinbaseScriptBytes;
    private byte[] extraNonce1;
    private byte[] extraNonce2;

    public AbstractCoinbase(StratumServer server, PoolUser poolUser)
    {
        this.server         = server;
        this.poolUser       = poolUser;
        this.networkParams  = server.getNetworkParameters();
    }

    public StratumServer getServer()
    {
        return this.server;
    }

    public PoolUser getPoolUser()
    {
        return this.poolUser;
    }

    /* (non-Javadoc)
     * @see com.redbottledesign.bitcoin.pool.rpc.bitcoin.Coinbase#getCoinbaseTransaction()
     */
    @Override
    public Transaction getCoinbaseTransaction()
    {
        return this.coinbaseTransaction;
    }

    /* (non-Javadoc)
     * @see com.redbottledesign.bitcoin.pool.rpc.bitcoin.Coinbase#getCoinbaseTransactionBytes()
     */
    @Override
    public byte[] getCoinbaseTransactionBytes()
    {
        return this.coinbaseTransactionBytes;
    }

    /* (non-Javadoc)
     * @see com.redbottledesign.bitcoin.pool.rpc.bitcoin.Coinbase#getCoinbasePart1()
     */
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

    /* (non-Javadoc)
     * @see com.redbottledesign.bitcoin.pool.rpc.bitcoin.Coinbase#getExtraNonce1()
     */
    @Override
    public byte[] getExtraNonce1()
    {
        return this.extraNonce1;
    }

    /* (non-Javadoc)
     * @see com.redbottledesign.bitcoin.pool.rpc.bitcoin.Coinbase#getExtraNonce2()
     */
    @Override
    public byte[] getExtraNonce2()
    {
        return this.extraNonce2;
    }

    /* (non-Javadoc)
     * @see com.redbottledesign.bitcoin.pool.rpc.bitcoin.Coinbase#setExtraNonce2(byte[])
     */
    @Override
    public void setExtraNonce2(byte[] extraNonce2)
    {
        if ((extraNonce2 == null) || (extraNonce2.length != EXTRA2_BYTE_LENGTH))
        {
            throw new IllegalArgumentException(
                String.format("Extra nonce #2 must be exactly %d bytes.", EXTRA2_BYTE_LENGTH));
        }

        for (int i = 0; i < extraNonce2.length; i++)
        {
            this.coinbaseScriptBytes[i + EXTRA2_OFF] = extraNonce2[i];
        }
    }

    /* (non-Javadoc)
     * @see com.redbottledesign.bitcoin.pool.rpc.bitcoin.Coinbase#getCoinbasePart2()
     */
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
                buff[i] = this.coinbaseTransactionBytes[i + TX_HEADER_LENGTH + 8 + 4];
            }

            return buff;
        }
    }

    @Override
    public abstract void regenerateCoinbaseTransaction();

    @Override
    public void markSolved()
    {
        // Default implementation: no-op
    }

    protected void setExtraNonce1(byte[] extraNonce1)
    {
        if ((extraNonce1 == null) || (extraNonce1.length != EXTRA1_BYTE_LENGTH))
        {
            throw new IllegalArgumentException(
                String.format("Extra nonce #1 must be exactly %d bytes.", EXTRA1_BYTE_LENGTH));
        }

        for (int i = 0; i < extraNonce1.length; i++)
        {
            this.coinbaseScriptBytes[i + EXTRA1_OFF] = extraNonce1[i];
        }
    }

    protected void setCoinbaseTransaction(Transaction coinbaseTransaction)
    {
        this.coinbaseTransaction = coinbaseTransaction;
    }

    protected void setCoinbaseTransactionBytes(byte[] coinbaseTransactionBytes)
    {
        this.coinbaseTransactionBytes = coinbaseTransactionBytes;
    }

    protected byte[] getCoinbaseScriptBytes()
    {
        return this.coinbaseScriptBytes;
    }

    protected void setCoinbaseScriptBytes(byte[] scriptBytes)
    {
        this.coinbaseScriptBytes = scriptBytes;
    }
}