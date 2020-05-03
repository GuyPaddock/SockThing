package com.redbottledesign.bitcoin.pool.rpc.stratum.server.state;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningAuthorizeRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningAuthorizeResponse;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningResumeRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningResumeResponse;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubscribeRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubscribeResponse;
import com.redbottledesign.bitcoin.pool.rpc.stratum.server.AmbiguousResponseException;
import com.redbottledesign.bitcoin.pool.rpc.stratum.server.MiningServerConnection;
import com.redbottledesign.bitcoin.pool.rpc.stratum.server.MiningServerEventListener;
import com.redbottledesign.bitcoin.pool.rpc.stratum.server.MiningServerEventNotifier;
import com.redbottledesign.bitcoin.pool.rpc.stratum.server.UnhandledRequestEventException;
import com.redbottledesign.bitcoin.rpc.stratum.transport.AbstractConnectionState;
import com.redbottledesign.bitcoin.rpc.stratum.transport.ConnectionState;
import com.redbottledesign.bitcoin.rpc.stratum.transport.tcp.StratumTcpServerConnection;

/**
 * <p>Abstract base class for all Stratum mining server connection states.</p>
 *
 * <p>This class configures all connection states to be able to accept the
 * following request messages:</p>
 *
 * <dl>
 *  <p>
 *    <dt>{@code mining.subscribe}</dt>
 *    <dd>Used to subscribe to work from a server, required before all other
 *        share-specific communication.</dd>
 *  </p>
 *
 *  <p>
 *    <dt>{@code mining.authorize}</dt>
 *    <dd>Used to authorize a worker, required before any shares can be
 *        submitted.</dd>
 *  </p>
 *
 *  <p>
 *    <dt>{@code mining.resume}</dt>
 *    <dd>Used to enable a session to resume a past session after a connection
 *        interruption (no authorization is needed).</dd>
 *  </p>
 * </dl>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 */
