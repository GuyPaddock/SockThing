package com.redbottledesign.bitcoin.pool.rpc.stratum.message;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.bitcoin.core.Sha256Hash;
import com.redbottledesign.bitcoin.rpc.stratum.MalformedStratumMessageException;
import com.redbottledesign.bitcoin.rpc.stratum.message.RequestMessage;

/**
 * <p>Java representation of a Stratum {@code mining.notify} request
 * message, which notifies a worker about new work / new jobs.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (gpaddock@redbottledesign.com)
 */
public class MiningNotifyRequest
extends RequestMessage
{
    /**
     * The name of this method as it appears in the request.
     */
    public static final String METHOD_NAME = "mining.notify";

    /**
     * The number of required parameters for this request.
     */
    public static final int PARAM_REQUIRED_COUNT = 9;

    /**
     * The offset of the parameter that specifies the Stratum Job ID.
     */
    private static final int PARAM_OFFSET_JOB_ID = 0;

    /**
     * The offset of the parameter that specifies the hash of the previous block.
     */
    private static final int PARAM_OFFSET_PREVIOUS_BLOCK_HASH = 1;

    /**
     * The offset of the parameter that specifies the first part of the coinbase.
     */
    private static final int PARAM_OFFSET_COINBASE_PART1 = 2;

    /**
     * The offset of the parameter that specifies the second part of the coinbase.
     */
    private static final int PARAM_OFFSET_COINBASE_PART2 = 3;

    /**
     * The offset of the parameter that specifies the list of merkle branches.
     */
    private static final int PARAM_OFFSET_MERKLE_BRANCHES = 4;

    /**
     * The offset of the parameter that specifies the block version, encoded in bytes.
     */
    private static final int PARAM_OFFSET_BLOCK_VERSION = 5;

    /**
     * The offset of the parameter that specifies the network difficulty, encoded in bytes.
     */
    private static final int PARAM_OFFSET_NETWORK_DIFFICULTY = 6;

    /**
     * The offset of the parameter that specifies the network time, encoded in bytes.
     */
    private static final int PARAM_OFFSET_NETWORK_TIME = 7;

    /**
     * The offset of the parameter that specifies whether or not to clean jobs.
     */
    private static final int PARAM_OFFSET_CLEAN_JOBS = 8;

    /**
     * <p>Constructor for {@link MiningNotifyRequest} that creates a new
     * instance with the specified mining parameters.</p>
     *
     * <p>The request is automatically assigned a unique ID.</p>
     *
     * @param   jobId
     *          The Stratum job ID.
     *
     * @param   previousBlockHash
     *          The hash of the previous block.
     *
     * @param   coinbasePart1
     *          The first part of the coinbase.
     *
     * @param   coinbasePart2
     *          The second part of the coinbase.
     *
     * @param   merkleBranches
     *          The list of merkle branches.
     *
     * @param   blockVersion
     *          The block version, encoded in bytes.
     *
     * @param   networkDifficultyBits
     *          The network difficulty, encoded in bytes.
     *
     * @param   networkTime
     *          The network time, encoded in bytes.
     *
     * @param   cleanJobs
     *          Whether or not to clean jobs.
     */
    public MiningNotifyRequest(String jobId, Sha256Hash previousBlockHash, byte[] coinbasePart1,
                               byte[] coinbasePart2, List<String> merkleBranches, byte[] blockVersion,
                               byte[] networkDifficultyBits, long networkTime, boolean cleanJobs)
    {
        this(
            RequestMessage.getNextRequestId(),
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

    /**
     * <p>Constructor for {@link MiningNotifyRequest} that creates a new
     * instance with the specified message ID and mining parameters.</p>
     *
     * <p>The message is automatically assigned a unique ID.</p>
     *
     * @param   id
     *          The message ID.
     *
     * @param   jobId
     *          The Stratum job ID.
     *
     * @param   previousBlockHash
     *          The hash of the previous block.
     *
     * @param   coinbasePart1
     *          The first part of the coinbase.
     *
     * @param   coinbasePart2
     *          The second part of the coinbase.
     *
     * @param   merkleBranches
     *          The list of merkle branches.
     *
     * @param   blockVersion
     *          The block version.
     *
     * @param   networkDifficultyBits
     *          The network difficulty.
     *
     * @param   networkTime
     *          The network time.
     *
     * @param   cleanJobs
     *          Whether or not to clean jobs.
     */
    public MiningNotifyRequest(String id, String jobId, Sha256Hash previousBlockHash, byte[] coinbasePart1,
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

    /**
     * Constructor for {@link MiningNotifyRequest} that creates a new
     * instance from information in the provided JSON message.
     *
     * @param   jsonMessage
     *          The message in JSON format.
     *
     * @throws  MalformedStratumMessageException
     *          If the provided JSON message object is not a properly-formed
     *          Stratum message or cannot be understood.
     */
    public MiningNotifyRequest(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        super(jsonMessage);
    }

    /**
     * Gets the Stratum job ID.
     *
     * @return  The job ID.
     */
    public String getJobId()
    {
        return this.getParams().get(PARAM_OFFSET_JOB_ID).toString();
    }

    /**
     * Gets the hash of the previous block.
     *
     * @return  The previous block hash.
     */
    public Sha256Hash getPreviousBlockHash()
    {
        return new Sha256Hash(this.getParams().get(PARAM_OFFSET_PREVIOUS_BLOCK_HASH).toString());
    }

    /**
     * Gets the first part of the coinbase transaction.
     *
     * @return  The first part of the coinbase.
     */
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

    /**
     * Gets the second part of the coinbase transaction.
     *
     * @return  The second part of the coinbase.
     */
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

    /**
     * Gets the list of merkle branches.
     *
     * @return  The merkle branch list.
     */
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

    /**
     * Gets the block version, encoded in bytes.
     *
     * @return  The block version.
     */
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

    /**
     * Gets the network difficulty, encoded in bytes.
     *
     * @return  The network difficulty.
     */
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

    /**
     * Gets the network time, encoded in bytes.
     *
     * @return  The network time.
     */
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

    /**
     * Gets whether or not the miner should clean all outstanding jobs before
     * switching over to this job.
     *
     * @return  {@code true} if the miner should discard any outstanding work;
     *          {@code false}, otherwise.
     */
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

    /**
     * {@inheritDoc}
     */
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
        if (params.size() < PARAM_REQUIRED_COUNT)
        {
            throw new MalformedStratumMessageException(
                METHOD_NAME,
                PARAM_REQUIRED_COUNT + " parameters are required.",
                jsonMessage);
        }

        /* The rest of validation will happen when data is requested by the getters.
         * To repeat that same code here would be redundant.
         */
    }
}