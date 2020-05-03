package com.redbottledesign.bitcoin.pool.rpc.stratum.client.state;

import com.redbottledesign.bitcoin.pool.rpc.stratum.client.MiningClientEventListener;
import com.redbottledesign.bitcoin.pool.rpc.stratum.client.MiningClientEventNotifier;
import com.redbottledesign.bitcoin.pool.rpc.stratum.client.StratumMiningClient;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningNotifyRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubmitResponse;
import com.redbottledesign.bitcoin.rpc.stratum.transport.MessageListener;

/**
 * <p>The connection state for the Stratum mining client when it is eligible
 * to begin receiving work from the mining pool.</p>
 *
 * <p>Aside from the standard requests that are accepted in all connection
 * states ({@code client.get_version} and {@code mining.set_difficulty}), this
 * state provides proper handling for the following type of request:</p>
 *
 * <dl>
 *    <dt>{@code mining.notify}</dt>
 *    <dd>Used to push new work to the miner.</dd>
 * </dl>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 */
public class JobProcessingState
extends AbstractMiningClientConnectionState
{
    /**
     * Constructor for {@link JobProcessingState} that configures
     * the connection state for the specified Stratum mining transport.
     *
     * @param   transport
     *          The Stratum mining client message transport.
     */
    public JobProcessingState(StratumMiningClient transport)
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

        // mining.submit response
        this.registerResponseHandler(
            MiningSubmitResponse.class,
            new MessageListener<MiningSubmitResponse>()
            {
                @Override
                public void onMessageReceived(MiningSubmitResponse message)
                {
                    JobProcessingState.this.handleMiningSubmitResponse(message);
                }
            });
    }

    /**
     * <p>Handles the {@code mining.notify} message.</p>
     *
     * <p>This implementation replaces the default, no-op implementation with one that
     * notifies listeners who subscribe to the
     * {@link MiningClientEventListener#onNewWorkReceived(MiningNotifyRequest)}
     * event.</p>
     *
     * @param   message
     *          The incoming request message.
     */
    @Override
    protected void handleMiningNotifyRequest(final MiningNotifyRequest message)
    {
        this.getTransport().notifyEventListeners(
            new MiningClientEventNotifier()
            {
                @Override
                public void notifyListener(MiningClientEventListener listener)
                {
                    listener.onNewWorkReceived(message);
                }
            });
    }

    /**
     * <p>Handles the response returned by the pool for the
     * {@code mining.submit} message.</p>
     *
     * <p>This implementation does nothing other than notify listeners who
     * subscribe to the
     * {@link MiningClientEventListener#onWorkSubmitted(MiningSubmitResponse)}
     * event.</p>
     *
     * @param   message
     *          The incoming response message.
     */
    protected void handleMiningSubmitResponse(final MiningSubmitResponse message)
    {
        this.getTransport().notifyEventListeners(
            new MiningClientEventNotifier()
            {
                @Override
                public void notifyListener(MiningClientEventListener listener)
                {
                    listener.onWorkSubmitted(message);
                }
            });
    }
}
