package com.redbottledesign.bitcoin.pool.rpc.bitcoin.piggyback;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.ProtocolException;
import com.google.bitcoin.core.Transaction;
import com.redbottledesign.bitcoin.pool.rpc.bitcoin.BitcoinDaemonBlockTemplate;
import com.redbottledesign.bitcoin.pool.rpc.bitcoin.MalformedBlockTemplateException;
import com.redbottledesign.util.BitcoinUnit;

public class PiggyBackedBlockTemplate
extends BitcoinDaemonBlockTemplate
{
    protected static final BigDecimal NORMAL_BLOCK_REWARD = new BigDecimal(25);

    private String workId;
    private Transaction coinbase;
    private byte[] coinbaseBytes;

    public PiggyBackedBlockTemplate(NetworkParameters networkParams, JSONObject jsonBlockTemplate)
    {
        super(networkParams, jsonBlockTemplate);

        this.coinbase       = null;
        this.coinbaseBytes  = null;

        this.parseTemplate(jsonBlockTemplate);
    }

    public Transaction getCoinbase()
    {
        return this.coinbase;
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

    protected void parseTemplate(JSONObject jsonBlockTemplate)
    {
        if (jsonBlockTemplate.has("coinbasetxn"))
        {
            try
            {
                JSONObject  jsonCoinbase        = jsonBlockTemplate.getJSONObject("coinbasetxn");
                char[]      jsonCoinbaseData    = jsonCoinbase.getString("data").toCharArray();

                this.coinbase       = new Transaction(this.networkParams, Hex.decodeHex(jsonCoinbaseData));
                this.coinbaseBytes  = this.coinbase.bitcoinSerialize();
            }

            catch (JSONException | ProtocolException | DecoderException ex)
            {
                throw new MalformedBlockTemplateException(
                    "Unable to parse coinbase transaction: " + ex.getMessage(),
                    ex);
            }
        }

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

    @Override
    public List<Transaction> getTransactions(Transaction coinbaseTxn)
    {
        // Ignore provided coinbase.
        return super.getTransactions(this.coinbase);
    }

    @Override
    public boolean hasCoinbaseTransactionBytes()
    {
        return (this.coinbaseBytes == null);
    }

    @Override
    public byte[] getCoinbaseTransactionBytes()
    {
        return this.coinbaseBytes;
    }
}