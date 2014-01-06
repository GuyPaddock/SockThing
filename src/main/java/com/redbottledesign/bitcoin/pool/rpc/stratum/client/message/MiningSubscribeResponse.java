package com.redbottledesign.bitcoin.pool.rpc.stratum.client.message;

import java.util.List;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;

import com.redbottledesign.bitcoin.rpc.stratum.MalformedStratumMessageException;
import com.redbottledesign.bitcoin.rpc.stratum.message.StratumArrayResult;
import com.redbottledesign.bitcoin.rpc.stratum.message.StratumResponseMessage;
import com.redbottledesign.bitcoin.rpc.stratum.message.StratumResult;

public class MiningSubscribeResponse
extends StratumResponseMessage
{
    public static final String RESPONSE_SUBJECT = "mining.notify";

    public MiningSubscribeResponse(long id, String subscriptionId, byte[] extraNonce1, int extraNonce2ByteLength)
    {
        super(id, createSubscriptionResult(subscriptionId, extraNonce1, extraNonce2ByteLength));
    }

    public MiningSubscribeResponse(MiningSubscribeRequest request, String subscriptionId, byte[] extraNonce1,
                                   int extraNonce2ByteLength)
    {
        super(request.getId(), createSubscriptionResult(subscriptionId, extraNonce1, extraNonce2ByteLength));
    }

    public MiningSubscribeResponse(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        super(jsonMessage);
    }

    protected static StratumArrayResult createSubscriptionResult(String subscriptionId, byte[] extraNonce1,
                                                                 int extraNonce2ByteLength)
    {
        return new StratumArrayResult(
            RESPONSE_SUBJECT,
            subscriptionId,
            Hex.encodeHexString(extraNonce1),
            extraNonce2ByteLength);
    }

    @Override
    public StratumArrayResult getResult()
    {
        return (StratumArrayResult)super.getResult();
    }

    public String getSubscriptionId()
    {
        return this.getResult().getSubjectKey();
    }

    public byte[] getExtraNonce1()
    {
        List<Object> resultData  = this.getResult().getResultData();
        String       extraNonce1 = resultData.get(0).toString();

        try
        {
            return Hex.decodeHex(extraNonce1.toCharArray());
        }

        catch (DecoderException ex)
        {
            // Should never happen
            throw new RuntimeException("Unable to decode extra nonce #1: " + ex.getMessage(), ex);
        }
    }

    public int getExtraNonce2ByteLength()
    {
        List<Object> resultData  = this.getResult().getResultData();
        int          extraNonce1 = (Integer)resultData.get(1);

        return extraNonce1;
    }

    @Override
    protected void validateParsedData(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        StratumResult       result      = super.getResult();
        StratumArrayResult  arrayResult;
        List<Object>        resultData;

        super.validateParsedData(jsonMessage);

        if (!(result instanceof StratumArrayResult))
            throw new MalformedStratumMessageException(jsonMessage, "Result in response message is not an array.");

        arrayResult = (StratumArrayResult)result;
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

        if (!(resultData.get(0) instanceof String))
        {
            throw new MalformedStratumMessageException(
                RESPONSE_SUBJECT,
                "Extra nonce #1 bytes should be provided as a String.",
                jsonMessage);
        }

        if (!(resultData.get(1) instanceof Integer))
        {
            throw new MalformedStratumMessageException(
                RESPONSE_SUBJECT,
                "Extra nonce #2 byte length should be provided as an Integer.",
                jsonMessage);
        }
    }
}
