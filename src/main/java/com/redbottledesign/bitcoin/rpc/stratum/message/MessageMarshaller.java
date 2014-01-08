package com.redbottledesign.bitcoin.rpc.stratum.message;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.redbottledesign.bitcoin.rpc.stratum.MalformedStratumMessageException;

/**
 * <p>Class for marshalling Stratum messages into and out of JSON format.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 */
public class MessageMarshaller
{
    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageMarshaller.class);

    /**
     * The number of minutes that a request can be pending in a marshaller
     * instance before it is considered "ignored" (i.e. will not receive a
     * response).
     */
    protected static int IGNORED_REQUEST_TIMEOUT_MINUTES = 10;

    /**
     * <p>The name of the key in a Stratum message object that holds the result
     * of an operation.</p>
     *
     * <p>Messages that do not have this key are assumed to be request messages,
     * while messages that do have it are assumed to be responses.</p>
     */
    protected static final String JSON_MESSAGE_KEY_RESULT = "result";

    /**
     * The map of method names to request message types.
     */
    protected Map<String, Class<? extends StratumRequestMessage>> requestMethodMap;

    /**
     * The map of waiting requests to response message types.
     *
     * This map is purged as requests are answered or time-out.
     */
    protected Cache<Long, Class<? extends StratumResponseMessage>> requestResponseMap;

    /**
     * The constructor for {@link MessageMarshaller}.
     */
    public MessageMarshaller()
    {
        this.requestMethodMap   = new HashMap<>();
        this.requestResponseMap = this.createRequestResponseMap();
    }

    /**
     * Registers a concrete {@link StratumRequestMessage} type as the
     * appropriate message for un-marshalling requests for the specified
     * Stratum method.
     *
     * @param   methodName
     *          The name of the method, as it appears in requests.
     *
     * @param   messageType
     *          The type of concrete message type for the method.
     */
    public void registerRequestHandler(String methodName, Class<? extends StratumRequestMessage> messageType)
    {
        this.requestMethodMap.put(methodName, messageType);
    }

    /**
     * Gets the appropriate concrete message type to handle the specified
     * Stratum method.
     *
     * @param   methodName
     *          The name of the method for which a message type is desired.
     *
     * @return  The concrete message type that is used to handle the specified
     *          method; or {@code null} if this instance does not have any
     *          registered handlers for the specified method.
     */
    public Class<? extends StratumMessage> getRequestHandlerForMethod(String methodName)
    {
        return this.requestMethodMap.get(methodName);
    }

    /**
     * Registers the request having the specified ID as pending a response
     * message of the specified type.
     *
     * @param   requestId
     *          The unique identifier for the request.
     *
     * @param   responseType
     *          The type of response message expected from the request.
     *
     * @throws  IllegalArgumentException
     *          If {@code requestId} matches a request that is already
     *          registered as pending with this instance.
     */
    public void registerPendingRequest(long requestId, Class<? extends StratumResponseMessage> responseType)
    throws IllegalArgumentException
    {
        if (this.requestResponseMap.getIfPresent(requestId) != null)
            throw new IllegalArgumentException(String.format("A request with ID #%s is already pending.", requestId));

        this.requestResponseMap.put(requestId, responseType);
    }

    /**
     * Marshals the specified JSON message into a concrete Stratum message.
     *
     * @param   jsonMessage
     *          The JSON object containing the full message.
     *
     * @return  A concrete {@link StratumMessage} that represents the
     *          information from the provided JSON message.
     *
     * @throws  MalformedStratumMessageException
     *          If the JSON information is invalid or doesn't match any of the
     *          messages that this marshaller was expecting or is capable of
     *          marshalling.
     */
    public StratumMessage marshal(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        StratumMessage result;

        if (!jsonMessage.has(JSON_MESSAGE_KEY_RESULT))
            result = marshalRequestMessage(jsonMessage);

        else
            result = marshalResponseMessage(jsonMessage);

        return result;
    }

    /**
     * Marshals the specified JSON message into a concrete Stratum request message.
     *
     * @param   jsonMessage
     *          The JSON object containing the full request message.
     *
     * @return  A concrete {@link StratumMessage} that represents the
     *          request information from the provided JSON message.
     *
     * @throws  MalformedStratumMessageException
     *          If the JSON information is invalid or doesn't match any of the
     *          requests that this marshaller was expecting or is capable of
     *          marshalling.
     */
    protected StratumMessage marshalRequestMessage(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        StratumMessage                  result;
        StratumRequestMessage           request     = new StratumRequestMessage(jsonMessage);
        String                          methodName  = request.getMethodName();
        Class<? extends StratumMessage> requestType = this.requestMethodMap.get(methodName);

        if (requestType != null)
            result = this.marshalMessage(jsonMessage, requestType);

        else
            throw new MalformedStratumMessageException(methodName, jsonMessage);

        return result;
    }

    /**
     * Marshals the specified JSON message into a concrete Stratum response message.
     *
     * @param   jsonMessage
     *          The JSON object containing the full response message.
     *
     * @return  A concrete {@link StratumMessage} that represents the
     *          response information from the provided JSON message.
     *
     * @throws  MalformedStratumMessageException
     *          If the JSON information is invalid or doesn't match any of the
     *          responses that this marshaller was expecting or is capable of
     *          marshalling.
     */
    protected StratumMessage marshalResponseMessage(JSONObject jsonMessage)
    throws MalformedStratumMessageException
    {
        StratumMessage                  result;
        StratumResponseMessage          response     = new StratumResponseMessage(jsonMessage);
        Long                            messageId    = response.getId(); // Must always be set in responses
        Class<? extends StratumMessage> responseType = this.requestResponseMap.getIfPresent(messageId);

        if (responseType != null)
            result = this.marshalMessage(jsonMessage, responseType);

        else
            throw new MalformedStratumMessageException(jsonMessage);

        return result;
    }

    /**
     * Marshals the specified JSON message into the specified type of Stratum
     * message.
     *
     * @param   jsonMessage
     *          The JSON object containing the message.
     *
     * @return  A concrete {@link StratumMessage} that represents the
     *          information from the provided JSON message.
     *
     * @throws  MalformedStratumMessageException
     *          If the JSON information is invalid.
     */
    protected StratumMessage marshalMessage(JSONObject jsonMessage, Class<? extends StratumMessage> messageType)
    throws MalformedStratumMessageException
    {
        StratumMessage result;

        try
        {
            // NOTE: This can throw MalformedStratumMessageException
            result = messageType.getConstructor(JSONObject.class).newInstance(jsonMessage);
        }

        catch (InstantiationException    | IllegalAccessException | IllegalArgumentException |
               InvocationTargetException | NoSuchMethodException  | SecurityException ex)
        {
            throw new RuntimeException(
                String.format(
                    "Stratum message class '%s' is missing a constructor that takes a JSONObject.",
                    messageType.getName()));
        }

        return result;
    }

    /**
     * <p>Callback invoked when a request expires without a reply.</p>
     *
     * <p>The default behavior is only to log the expiration as an error.</p>
     *
     * @param   messageId
     *          The unique identifier for the message.
     *
     * @param   expectedResponseType
     *          The type of response that was expected for the message.
     */
    protected void onRequestExpired(Long messageId, Class<? extends StratumResponseMessage> expectedResponseType)
    {
        if (LOGGER.isErrorEnabled())
        {
            LOGGER.error(
                String.format(
                    "Request #%d (expecting a response of type '%s') was expired after %d minutes of being ignored " +
                    "without receiving a reply.",
                    messageId,
                    expectedResponseType.getName(),
                    IGNORED_REQUEST_TIMEOUT_MINUTES));
        }
    }

    /**
     * Creates a new request response map, which holds requests that are waiting for responses.
     *
     * @return  A new cache for waiting request responses.
     */
    protected Cache<Long, Class<? extends StratumResponseMessage>> createRequestResponseMap()
    {
        return CacheBuilder
            .newBuilder()
            .expireAfterWrite(IGNORED_REQUEST_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .removalListener(new RemovalListener<Long, Class<? extends StratumResponseMessage>>()
            {
                @Override
                public void onRemoval(RemovalNotification<Long, Class<? extends StratumResponseMessage>> notification)
                {
                    if (notification.getCause() == RemovalCause.EXPIRED)
                    {
                        MessageMarshaller.this.onRequestExpired(
                            notification.getKey(),
                            notification.getValue());
                    }
                }
            }).build();
    }
}