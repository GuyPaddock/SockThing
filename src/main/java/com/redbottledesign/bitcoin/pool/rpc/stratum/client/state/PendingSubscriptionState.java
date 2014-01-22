package com.redbottledesign.bitcoin.pool.rpc.stratum.client.state;

import com.redbottledesign.bitcoin.pool.rpc.stratum.client.MiningClientEventListener;
import com.redbottledesign.bitcoin.pool.rpc.stratum.client.MiningClientEventNotifier;
import com.redbottledesign.bitcoin.pool.rpc.stratum.client.StratumMiningClient;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubscribeRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubscribeResponse;
import com.redbottledesign.bitcoin.rpc.stratum.transport.MessageListener;

public class PendingSubscriptionState
extends AbstractMiningConnectionState
{
    public PendingSubscriptionState(StratumMiningClient transport)
    {
        super(transport);
    }

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

    protected void handleMiningSubscribe(final MiningSubscribeResponse message)
    {
        final StratumMiningClient transport = this.getTransport();

        transport.notifyEventListeners(new MiningClientEventNotifier()
        {
            @Override
            public void notifyListener(MiningClientEventListener listener)
            {
                listener.onMinerSubscribed(message);
            }
        });

        this.moveToState(new PendingAuthorizationState(transport));
    }
}