public abstract class AbstractMiningServerConnectionState
extends AbstractConnectionState
{
    /*
     * mining.subscribe
     * mining.authorize
     * mining.resume
     * mining.submit
     *
     * mining.pool.stop
     *
     * mining.pool.queue.persistence.evict
     * mining.pool.queue.persistence.evict-all
     *
     * mining.pool.queue.pplns.evict
     * mining.pool.queue.pplns.evict-all
     */
    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMiningServerConnectionState.class);

    /**
     * Constructor for {@link AbstractMiningServerConnectionState} that configures
     * the connection state for the specified Stratum mining transport.
     *
     * @param   transport
     *          The Stratum TCP server connection, which serves as a message
     *          transport.
     */
    public AbstractMiningServerConnectionState(MiningServerConnection transport)
    {
        super(transport);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This override narrows the type of object being returned to be of the
     * type {@link MiningServerConnection}, to eliminate unnecessary casts
     * elsewhere in this implementation.</p>
     */
    @Override
    public MiningServerConnection getTransport()
    {
        return (MiningServerConnection)super.getTransport();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initializeHandlers()
    {
        super.initializeHandlers();
    }

    /**
     * <p>Handles the {@code mining.authorize} request message.</p>
     *
     * <p>This implementation notifies all listeners who subscribe to the
     * {@link MiningServerEventListener#onClientAuthenticating(StratumTcpServerConnection, MiningAuthorizeRequest)}
     * event, expecting that at least one will approve or deny the worker's
     * authentication request.</p>
     *
     * <p>If no listener returns a response to the event, an
     * {@link UnhandledRequestEventException} is thrown.</p>
     *
     * @see MiningServerEventListener#onClientAuthenticating(StratumTcpServerConnection, MiningAuthorizeRequest)
     *
     * @param   message
     *          The incoming request message.
     *
     * @throws  UnhandledRequestEventException
     *          If no listener approved or denied the worker's authentication
     *          request.
     */
    protected void handleMiningAuthorizeRequest(final MiningAuthorizeRequest message)
    throws UnhandledRequestEventException
    {
        final MiningServerConnection        connection      = this.getTransport();
        final List<MiningAuthorizeResponse> responses       = new LinkedList<>();
        MiningAuthorizeResponse             finalResponse;

        connection.getServer().notifyEventListeners(
            new MiningServerEventNotifier()
            {
                @Override
                public void notifyListener(MiningServerEventListener listener)
                {
                    final MiningAuthorizeResponse response = listener.onClientAuthenticating(connection, message);

                    if (response != null)
                        responses.add(response);
                }
            });

        if (responses.isEmpty())
        {
            throw new UnhandledRequestEventException(
                String.format(
                    "A '%s' message was received from a client, but no listener responded to it. Unable to approve " +
                    "or deny the client's authorization request.",
                    message.getMethodName()));
        }

        finalResponse = this.determineAppropriateAuthorizeResponse(responses);

        connection.sendResponse(finalResponse);

        if (finalResponse.isAuthorized())
            this.moveToState(this.determineNextConnectionState(false, true));

        else
            connection.close();
    }

    /**
     * <p>Handles the {@code mining.resume} request message.</p>
     *
     * <p>This implementation notifies all listeners who subscribe to the
     * {@link MiningServerEventListener#onClientResumingSession(StratumTcpServerConnection, MiningResumeRequest)}
     * event, expecting that at least one will approve or deny the worker's
     * session resume request.</p>
     *
     * <p>If no listener returns a response to the event, an
     * {@link UnhandledRequestEventException} is thrown.</p>
     *
     * @see MiningServerEventListener#onClientResumingSession(StratumTcpServerConnection, MiningResumeRequest)
     *
     * @param   message
     *          The incoming request message.
     *
     * @throws  UnhandledRequestEventException
     *          If no listener approved or denied the worker's session resume
     *          request.
     */
    protected void handleMiningResumeRequest(final MiningResumeRequest message)
    throws UnhandledRequestEventException
    {
        final MiningServerConnection        connection      = this.getTransport();
        final List<MiningResumeResponse>    responses       = new LinkedList<>();
        MiningResumeResponse                finalResponse;

        connection.getServer().notifyEventListeners(
            new MiningServerEventNotifier()
            {
                @Override
                public void notifyListener(MiningServerEventListener listener)
                {
                    final MiningResumeResponse response = listener.onClientResumingSession(connection, message);

                    if (response != null)
                        responses.add(response);
                }
            });

        if (responses.isEmpty())
        {
            throw new UnhandledRequestEventException(
                String.format(
                    "A '%s' message was received from a client, but no listener responded to it. Unable to approve " +
                    "or deny the client's request to resume the session.",
                    message.getMethodName()));
        }

        finalResponse = this.determineAppropriateResumeResponse(responses);

        connection.sendResponse(finalResponse);
    }

    /**
     * <p>Handles the {@code mining.subscribe} request message.</p>
     *
     * <p>This implementation notifies all listeners who subscribe to the
     * {@link MiningServerEventListener#onClientSubscribing(StratumTcpServerConnection, MiningSubscribeRequest)}
     * event, expecting that at least one will respond to the request.</p>
     *
     * <p>If no listener returns a response to the event, an
     * {@link UnhandledRequestEventException} is thrown. If more than one listener
     * returns a response to the event, an {@link AmbiguousResponseException}
     * is thrown.</p>
     *
     * @see MiningServerEventListener#onClientSubscribing(StratumTcpServerConnection, MiningSubscribeRequest)
     *
     * @param   message
     *          The incoming request message.
     *
     * @throws  UnhandledRequestEventException
     *          If no listener provided a response to the subscribe request.
     *
     * @throws  AmbiguousResponseException
     *          If more than one listener provided a response to the subscribe
     *          request.
     */
    protected void handleMiningSubscribeRequest(final MiningSubscribeRequest message)
    throws UnhandledRequestEventException, AmbiguousResponseException
    {
        final MiningServerConnection        connection  = this.getTransport();
        final List<MiningSubscribeResponse> responses   = new LinkedList<>();

        connection.getServer().notifyEventListeners(
            new MiningServerEventNotifier()
            {
                @Override
                public void notifyListener(MiningServerEventListener listener)
                {
                    final MiningSubscribeResponse response = listener.onClientSubscribing(connection, message);

                    if (response != null)
                        responses.add(response);
                }
            });

        if (responses.isEmpty())
        {
            throw new UnhandledRequestEventException(
                String.format(
                    "A '%s' message was received from a client, but no listener responded to it. Unable to respond to" +
                    "subscription request.",
                    message.getMethodName()));
        }

        if (responses.size() > 1)
        {
            throw new AmbiguousResponseException(
                String.format(
                    "A '%s' message was received from a client, and multiple listeners responded to it. Unable to " +
                    "determine what response should be sent back to the client.",
                    message.getMethodName()));
        }

        connection.sendResponse(responses.get(0));

        this.moveToState(this.determineNextConnectionState(true, false));
    }

    /**
     * <p>Determines the appropriate response to send back among the list of
     * responses listeners generated to a {@code mining.authorize} request.</p>
     *
     * <p>The first response that authorizes the worker is sent back, and the
     * rest of the responses are discarded. If all responses denied the worker's
     * request, the first response is sent back.</p>
     *
     * @param   responses
     *          The list of responses to the event.
     *
     * @return  The appropriate response to send back to the client for the
     *          request.
     */
    protected MiningAuthorizeResponse determineAppropriateAuthorizeResponse(final List<MiningAuthorizeResponse> responses)
    {
        final int               responseCount   = responses.size();
        MiningAuthorizeResponse finalResponse   = responses.get(0);

        if (responseCount > 1)
        {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug(
                    String.format(
                        "determineAppropriateResponse(): %d responses were returned from listeners, but ideally " +
                        "there should be only one. The first response that approves the client's authorization " +
                        "request will be sent back and the rest will be discarded.",
                        responseCount));
            }

            for (MiningAuthorizeResponse response : responses)
            {
                // Take the first approval
                if (response.isAuthorized())
                {
                    finalResponse = response;
                    break;
                }
            }
        }

        return finalResponse;
    }

    /**
     * <p>Determines the appropriate response to send back among the list of
     * responses listeners generated to a {@code mining.resume} request.</p>
     *
     * <p>The first response that enables the worker to resume the session is
     * sent back, and the rest of the responses are discarded. If all responses
     * denied the worker's request, the first response is sent back.</p>
     *
     * @param   responses
     *          The list of responses to the event.
     *
     * @return  The appropriate response to send back to the client for the
     *          request.
     */
    protected MiningResumeResponse determineAppropriateResumeResponse(final List<MiningResumeResponse> responses)
    {
        final int               responseCount   = responses.size();
        MiningResumeResponse    finalResponse   = responses.get(0);

        if (responseCount > 1)
        {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug(
                    String.format(
                        "determineAppropriateResumeResponse(): %d responses were returned from listeners, but " +
                        "ideally there should be only one. The first response that approves the client's resume " +
                        "request will be sent back and the rest will be discarded.",
                        responseCount));
            }

            for (MiningResumeResponse response : responses)
            {
                // Take the first approval
                if (response.wasResumed())
                {
                    finalResponse = response;
                    break;
                }
            }
        }

        return finalResponse;
    }

    /**
     * <p>Determines the appropriate next state to move to given the specified
     * subscribed and authorized statuses.</p>
     *
     * <p>Sub-classes can override this behavior to return the state that is
     * appropriate from the current state. For example, when the connection is
     * in a state in which the worker is already subscribed, the value provided
     * for {@code subscribed} would not affect the result.</p>
     *
     * @param   subscribed
     *          Whether or not the worker is subscribed.
     *
     * @param   authorized
     *          Whether or not the worker has authenticated.
     *
     * @return  The state the connection should move to.
     */
    protected ConnectionState determineNextConnectionState(boolean subscribed, boolean authorized)
    {
        ConnectionState        result;
        MiningServerConnection connection = this.getTransport();

        if (subscribed && authorized)
            result = new MiningState(connection);

        else if (subscribed)
            result = new SubscribedPendingAuthorizationState(connection);

        else if (authorized)
            result = new AuthorizedPendingSubscriptionState(connection);

        else
            throw new IllegalArgumentException("Must be either subscribed or authorized to move to another state.");

        return result;
    }
}
