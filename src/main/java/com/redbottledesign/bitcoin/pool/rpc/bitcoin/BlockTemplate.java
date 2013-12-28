package com.redbottledesign.bitcoin.pool.rpc.bitcoin;

import java.math.BigInteger;
import java.util.List;

import com.google.bitcoin.core.Coinbase;
import com.google.bitcoin.core.Transaction;

public interface BlockTemplate
{
    public long getHeight();

    public double getDifficulty();

    public String getDifficultyBits();

    public String getPreviousBlockHash();

    public int getCurrentTime();

    public String getTarget();

    public boolean hasBlockReward();

    public BigInteger getReward();

    public BigInteger getTotalFees();

    public List<Transaction> getTransactions();

    public List<Transaction> getTransactions(Coinbase coinbase);
}
