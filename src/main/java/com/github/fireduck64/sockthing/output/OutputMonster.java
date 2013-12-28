
package com.github.fireduck64.sockthing.output;

import java.math.BigInteger;

import com.github.fireduck64.sockthing.PoolUser;
import com.google.bitcoin.core.Transaction;


public interface OutputMonster
{
    public void addOutputs(PoolUser pu, Transaction tx, BigInteger rewardValue, BigInteger feeValue);
}
