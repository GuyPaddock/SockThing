package com.redbottledesign.bitcoin.pool.rpc.stratum.message;

import java.util.List;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;

import com.redbottledesign.bitcoin.rpc.stratum.MalformedStratumMessageException;
import com.redbottledesign.bitcoin.rpc.stratum.message.ArrayResult;
import com.redbottledesign.bitcoin.rpc.stratum.message.ResponseMessage;
import com.redbottledesign.bitcoin.rpc.stratum.message.Result;

/**
 * <p>Java representation of a Stratum {@code mining.notify} response
 * message.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (gpaddock@redbottledesign.com)
 */
public class MiningSubscribeResponse
extends ResponseMessage
{
    /**
     * The offset of the result value that specifies the bytes in extra nonce #1.
     */
    private static final int RESULT_OFFSET_EXTRA_NONCE_1 = 0;

    /**
     * The offset of the result value that specifies size of extra nonce #2.
     */
    private static final int RESULT_OFFSET_EXTRA_NONCE_2_BYTE_LENGTH = 1;

    /**
     * The unique subject for this type of response.
     */
    public static final String RESPONSE_SUBJECT = "mining.notify";

    /**
     * Constructor for {@link MiningSubscribeResponse} that creates a new
     * instance with the specified request ID, subscription ID, extra
     * nonce #1, and extra nonce #2 byte length.
     *
     * @param   id
     *          The ID of the request to which this response corresponds.
     *
     * @param   subscriptionId
     *          The unique subscription ID that the worker can use to refer to
     *          this subscription later.
     *
     * @param   extraNonce1
     *          The bytes the worker must use for extra nonce #1.
     *
     * @param   extraNonce2ByteLength
     *          The number of bytes that the worker can use to generate extra
     *          nonce #2.
     */
    public MiningSubscribeResponse(String id, String subscriptionId, byte[] extraNonce1, int extraNonce2ByteLength)
    {
        super(id, createSubscriptionResult(subscriptionId, extraNonce1, extraNonce2ByteLength));
    }

    /**
     * Constructor for {@link MiningSubscribeResponse} that creates a new
     * instance with the specified request, subscription ID, extra nonce #1,
     * and extra nonce #2 byte length.
     *
     * @param   request
     *          The request to which this response corresponds.
     *
     * @param   subscriptionId
     *          The unique subscription ID that the worker can use to refer to
     *          this subscription later.
     *
     * @param   extraNonce1
     *          The bytes the worker must use for extra nonce #1.
     *
     * @param   extraNonce2ByteLength
     *          The number of bytes that the worker can use to generate extra
     *          nonce #2.
     */
    public MiningSubscribeResponse(MiningSubscribeRequest request, String subscriptionId, byte[] extraNonce1,
                                   int extraNonce2ByteLength)
    {
        this(request.getId(), subscriptionId, extraNonce1, extraNonce2ByteLength);
    }

    /**
     * Constructor for {@link MiningSubscribeResponse} that creates a new
     * instance from information in the provided JSON message.
     *
     * @param   jsonMessage
     *          The message in JSON format.
     *
     * @throws  MalformedStratumMessageException
     *          If the provided JSON message object is not a properly-formed
     *          Stratum message or cannot be understood.
     */
    public MiningSubscribeResponse(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        super(jsonMessage);
    }

    /**
     * Creates a new stratum result object from the given subscription id,
     * nonce #1 bytes, and nonce #2 size.
     *
     * @param   subscriptionId
     *          The unique subscription ID that the worker can use to refer to
     *          this subscription later.
     *
     * @param   extraNonce1
     *          The bytes the worker must use for extra nonce #1.
     *
     * @param   extraNonce2ByteLength
     *          The number of bytes that the worker must fill to generate extra
     *          nonce #2.
     *
     * @return  A stratum result object that wraps the given values.
     */
    protected static ArrayResult createSubscriptionResult(String subscriptionId, byte[] extraNonce1,
                                                                 int extraNonce2ByteLength)
    {
        return new ArrayResult(
            RESPONSE_SUBJECT,
            subscriptionId,
            Hex.encodeHexString(extraNonce1),
            extraNonce2ByteLength);
    }

    /**
     * Gets the result of this response as a {@link ArrayResult}.
     *
     * @return  The result of this response, as a {@link ArrayResult}.
     */
    @Override
    public ArrayResult getResult()
    {
        return (ArrayResult)super.getResult();
    }

    /**
     * Gets the unique subscription ID that the worker can use to refer to this
     * subscription later.
     *
     * @return  The result of this response, as a {@link ArrayResult}.
     */
    public String getSubscriptionId()
    {
        return this.getResult().getSubjectKey();
    }

    /**
     * Gets the bytes the worker must use for extra nonce #1.
     *
     * @return  The bytes in extra nonce #1.
     */
    public byte[] getExtraNonce1()
    {
        List<Object> resultData  = this.getResult().getResultData();
        String       extraNonce1 = resultData.get(RESULT_OFFSET_EXTRA_NONCE_1).toString();

        try
        {
            return Hex.decodeHex(extraNonce1.toCharArray());
        }

        catch (DecoderException ex)
        {
            throw new RuntimeException("Unable to decode extra nonce #1: " + ex.getMessage(), ex);
        }
    }

    /**
     * Gets the number of bytes that the worker must fill to generate extra
     * nonce #2.
     *
     * @return  The number of bytes in extra nonce #2.
     */
    public int getExtraNonce2ByteLength()
    {
        List<Object> resultData  = this.getResult().getResultData();
        int          extraNonce1 = (Integer)resultData.get(RESULT_OFFSET_EXTRA_NONCE_2_BYTE_LENGTH);

        return extraNonce1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateParsedData(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        Result          result      = super.getResult();
        ArrayResult     arrayResult;
        List<Object>    resultData;

        super.validateParsedData(jsonMessage);

        if (!(result instanceof ArrayResult))
            throw new MalformedStratumMessageException(jsonMessage, "Result in response message is not an array.");

        arrayResult = (ArrayResult)result;
        resultData  = arrayResult.getResultData();

        if (!arrayResult.getSubject().equals(RESPONSE_SUBJECT))
        {
            throw new MalformedStratumMessageException(
                jsonMessage,
                String.format("Expected reply subject must be '%s'.", RESPONSE_SUBJECT));
        }

        /* There are two pieces of data expected after the subject tuple: the
         * raw extra nonce #1 bytes, and the size (in bytes) of extra nonce #2.
         *
         * We don't put an upper bound on the result size, because some pools
         * (even SockThing) send extra data we don't care about.
         */
        if (resultData.size() < 2)
        {
            throw new MalformedStratumMessageException(
                RESPONSE_SUBJECT,
                "Extra nonce #1 bytes and extra nonce #2 size were both expected.",
                jsonMessage);
        }

        if (!(resultData.get(RESULT_OFFSET_EXTRA_NONCE_1) instanceof String))
        {
            throw new MalformedStratumMessageException(
                RESPONSE_SUBJECT,
                "Extra nonce #1 bytes should be provided as a String.",
                jsonMessage);
        }

        if (!(resultData.get(RESULT_OFFSET_EXTRA_NONCE_2_BYTE_LENGTH) instanceof Integer))
        {
            throw new MalformedStratumMessageException(
                RESPONSE_SUBJECT,
                "Extra nonce #2 byte length should be provided as an Integer.",
                jsonMessage);
        }
    }
}
