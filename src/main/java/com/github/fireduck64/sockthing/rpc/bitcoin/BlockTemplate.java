package com.github.fireduck64.sockthing.rpc.bitcoin;

import java.math.BigInteger;
import java.util.List;

import com.google.bitcoin.core.Coinbase;
import com.google.bitcoin.core.Transaction;

public interface BlockTemplate
{
    public long getHeight();

    public String getDifficultyBits();

    public String getPreviousBlockHash();

    public int getCurrentTime();

    public String getTarget();

    public BigInteger getBlockReward();

    public BigInteger getTotalFees();

    public List<Transaction> getTransactions();

    public List<Transaction> getTransactions(Coinbase coinbase);
}
