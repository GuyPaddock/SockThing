package com.redbottledesign.bitcoin.pool.rpc.bitcoin;

import com.github.fireduck64.sockthing.PoolUser;
import com.google.bitcoin.core.Transaction;

public interface Coinbase
{
    public abstract Transaction getCoinbaseTransaction();

    public abstract byte[] getCoinbaseTransactionBytes();

    public abstract int getTotalCoinbaseTransactionLength();

    public abstract byte[] getCoinbasePart1()
    throws IllegalStateException;

    public abstract int getCoinbase1Offset();

    public abstract int getCoinbase1Size();

    public abstract byte[] getExtraNonce1();

    public abstract int getExtraNonce1Offset();

    public abstract int getExtraNonce1Size();

    public abstract byte[] getExtraNonce2();

    public abstract int getExtraNonce2Offset();

    public abstract int getExtraNonce2Size();

    public abstract void setExtraNonce2(byte[] extraNonce2);

    public abstract byte[] getCoinbasePart2()
    throws IllegalStateException;

    public abstract int getCoinbase2Offset();

    public abstract int getCoinbase2Size();

    public abstract void regenerateCoinbaseTransaction(PoolUser user);

    public abstract void markSolved();
}