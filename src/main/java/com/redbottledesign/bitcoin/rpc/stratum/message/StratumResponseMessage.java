package com.redbottledesign.bitcoin.rpc.stratum.message;

import org.json.JSONException;
import org.json.JSONObject;

import com.redbottledesign.bitcoin.rpc.stratum.MalformedStratumMessageException;

/**
 * <p>Java representation of a Stratum response message.</p>
 *
 * <p>Response messages must include the following:</p>
 *
 * <ul>
 *  <li>an {@code id} field, which cannot be {@code null} and must match the
 *  identifier that was specified in the request.</li>
 *
 *  <li>a {@code result} field, which can be either an array, a single value,
 *      or must be {@code null} if the request could not be successfully
 *      processed.</li>
 *
 *  <li>an {@code error} field, which must be {@code null} if the request was
 *      processed successfully, or must contain the last request error message
 *      if the request could not be successfully processed.</li>
 * </ul>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (gpaddock@redbottledesign.com)
 */
public class StratumResponseMessage<T>
extends StratumMessage
{
    /**
     * Constant for the name of the {@code method} field in the JSON object for
     * this message.
     */
    protected static final String JSON_STRATUM_KEY_RESULT = "result";

    /**
     * Constant for the name of the {@code error} field in the JSON object for
     * this message.
     */
    protected static final String JSON_STRATUM_KEY_ERROR = "error";

    /**
     * The result of the method call.
     */
    private StratumResult result;

    /**
     * An error message describing why the last request could not be completed,
     * if the request failed.
     */
    private String error;

    /**
     * Constructor for {@link StratumResponseMessage} that initializes a new
     * instance from information in the included JSON message.
     *
     * @param   jsonMessage
     *          The JSON message object.
     *
     * @throws  MalformedStratumMessageException
     *          If the provided JSON message object is not a properly-formed
     *          Stratum message or cannot be understood.
     */
    public StratumResponseMessage(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        super(jsonMessage);
    }

    /**
     * Constructor for {@link StratumResponseMessage} that initializes a new
     * instance having the specified numeric ID and result.
     *
     * @param   id
     *          The unique, numeric identifier for the message. This may be
     *          {@code null}.
     *
     * @param   result
     *          The result of the method call.
     */
    public StratumResponseMessage(long id, StratumResult result)
    {
        super(id);

        this.setResult(result);
    }

    /**
     * Gets the result of the method call.
     *
     * @return  The result of the method call.
     */
    public StratumResult getResult()
    {
        return this.result;
    }

    /**
     * Gets an error message describing why the last request could not be
     * completed, if the request failed.
     *
     * @return  The last error message.
     */
    public String getError()
    {
        return this.error;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJson()
    {
        JSONObject object = super.toJson();

        try
        {
            object.put(JSON_STRATUM_KEY_RESULT, this.getResult().toJson());
        }

        catch (JSONException ex)
        {
            // Should not happen
            throw new RuntimeException("Unexpected exception while contructing JSON object: " + ex.getMessage(), ex);
        }

        return object;
    }

    /**
     * Sets the result of the method call.
     *
     * @param   result
     *          The new result of the method call.
     */
    protected void setResult(StratumResult result)
    {
        this.result = result;
    }

    /**
     * Sets an error message describing why the last request could not be
     * completed, if the request failed.
     *
     * @param   error
     *          The error message.
     */
    protected void setError(String error)
    {
        this.error = error;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void parseMessage(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        super.parseMessage(jsonMessage);

        this.parseResult(jsonMessage);
        this.parseError(jsonMessage);
    }

    /**
     * Parses-out the {@code result} field from the message.
     *
     * @param   jsonMessage
     *          The message to parse.
     *
     * @throws  MalformedStratumMessageException
     *          If the provided JSON message object is not a properly-formed
     *          Stratum message or cannot be understood.
     */
    protected void parseResult(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        StratumResult result;

        if (!jsonMessage.has(JSON_STRATUM_KEY_RESULT))
        {
            throw new MalformedStratumMessageException(
                jsonMessage, String.format("missing '%s'", JSON_STRATUM_KEY_RESULT));
        }

        try
        {
            result = StratumResultFactory.getInstance().createResult(jsonMessage.get(JSON_STRATUM_KEY_RESULT));
        }

        catch (JSONException ex)
        {
            throw new MalformedStratumMessageException(jsonMessage, ex);
        }

        this.setResult(result);
    }

    /**
     * Parses-out the {@code error} field from the message.
     *
     * @param   jsonMessage
     *          The message to parse.
     *
     * @throws  MalformedStratumMessageException
     *          If the provided JSON message object is not a properly-formed
     *          Stratum message or cannot be understood.
     */
    protected void parseError(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        String error;

        if (!jsonMessage.has(JSON_STRATUM_KEY_ERROR))
        {
            throw new MalformedStratumMessageException(
                jsonMessage, String.format("missing '%s'", JSON_STRATUM_KEY_ERROR));
        }

        try
        {
            error = jsonMessage.getString(JSON_STRATUM_KEY_ERROR);
        }

        catch (JSONException ex)
        {
            throw new MalformedStratumMessageException(jsonMessage, ex);
        }

        this.setError(error);
    }
}