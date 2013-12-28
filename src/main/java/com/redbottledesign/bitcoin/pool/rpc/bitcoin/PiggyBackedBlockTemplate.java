package com.redbottledesign.bitcoin.pool.rpc.bitcoin;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.json.JSONObject;

import com.github.fireduck64.sockthing.rpc.bitcoin.BitcoinDaemonBlockTemplate;
import com.github.fireduck64.sockthing.rpc.bitcoin.MalformedBlockTemplateException;
import com.google.bitcoin.core.NetworkParameters;
import com.redbottledesign.util.BitcoinUnit;

public class PiggyBackedBlockTemplate
extends BitcoinDaemonBlockTemplate
{
    protected static final BigDecimal NORMAL_BLOCK_REWARD = new BigDecimal(25);

    public PiggyBackedBlockTemplate(NetworkParameters networkParams, JSONObject jsonBlockTemplate)
    {
        super(networkParams, jsonBlockTemplate);
    }

    @Override
    public boolean hasBlockReward()
    {
        return false;
    }

    @Override
    public BigInteger getReward()
    throws MalformedBlockTemplateException
    {
        BigDecimal rewardInSatoshis = BitcoinUnit.SATOSHIS.convert(NORMAL_BLOCK_REWARD, BitcoinUnit.BITCOINS);

        // (25 * 1 / Network Difficulty)
        return rewardInSatoshis.multiply(
            BigDecimal.ONE.divide(BigDecimal.valueOf(this.getDifficulty()))).toBigInteger();
    }
}