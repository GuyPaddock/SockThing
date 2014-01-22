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
 * <p>� 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (gpaddock@redbottledesign.com)
 */
public class ResponseMessage
extends Message
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
    private Result result;

    /**
     * An error message describing why the last request could not be completed,
     * if the request failed.
     */
    private String error;

    /**
     * Constructor for {@link ResponseMessage} that initializes a new
     * instance from information in the included JSON message.
     *
     * @param   jsonMessage
     *          The JSON message object.
     *
     * @throws  MalformedStratumMessageException
     *          If the provided JSON message object is not a properly-formed
     *          Stratum message or cannot be understood.
     */
    public ResponseMessage(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        super(jsonMessage);
    }

    /**
     * Constructor for {@link ResponseMessage} that initializes a new
     * instance having the specified ID and result.
     *
     * @param   id
     *          The unique identifier for the message. This may be
     *          {@code null}.
     *
     * @param   result
     *          The result of the method call.
     */
    public ResponseMessage(String id, Result result)
    {
        super(id);

        this.setResult(result);
    }

    /**
     * Constructor for {@link ResponseMessage} that initializes a new
     * instance having the specified numeric ID and error.
     *
     * @param   id
     *          The unique identifier for the message. This may be
     *          {@code null}.
     *
     * @param   error
     *          The error that occurred while processing the request.
     */
    public ResponseMessage(String id, String error)
    {
        super(id);

        this.setError(error);
    }

    /**
     * Gets the result of the method call.
     *
     * @return  The result of the method call.
     */
    public Result getResult()
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
     * Sets the unique identifier for this response, which must correspond
     * to the identifier provided in the original request.
     *
     * @param   id
     *          Sets the unique identifier for the message. This cannot be
     *          {@code null}.
     *
     * @throws  IllegalArgumentException
     *          If {@code id} is {@code null}.
     */
    @Override
    protected void setId(String id)
    throws IllegalArgumentException
    {
        if (id == null)
            throw new IllegalArgumentException("id cannot be null.");

        super.setId(id);
    }

    /**
     * Sets the result of the method call.
     *
     * @param   result
     *          The new result of the method call.
     */
    protected void setResult(Result result)
    {
        if ((result != null) && (this.getError() != null))
            throw new IllegalArgumentException("Result must be null if an error is set.");

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
        if ((error != null) && (this.getResult() != null))
            throw new IllegalArgumentException("Error must be null if a result is set.");

        this.error = error;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void parseMessage(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        this.parseResult(jsonMessage);
        this.parseError(jsonMessage);

        // Call superclass last, since it calls validateParsedData()
        super.parseMessage(jsonMessage);
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
        Result result;

        if (!jsonMessage.has(JSON_STRATUM_KEY_RESULT))
        {
            throw new MalformedStratumMessageException(
                jsonMessage, String.format("missing '%s'", JSON_STRATUM_KEY_RESULT));
        }

        try
        {
            result = ResultFactory.getInstance().createResult(jsonMessage.get(JSON_STRATUM_KEY_RESULT));
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
        String error = null;

        if (!jsonMessage.has(JSON_STRATUM_KEY_ERROR))
        {
            throw new MalformedStratumMessageException(
                jsonMessage, String.format("missing '%s'", JSON_STRATUM_KEY_ERROR));
        }

        if (!jsonMessage.isNull(JSON_STRATUM_KEY_ERROR))
        {
            try
            {
                error = jsonMessage.getString(JSON_STRATUM_KEY_ERROR);
            }

            catch (JSONException ex)
            {
                throw new MalformedStratumMessageException(jsonMessage, ex);
            }
        }

        this.setError(error);
    }
}