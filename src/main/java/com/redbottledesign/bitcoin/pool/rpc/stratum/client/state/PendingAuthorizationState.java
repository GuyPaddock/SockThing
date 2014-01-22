package com.redbottledesign.bitcoin.pool.rpc.stratum.client.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redbottledesign.bitcoin.pool.rpc.stratum.client.MiningClientEventListener;
import com.redbottledesign.bitcoin.pool.rpc.stratum.client.MiningClientEventNotifier;
import com.redbottledesign.bitcoin.pool.rpc.stratum.client.StratumMiningClient;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningAuthorizeRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningAuthorizeResponse;
import com.redbottledesign.bitcoin.rpc.stratum.transport.MessageListener;

/**
 * <p>The connection state for the Stratum mining client after it has
 * connected to the mining pool but before it has authenticated.</p>
 *
 * <p>Aside from the standard {@code mining.set_difficulty} and
 * {@code client.get_version} requests that are accepted in all connection
 * states, this state does not accept any requests.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 */
public class PendingAuthorizationState
extends AbstractMiningConnectionState
{
    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PendingAuthorizationState.class);

    /**
     * Constructor for {@link PendingAuthorizationState} that configures
     * the connection state for the specified Stratum mining transport.
     *
     * @param   transport
     *          The Stratum mining client message transport.
     */
    public PendingAuthorizationState(StratumMiningClient transport)
    {
        super(transport);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initializeHandlers()
    {
        super.initializeHandlers();

        // mining.authorize response
        this.registerResponseHandler(
            MiningAuthorizeResponse.class,
            new MessageListener<MiningAuthorizeResponse>()
            {
                @Override
                public void onMessageReceived(MiningAuthorizeResponse message)
                {
                    PendingAuthorizationState.this.handleMiningAuthorizeResponse(message);
                }
            });
    }

    /**
     * {@inheritDoc}
     *
     * <p>When this state starts, it automatically attempts to authenticate the
     * worker.</p>
     */
    @Override
    public void start()
    {
        final StratumMiningClient transport = this.getTransport();

        super.start();

        // Authorize the worker
        transport.sendRequest(
            new MiningAuthorizeRequest(
                transport.getWorkerUsername(),
                transport.getWorkerPassword()),
            MiningAuthorizeResponse.class);
    }

    /**
     * <p>Handles the response returned by the pool for the
     * {@code mining.authorize} message.</p>
     *
     * <p>In this implementation, if the worker was successfully authenticated,
     * listeners subscribed to the
     * {@link MiningClientEventListener#onAuthenticated(MiningAuthorizeResponse)}
     * event are notified, and the Stratum client is moved to the
     * {@link PendingSubscriptionState} state. If the worker was not successfully
     * authenticated, an error is logged and the client disconnects from the
     * mining pool.</p>
     *
     * @param   message
     *          The incoming response message.
     */
    protected void handleMiningAuthorizeResponse(final MiningAuthorizeResponse message)
    {
        final StratumMiningClient transport = this.getTransport();

        if (message.isAuthorized())
        {
            transport.notifyEventListeners(
                new MiningClientEventNotifier()
                {
                    @Override
                    public void notifyListener(MiningClientEventListener listener)
                    {
                        listener.onAuthenticated(message);
                    }
                });

            this.moveToState(new PendingSubscriptionState(transport));
        }

        else
        {
            if (LOGGER.isErrorEnabled())
            {
                LOGGER.error(
                    String.format(
                        "Failed to authenticate worker \"%s\": %s",
                        transport.getWorkerUsername(),
                        message.getError()));
            }

            // Close the connection; otherwise we'll be permanently stuck in this state.
            transport.close();
        }
    }
}