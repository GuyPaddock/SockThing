package com.redbottledesign.bitcoin.pool.rpc.stratum.message;

import java.util.List;

import org.json.JSONObject;

import com.redbottledesign.bitcoin.rpc.stratum.MalformedStratumMessageException;
import com.redbottledesign.bitcoin.rpc.stratum.message.RequestMessage;

/**
 * <p>Java representation of a Stratum {@code mining.set_difficulty} request
 * message.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (gpaddock@redbottledesign.com)
 */
public class MiningSetDifficultyRequest
extends RequestMessage
{
    /**
     * The name of this method as it appears in the request.
     */
    public static final String METHOD_NAME = "mining.set_difficulty";

    /**
     * The number of required parameters for this request.
     */
    public static final int PARAM_REQUIRED_COUNT = 1;

    /**
     * The offset of the parameter that specifies the worker user name.
     */
    private static final int PARAM_OFFSET_DIFFICULTY = 0;

    /**
     * <p>Constructor for {@link MiningSetDifficultyRequest} that creates a new
     * instance with the specified difficulty.</p>
     *
     * <p>The request is automatically assigned a unique ID.</p>
     *
     * @param   difficulty
     *          The new difficulty.
     */
    public MiningSetDifficultyRequest(int difficulty)
    {
        this(RequestMessage.getNextRequestId(), difficulty);
    }

    /**
     * Constructor for {@link MiningSetDifficultyRequest} that creates a new
     * instance with the specified message ID, username, and password.
     *
     * @param   id
     *          The message ID.
     *
     * @param   difficulty
     *          The new difficulty.
     */
    public MiningSetDifficultyRequest(String id, long difficulty)
    {
        super(id, METHOD_NAME, difficulty);
    }

    /**
     * Constructor for {@link MiningSetDifficultyRequest} that creates a new
     * instance from information in the provided JSON message.
     *
     * @param   jsonMessage
     *          The message in JSON format.
     */
    public MiningSetDifficultyRequest(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        super(jsonMessage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateParsedData(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        List<Object>    params          = this.getParams();
        Object          difficultyParam;
        Integer         difficulty;

        super.validateParsedData(jsonMessage);

        if (params.size() < PARAM_REQUIRED_COUNT)
        {
            throw new MalformedStratumMessageException(
                METHOD_NAME,
                "difficulty is required",
                jsonMessage);
        }

        difficultyParam = params.get(PARAM_OFFSET_DIFFICULTY);

        if (!(difficultyParam instanceof Integer))
        {
            throw new MalformedStratumMessageException(
                METHOD_NAME,
                "difficulty must be an integer",
                jsonMessage);
        }

        difficulty = (Integer)difficultyParam;

        if (difficulty < 1)
        {
            throw new MalformedStratumMessageException(
                METHOD_NAME,
                "difficulty must be at least 1.",
                jsonMessage);
        }

        if (difficulty > 65536)
        {
            throw new MalformedStratumMessageException(
                METHOD_NAME,
                "difficulty must be less than 65536.",
                jsonMessage);
        }
    }

    /**
     * Gets the new difficulty.
     *
     * @return  The difficulty.
     */
    public int getDifficulty()
    {
        return (Integer)this.getParams().get(PARAM_OFFSET_DIFFICULTY);
    }
}