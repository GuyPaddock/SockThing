package com.redbottledesign.bitcoin.pool.rpc.bitcoin;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.bitcoin.core.NetworkParameters;
import com.redbottledesign.util.BitcoinUnit;

public class PiggyBackedBlockTemplate
extends BitcoinDaemonBlockTemplate
{
    protected static final BigDecimal NORMAL_BLOCK_REWARD = new BigDecimal(25);

    private String workId;

    public PiggyBackedBlockTemplate(NetworkParameters networkParams, JSONObject jsonBlockTemplate)
    {
        super(networkParams, jsonBlockTemplate);

        this.setWorkId(jsonBlockTemplate);
    }

    public String getWorkId()
    {
        return this.workId;
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
        BigInteger rewardInSatoshis = BitcoinUnit.SATOSHIS.convert(NORMAL_BLOCK_REWARD, BitcoinUnit.BITCOINS).toBigInteger();

        // (25 * 1 / Network Difficulty)
        return rewardInSatoshis.multiply(BigInteger.ONE.divide(BigInteger.valueOf((long)this.getDifficulty())));
    }

    @Override
    public BigInteger getTotalFees()
    {
        // No fee info from upstream pools
        return BigInteger.ZERO;
    }

    protected void setWorkId(JSONObject jsonBlockTemplate)
    {
        if (jsonBlockTemplate.has("workid"))
        {
            try
            {
                this.workId = jsonBlockTemplate.getString("workid");
            }

            catch (JSONException ex)
            {
                throw new MalformedBlockTemplateException(
                    "Unable to parse work ID: " + ex.getMessage(),
                    ex);
            }
        }
    }
}