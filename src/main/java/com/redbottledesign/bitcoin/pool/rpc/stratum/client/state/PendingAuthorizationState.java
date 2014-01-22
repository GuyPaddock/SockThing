package com.redbottledesign.bitcoin.pool.rpc.stratum.client.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redbottledesign.bitcoin.pool.rpc.stratum.client.MiningClientEventListener;
import com.redbottledesign.bitcoin.pool.rpc.stratum.client.MiningClientEventNotifier;
import com.redbottledesign.bitcoin.pool.rpc.stratum.client.StratumMiningClient;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningAuthorizeRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningAuthorizeResponse;
import com.redbottledesign.bitcoin.rpc.stratum.transport.MessageListener;

public class PendingAuthorizationState
extends JobProcessingState
{
    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PendingAuthorizationState.class);

    public PendingAuthorizationState(StratumMiningClient transport)
    {
        super(transport);
    }

    @Override
    protected void initializeHandlers()
    {
        super.initializeHandlers();

        this.registerResponseHandler(
            MiningAuthorizeResponse.class,
            new MessageListener<MiningAuthorizeResponse>()
            {
                @Override
                public void onMessageReceived(MiningAuthorizeResponse message)
                {
                    PendingAuthorizationState.this.handleMiningAuthorize(message);
                }
            });
    }

    @Override
    public void start()
    {
        final StratumMiningClient transport = this.getTransport();

        super.start();

        // Authorize workers
        transport.sendRequest(
            new MiningAuthorizeRequest(
                transport.getWorkerUsername(),
                transport.getWorkerPassword()),
            MiningAuthorizeResponse.class);
    }

    protected void handleMiningAuthorize(final MiningAuthorizeResponse message)
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
                        listener.onWorkerAuthenticated(message);
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
                        "Failed to authenticate worker \"%s\": %s",
                        transport.getWorkerUsername(),
                        message.getError()));
            }

            // Close the connection; otherwise we'll be permanently stuck in this state.
            transport.close();
        }
    }
}