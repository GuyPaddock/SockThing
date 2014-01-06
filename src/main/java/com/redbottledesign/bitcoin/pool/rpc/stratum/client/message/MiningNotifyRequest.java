package com.redbottledesign.bitcoin.pool.rpc.stratum.client.message;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.bitcoin.core.Sha256Hash;
import com.redbottledesign.bitcoin.rpc.stratum.MalformedStratumMessageException;
import com.redbottledesign.bitcoin.rpc.stratum.message.StratumRequestMessage;

public class MiningNotifyRequest
extends StratumRequestMessage
{

    public static final String METHOD_NAME = "mining.notify";

    private static final int PARAM_OFFSET_JOB_ID                = 0;
    private static final int PARAM_OFFSET_PREVIOUS_BLOCK_HASH   = 1;
    private static final int PARAM_OFFSET_COINBASE_PART1        = 2;
    private static final int PARAM_OFFSET_COINBASE_PART2        = 3;
    private static final int PARAM_OFFSET_MERKLE_BRANCHES       = 4;
    private static final int PARAM_OFFSET_BLOCK_VERSION         = 5;
    private static final int PARAM_OFFSET_NETWORK_DIFFICULTY    = 6;
    private static final int PARAM_OFFSET_NETWORK_TIME          = 7;
    private static final int PARAM_OFFSET_CLEAN_JOBS            = 8;

    public MiningNotifyRequest(String jobId, Sha256Hash previousBlockHash, byte[] coinbasePart1,
                               byte[] coinbasePart2, List<String> merkleBranches, byte[] blockVersion,
                               byte[] networkDifficultyBits, long networkTime, boolean cleanJobs)
    {
        this(
            StratumRequestMessage.getNextRequestId(),
            jobId,
            previousBlockHash,
            coinbasePart1,
            coinbasePart2,
            merkleBranches,
            blockVersion,
            networkDifficultyBits,
            networkTime,
            cleanJobs);
    }

    public MiningNotifyRequest(long id, String jobId, Sha256Hash previousBlockHash, byte[] coinbasePart1,
                               byte[] coinbasePart2, List<String> merkleBranches, byte[] blockVersion,
                               byte[] networkDifficultyBits, long networkTime, boolean cleanJobs)
    {
        super(
            id,
            METHOD_NAME,
            jobId,
            previousBlockHash.toString(),
            Hex.encodeHexString(coinbasePart1),
            Hex.encodeHexString(coinbasePart2),
            merkleBranches.toArray(),
            Hex.encodeHexString(blockVersion),
            Hex.encodeHexString(networkDifficultyBits),
            Long.toString(networkTime),
            cleanJobs);
    }

    public MiningNotifyRequest(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        super(jsonMessage);
    }

    public String getJobId()
    {
        return this.getParams().get(PARAM_OFFSET_JOB_ID).toString();
    }

    public Sha256Hash getPreviousBlockHash()
    {
        return new Sha256Hash(this.getParams().get(PARAM_OFFSET_PREVIOUS_BLOCK_HASH).toString());
    }

    public byte[] getCoinbasePart1()
    {
        try
        {
            return Hex.decodeHex(this.getParams().get(PARAM_OFFSET_COINBASE_PART1).toString().toCharArray());
        }

        catch (DecoderException ex)
        {
            throw new RuntimeException("Unable to decode coinbase part #1: " + ex.getMessage(), ex);
        }
    }

    public byte[] getCoinbasePart2()
    {
        try
        {
            return Hex.decodeHex(this.getParams().get(PARAM_OFFSET_COINBASE_PART2).toString().toCharArray());
        }

        catch (DecoderException ex)
        {
            throw new RuntimeException("Unable to decode coinbase part #2: " + ex.getMessage(), ex);
        }
    }

    public List<String> getMerkleBranches()
    {
        JSONArray       jsonMerkleBranches  = (JSONArray)this.getParams().get(PARAM_OFFSET_MERKLE_BRANCHES);
        List<String>    results             = new LinkedList<String>();

        for (int branchIndex = 0; branchIndex < jsonMerkleBranches.length(); ++branchIndex)
        {
            try
            {
                results.add(jsonMerkleBranches.getString(branchIndex));
            }

            catch (JSONException ex)
            {
                throw new RuntimeException("Unable to decode merkle branch: " + ex.getMessage(), ex);
            }
        }

        return results;
    }

    public byte[] getBlockVersion()
    {
        try
        {
            return Hex.decodeHex(this.getParams().get(PARAM_OFFSET_BLOCK_VERSION).toString().toCharArray());
        }

        catch (DecoderException ex)
        {
            // Should never happen after we validate this in validateParsedData()
            throw new RuntimeException("Unable to decode block version: " + ex.getMessage(), ex);
        }
    }

    public byte[] getNetworkDifficultyBits()
    {
        try
        {
            return Hex.decodeHex(this.getParams().get(PARAM_OFFSET_NETWORK_DIFFICULTY).toString().toCharArray());
        }

        catch (DecoderException ex)
        {
            throw new RuntimeException("Unable to decode network difficulty: " + ex.getMessage(), ex);
        }
    }

    public long getNetworkTime()
    {
        String  networkTimeString = this.getParams().get(PARAM_OFFSET_NETWORK_TIME).toString();
        long    networkTime;

        try
        {
            networkTime = Long.decode("0x" + networkTimeString);
        }

        catch (NumberFormatException ex)
        {
            throw new RuntimeException("Unable to decode network time: " + ex.getMessage(), ex);
        }

        return networkTime;
    }

    public boolean shouldCleanJobs()
    {
        Object cleanJobs = this.getParams().get(PARAM_OFFSET_CLEAN_JOBS);

        if (!(cleanJobs instanceof Boolean))
        {
            throw new RuntimeException(
                String.format(
                    "Clean jobs should be a boolean, but an object of type '%s' was encountered instead.",
                    cleanJobs.getClass().getName()));
        }

        return (boolean)cleanJobs;
    }

    @Override
    protected void validateParsedData(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        List<Object>    params;

        super.validateParsedData(jsonMessage);

        params = this.getParams();

        /*
         * There are nine pieces of data expected in a notify message:
         *
         * - params[0]   = Job ID. This is included when miners submit a results
         *                 so work can be matched with proper transactions.
         *
         * - params[1]   = Hash of previous block. Used to build the header.
         *
         * - params[2]   = Coinbase (part 1). The miner inserts ExtraNonce1 and
         *                 ExtraNonce2 after this section of the coinbase.
         *
         * - params[3]   = Coinbase (part 2). The miner appends this after the
         *                 first part of the coinbase and the two ExtraNonce
         *                 values.
         *
         * - params[4][] = List of merkle branches. The coinbase transaction is
         *                 hashed against the merkle branches to build the final
         *                 merkle root.
         *
         * - params[5]   = Bitcoin block version, used in the block header.
         * - params[6]   = nBit, the encoded network difficulty. Used in the
         *                 block header.
         *
         * - params[7]   = nTime, the current time. nTime rolling should be
         *                 supported, but should not increase faster than
         *                 actual time.
         *
         * - params[8]   = Clean Jobs. If true, miners should abort their
         *                 current work and immediately use the new job. If
         *                 false, they can still use the current job, but
         *                 should move to the new one after exhausting the
         *                 current nonce range.
         */
        if (params.size() < 9)
        {
            throw new MalformedStratumMessageException(
                METHOD_NAME,
                "9 parameters are required.",
                jsonMessage);
        }

        /* The rest of validation will happen when data is requested by the getters.
         * To repeat that same code here would be redundant.
         */
    }
}