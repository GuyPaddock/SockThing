package com.redbottledesign.bitcoin.rpc.stratum.message;

import org.json.JSONArray;

import com.redbottledesign.bitcoin.rpc.stratum.MalformedStratumMessageException;

/**
 * <p>Factory for transforming a Stratum result from a JSON object into the
 * appropriate {@link StratumResult} object.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 */
public class StratumResultFactory
{
    /**
     * The singleton instance of this factory.
     */
    private static StratumResultFactory INSTANCE = new StratumResultFactory();

    /**
     * Gets an instance of this factory.
     *
     * @return  The current factory instance.
     */
    public static StratumResultFactory getInstance()
    {
        return INSTANCE;
    }

    /**
     * Instantiates the appropriate {@link StratumResult} object to parse the
     * provided JSON object and wrap its value.
     *
     * @param   jsonObject
     *          The JSON object to parse.
     *
     * @return  The appropriate result object.
     *
     * @throws  MalformedStratumMessageException
     */
    public StratumResult createResult(Object jsonObject) throws MalformedStratumMessageException
    {
        StratumResult result;

        // Single array response
        if (jsonObject instanceof JSONArray)
            result = new StratumArrayResult((JSONArray)jsonObject);

        // Single-value responses (boolean, string, etc)
        else
            result = new StratumValueResult<Object>(jsonObject);

        return result;
    }
}