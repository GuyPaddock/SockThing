package com.redbottledesign.bitcoin.pool.rpc.stratum.client.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redbottledesign.bitcoin.pool.rpc.stratum.client.MiningClientEventListener;
import com.redbottledesign.bitcoin.pool.rpc.stratum.client.MiningClientEventNotifier;
import com.redbottledesign.bitcoin.pool.rpc.stratum.client.StratumMiningClient;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.ClientGetVersionRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.ClientGetVersionResponse;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningNotifyRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSetDifficultyRequest;
import com.redbottledesign.bitcoin.rpc.stratum.transport.AbstractConnectionState;
import com.redbottledesign.bitcoin.rpc.stratum.transport.MessageListener;

/**
 * <p>Abstract base class for all Stratum mining client connection states.</p>
 *
 * <p>This class configures all connection states to be able to accept the
 * following request messages:</p>
 *
 * <dl>
 *  <p>
 *    <dt>{@code client.get_version}</dt>
 *    <dd>Used to get the name and version of the mining software being
 *        used.</dd>
 *  </p>
 *
 *  <p>
 *    <dt>{@code mining.set_difficulty}</dt>
 *    <dd>Used to signal the miner to stop submitting shares under the new
 *        difficulty.</dd>
 *  </p>
 * </dl>
 *
 * <p>This class also provides a no-op implementation for the following request
 * message, which is handled normally in the {@link JobProcessingState}:</p>
 *
 * <dl>
 *    <dt>{@code mining.notify}</dt>
 *    <dd>Used to push new work to the miner.</dd>
 * </dl>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 *
 */
public abstract class AbstractMiningClientConnectionState
extends AbstractConnectionState
{
    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMiningClientConnectionState.class);

    /**
     * Constructor for {@link AbstractMiningClientConnectionState} that configures
     * the connection state for the specified Stratum mining transport.
     *
     * @param   transport
     *          The Stratum mining client message transport.
     */
    public AbstractMiningClientConnectionState(StratumMiningClient transport)
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

        // client.get_version
        this.registerRequestHandler(
            ClientGetVersionRequest.METHOD_NAME,
            ClientGetVersionRequest.class,
            new MessageListener<ClientGetVersionRequest>()
            {
                @Override
                public void onMessageReceived(ClientGetVersionRequest message)
                {
                    AbstractMiningClientConnectionState.this.handleGetVersionRequest(message);
                }
            });

        // mining.set_difficulty
        this.registerRequestHandler(
            MiningSetDifficultyRequest.METHOD_NAME,
            MiningSetDifficultyRequest.class,
            new MessageListener<MiningSetDifficultyRequest>()
            {
                @Override
                public void onMessageReceived(MiningSetDifficultyRequest message)
                {
                    AbstractMiningClientConnectionState.this.handleSetDifficultyRequest(message);
                }
            });

        // mining.notify (no-op)
        this.registerRequestHandler(
            MiningNotifyRequest.METHOD_NAME,
            MiningNotifyRequest.class,
            new MessageListener<MiningNotifyRequest>()
            {
                @Override
                public void onMessageReceived(MiningNotifyRequest message)
                {
                    AbstractMiningClientConnectionState.this.handleMiningNotifyRequest(message);
                }
            });
    }

    /**
     * {@inheritDoc}
     *
     * <p>This override narrows the type of object being returned to be of the
     * type {@link StratumMiningClient}, to eliminate unnecessary casts
     * elsewhere in this implementation.</p>
     */
    @Override
    public StratumMiningClient getTransport()
    {
        return (StratumMiningClient)super.getTransport();
    }

    /**
     * <p>Handles the {@code mining.set_difficulty} message.</p>
     *
     * <p>This implementation does nothing other than notify listeners who
     * subscribe to the
     * {@link MiningClientEventListener#onDifficultySet(MiningSetDifficultyRequest)}
     * event.</p>
     *
     * @param   message
     *          The incoming request message.
     */
    protected void handleSetDifficultyRequest(final MiningSetDifficultyRequest message)
    {
        this.getTransport().notifyEventListeners(
            new MiningClientEventNotifier()
            {
                @Override
                public void notifyListener(MiningClientEventListener listener)
                {
                    listener.onDifficultySet(message);
                }
            });
    }

    /**
     * <p>Handles the {@code client.get_version} message.</p>
     *
     * <p>This implementation automatically responds with the version string
     * that has been set on the {@link StratumMiningClient}.</p>
     *
     * @param   message
     *          The incoming request message.
     *
     * @see     StratumMiningClient#getClientVersionString()
     * @see     StratumMiningClient#setClientVersionString(String)
     */
    protected void handleGetVersionRequest(ClientGetVersionRequest message)
    {
        final StratumMiningClient transport = this.getTransport();

        transport.sendResponse(
            new ClientGetVersionResponse(
                message.getId(),
                transport.getClientVersionString()));
    }

    /**
     * <p>Handles the {@code mining.notify} message.</p>
     *
     * <p>This implementation does nothing other than log that the request was
     * not handled.</p>
     *
     * @param   message
     *          The incoming request message.
     */
    protected void handleMiningNotifyRequest(final MiningNotifyRequest message)
    {
        // No-op implementation until we're in the JobProcessingState
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(
                String.format(
                    "A '%s' request was received and dropped because the Stratum mining client was not " +
                    "prepared to receive it: %s",
                    MiningNotifyRequest.METHOD_NAME,
                    message.toJson()));
        }
    }
}
