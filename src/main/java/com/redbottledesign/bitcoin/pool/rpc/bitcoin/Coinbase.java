package com.redbottledesign.bitcoin.pool.rpc.bitcoin;

import com.google.bitcoin.core.Transaction;

public interface Coinbase
{
    public abstract Transaction getCoinbaseTransaction();

    public abstract byte[] getCoinbaseTransactionBytes();

    public abstract byte[] getCoinbasePart1()
    throws IllegalStateException;

    public abstract byte[] getExtraNonce1();

    public abstract byte[] getExtraNonce2();

    public abstract void setExtraNonce2(byte[] extraNonce2);

    public abstract byte[] getCoinbasePart2()
    throws IllegalStateException;

    public abstract void regenerateCoinbaseTransaction();

    public void markSolved();
}