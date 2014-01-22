package com.redbottledesign.bitcoin.pool.rpc.stratum.client.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redbottledesign.bitcoin.pool.rpc.stratum.client.MiningClientEventListener;
import com.redbottledesign.bitcoin.pool.rpc.stratum.client.MiningClientEventNotifier;
import com.redbottledesign.bitcoin.pool.rpc.stratum.client.StratumMiningClient;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubscribeRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubscribeResponse;
import com.redbottledesign.bitcoin.rpc.stratum.transport.MessageListener;

/**
 * <p>The connection state for the Stratum mining client after it has
 * connected to the mining pool and authenticated the worker, but before it has
 * subscribed to receive work.</p>
 *
 * <p>Aside from the standard requests that are accepted in all connection
 * states ({@code client.get_version}, {@code mining.set_difficulty}, and the
 * no-op for {@code mining.notify}), this state does not accept any
 * requests.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 */
public class PendingSubscriptionState
extends AbstractMiningConnectionState
{
    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PendingSubscriptionState.class);

    /**
     * Constructor for {@link PendingSubscriptionState} that configures
     * the connection state for the specified Stratum mining transport.
     *
     * @param   transport
     *          The Stratum mining client message transport.
     */
    public PendingSubscriptionState(StratumMiningClient transport)
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

        this.registerResponseHandler(
            MiningSubscribeResponse.class,
            new MessageListener<MiningSubscribeResponse>()
            {
                @Override
                public void onMessageReceived(MiningSubscribeResponse message)
                {
                    PendingSubscriptionState.this.handleMiningSubscribe(message);
                }
            });
    }

    /**
     * {@inheritDoc}
     *
     * <p>When this state starts, it automatically attempts to subscribe to
     * work.</p>
     */
    @Override
    public void start()
    {
        final StratumMiningClient transport = this.getTransport();

        super.start();

        // Subscribe to mining details
        transport.sendRequest(
            new MiningSubscribeRequest(),
            MiningSubscribeResponse.class);
    }

    /**
     * <p>Handles the response returned by the pool for the
     * {@code mining.subscribe} message.</p>
     *
     * <p>In this implementation, if the subscription was successful,
     * listeners subscribed to the
     * {@link MiningClientEventListener#onSubscribed(MiningSubscribeResponse)}
     * event are notified, and the Stratum client is moved to the
     * {@link JobProcessingState} state. If the subscription failed, an error
     * is logged and the client disconnects from the mining pool.</p>
     *
     * @param   message
     *          The incoming response message.
     */
    protected void handleMiningSubscribe(final MiningSubscribeResponse message)
    {
        final StratumMiningClient transport = this.getTransport();

        if (message.wasRequestSuccessful())
        {
            transport.notifyEventListeners(new MiningClientEventNotifier()
            {
                @Override
                public void notifyListener(MiningClientEventListener listener)
                {
                    listener.onSubscribed(message);
                }
            });

            this.moveToState(new JobProcessingState(transport));
        }

        else
        {
            if (LOGGER.isErrorEnabled())
            {
                LOGGER.error(
                    String.format(
                        "Failed to subscribe to work: %s",
                        message.getError()));
            }

            // Close the connection; otherwise we'll be permanently stuck in this state.
            transport.close();
        }
    }
}