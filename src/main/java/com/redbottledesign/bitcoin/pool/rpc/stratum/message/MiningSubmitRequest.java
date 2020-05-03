package com.redbottledesign.bitcoin.pool.rpc.stratum.message;

import java.util.List;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;

import com.redbottledesign.bitcoin.rpc.stratum.MalformedStratumMessageException;
import com.redbottledesign.bitcoin.rpc.stratum.message.RequestMessage;

/**
 * <p>Java representation of a Stratum {@code mining.submit} request
 * message, which is used by a worker to submit work.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (gpaddock@redbottledesign.com)
 */
public class MiningSubmitRequest
extends RequestMessage
{
    /**
     * The name of this method as it appears in the request.
     */
    public static final String METHOD_NAME = "mining.submit";

    /**
     * The number of required parameters for this request.
     */
    public static final int PARAM_REQUIRED_COUNT = 5;

    /**
     * The offset of the parameter that specifies the worker name.
     */
    private static final int PARAM_OFFSET_WORKER_NAME = 0;

    /**
     * The offset of the parameter that specifies the job ID.
     */
    private static final int PARAM_OFFSET_JOB_ID = 1;

    /**
     * The offset of the parameter that specifies extra nonce #2.
     */
    private static final int PARAM_OFFSET_EXTRA_NONCE_2 = 2;

    /**
     * The offset of the parameter that specifies the network time.
     */
    private static final int PARAM_OFFSET_NETWORK_TIME = 3;

    /**
     * The offset of the parameter that specifies the nonce.
     */
    private static final int PARAM_OFFSET_NONCE = 4;

    /**
     * <p>Constructor for {@link MiningSubmitRequest} that creates a new
     * instance with the specified worker, job, extra nonce, network time,
     * and nonce.</p>
     *
     * <p>The request is automatically assigned a unique ID.</p>
     *
     * @param   workerName
     *          The name of the worker.
     *
     * @param   jobId
     *          The unique identifier for the job.
     *
     * @param   extraNonce2
     *          The bytes of the extra nonce.
     *
     * @param   networkTime
     *          The network time off of which the worker is operating.
     *
     * @param   nonce
     *          The bytes of the nonce.
     */
    public MiningSubmitRequest(String workerName, String jobId, byte[] extraNonce2, long networkTime, byte[] nonce)
    {
        this(RequestMessage.getNextRequestId(), workerName, jobId, extraNonce2, networkTime, nonce);
    }

    /**
     * Constructor for {@link MiningSubmitRequest} that creates a new
     * instance with the specified message ID, worker, job, extra nonce,
     * network time, and nonce.
     *
     * @param   id
     *          The message ID.
     *
     * @param   workerName
     *          The name of the worker.
     *
     * @param   jobId
     *          The unique identifier for the job.
     *
     * @param   extraNonce2
     *          The bytes of the extra nonce.
     *
     * @param   networkTime
     *          The network time off of which the worker is operating.
     *
     * @param   nonce
     *          The bytes of the nonce.
     */
    public MiningSubmitRequest(String id, String workerName, String jobId, byte[] extraNonce2, long networkTime,
                               byte[] nonce)
    {
        super(id, METHOD_NAME, workerName, jobId, extraNonce2, networkTime, nonce);
    }

    /**
     * Constructor for {@link MiningSubmitRequest} that creates a new
     * instance from information in the provided JSON message.
     *
     * @param   jsonMessage
     *          The message in JSON format.
     *
     * @throws  MalformedStratumMessageException
     *          If the provided JSON message object is not a properly-formed
     *          Stratum message or cannot be understood.
     */
    public MiningSubmitRequest(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        super(jsonMessage);
    }

    /**
     * Gets the name of the worker.
     *
     * @return  The name of the worker.
     */
    public String getWorkerName()
    {
        return this.getParams().get(PARAM_OFFSET_WORKER_NAME).toString();
    }

    /**
     * Gets the unique job ID.
     *
     * @return  The job ID.
     */
    public String getJobId()
    {
        return this.getParams().get(PARAM_OFFSET_JOB_ID).toString();
    }

    /**
     * Gets extra nonce #2, encoded in bytes.
     *
     * @return  The second extra nonce.
     */
    public byte[] getExtraNonce2()
    {
        try
        {
            return Hex.decodeHex(this.getParams().get(PARAM_OFFSET_EXTRA_NONCE_2).toString().toCharArray());
        }

        catch (DecoderException ex)
        {
            throw new RuntimeException("Unable to decode extra nonce #2: " + ex.getMessage(), ex);
        }
    }

    /**
     * Gets the network time.
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
     * Gets the nonce, encoded in bytes.
     *
     * @return  The bytes of the nonce.
     */
    public byte[] getNonce()
    {
        try
        {
            return Hex.decodeHex(this.getParams().get(PARAM_OFFSET_NONCE).toString().toCharArray());
        }

        catch (DecoderException ex)
        {
            throw new RuntimeException("Unable to decode nonce: " + ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateParsedData(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        List<Object> params = this.getParams();

        super.validateParsedData(jsonMessage);

        if (params.size() < PARAM_REQUIRED_COUNT)
        {
            throw new MalformedStratumMessageException(
                METHOD_NAME,
                PARAM_REQUIRED_COUNT + " parameters are required",
                jsonMessage);
        }

        /* The rest of validation will happen when data is requested by the getters.
         * To repeat that same code here would be redundant.
         */
    }
}
