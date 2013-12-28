package com.redbottledesign.bitcoin.pool.rpc.bitcoin;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.util.HexUtil;
import com.google.bitcoin.core.Coinbase;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.ProtocolException;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;

public class BitcoinDaemonBlockTemplate
implements BlockTemplate
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BitcoinDaemonBlockTemplate.class);

    protected NetworkParameters networkParams;
    protected JSONObject jsonBlockTemplate;

    public BitcoinDaemonBlockTemplate(NetworkParameters networkParams, JSONObject jsonBlockTemplate)
    {
        this.networkParams      = networkParams;
        this.jsonBlockTemplate  = jsonBlockTemplate;
    }

    @Override
    public long getHeight()
    throws MalformedBlockTemplateException
    {
        try
        {
            long result = this.jsonBlockTemplate.getLong("height");

            if (result <= 0)
                throw new MalformedBlockTemplateException("Unexpected block height: " + result);

            return result;
        }

        catch (JSONException ex)
        {
            throw new MalformedBlockTemplateException(
                "Block height is missing or unable to be parsed: " + ex.getMessage(),
                ex);
        }
    }

    @Override
    public double getDifficulty()
    {
        return HexUtil.difficultyFromHex(this.getDifficultyBits());
    }

    @Override
    public String getDifficultyBits()
    throws MalformedBlockTemplateException
    {
        try
        {
            return this.jsonBlockTemplate.getString("bits");
        }

        catch (JSONException ex)
        {
            throw new MalformedBlockTemplateException(
                "Difficulty bits are missing or unable to be parsed: " + ex.getMessage(),
                ex);
        }
    }

    @Override
    public String getPreviousBlockHash()
    throws MalformedBlockTemplateException
    {
        try
        {
            return this.jsonBlockTemplate.getString("previousblockhash");
        }

        catch (JSONException ex)
        {
            throw new MalformedBlockTemplateException(
                "Previous block hash is missing or unable to be parsed: " + ex.getMessage(),
                ex);
        }
    }

    @Override
    public int getCurrentTime()
    throws MalformedBlockTemplateException
    {
        try
        {
            return this.jsonBlockTemplate.getInt("curtime");
        }

        catch (JSONException ex)
        {
            throw new MalformedBlockTemplateException(
                "Current time is missing or unable to be parsed: " + ex.getMessage(),
                ex);
        }
    }

    @Override
    public String getTarget()
    throws MalformedBlockTemplateException
    {
        try
        {
            return this.jsonBlockTemplate.getString("target");
        }

        catch (JSONException ex)
        {
            throw new MalformedBlockTemplateException(
                "Target is missing or unable to be parsed: " + ex.getMessage(),
                ex);
        }
    }

    @Override
    public boolean hasBlockReward()
    {
        return true;
    }

    @Override
    public BigInteger getReward()
    throws MalformedBlockTemplateException
    {
        try
        {
            return new BigInteger(this.jsonBlockTemplate.get("coinbasevalue").toString());
        }

        catch (JSONException ex)
        {
            throw new MalformedBlockTemplateException(
                "Block reward is missing or unable to be parsed: " + ex.getMessage(),
                ex);
        }
    }

    @Override
    public BigInteger getTotalFees()
    throws MalformedBlockTemplateException
    {
        try
        {
            BigInteger totalFees = BigInteger.ZERO;

            JSONArray transactions = this.jsonBlockTemplate.getJSONArray("transactions");

            for (int i = 0; i < transactions.length(); i++)
            {
                JSONObject tx = transactions.getJSONObject(i);

                try
                {
                    BigInteger feeInSatoshis = new BigInteger(tx.get("fee").toString());

                    totalFees = totalFees.add(feeInSatoshis);
                }

                catch (JSONException ex)
                {
                    throw new MalformedBlockTemplateException(
                        String.format(
                            "Fee information for transaction %d is missing or unable to be parsed: %s",
                            i,
                            ex.getMessage()),
                        ex);
                }
            }

            return totalFees;
        }

        catch (JSONException ex)
        {
            throw new MalformedBlockTemplateException(
                "Transaction information is missing or unable to be parsed: " + ex.getMessage(),
                ex);
        }
    }

    @Override
    public List<Transaction> getTransactions()
    {
        return this.getTransactions(null);
    }

    @Override
    public List<Transaction> getTransactions(Coinbase coinbase)
    {
        try
        {
            LinkedList<Transaction> transactions        = new LinkedList<Transaction>();
            JSONArray               jsonTransactions    = this.jsonBlockTemplate.getJSONArray("transactions");

            if (coinbase != null)
                transactions.add(coinbase.genTx());

            for (int i = 0; i < jsonTransactions.length(); i++)
            {
                JSONObject  jsonTransaction      = jsonTransactions.getJSONObject(i);
                char[]      jsonTransactionData  = jsonTransaction.getString("data").toCharArray();

                try
                {
                    Transaction transaction = new Transaction(this.networkParams, Hex.decodeHex(jsonTransactionData));

                    transactions.add(transaction);

                    if (LOGGER.isDebugEnabled() && jsonTransaction.has("hash"))
                    {
                        Sha256Hash jsonHash =
                            new Sha256Hash(
                                HexUtil.swapEndianHexString(
                                    jsonTransaction.getString("hash")));

                        LOGGER.debug(
                            String.format(
                                "TX hash from JSON: %s, TX hash from decoding: %s",
                                jsonHash.toString(),
                                transaction.getHash().toString()));
                    }
                }

                catch (ProtocolException e)
                {
                    throw new RuntimeException(e);
                }
            }

            return transactions;
        }

        catch (JSONException | DecoderException ex)
        {
            throw new MalformedBlockTemplateException(
                "Transaction information is missing or unable to be parsed: " + ex.getMessage(),
                ex);
        }
    }
}