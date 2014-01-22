package com.redbottledesign.bitcoin.pool.rpc.stratum.client.state;

import com.redbottledesign.bitcoin.pool.rpc.stratum.client.MiningClientEventListener;
import com.redbottledesign.bitcoin.pool.rpc.stratum.client.MiningClientEventNotifier;
import com.redbottledesign.bitcoin.pool.rpc.stratum.client.StratumMiningClient;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningNotifyRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubmitResponse;
import com.redbottledesign.bitcoin.rpc.stratum.message.MessageMarshaller;
import com.redbottledesign.bitcoin.rpc.stratum.transport.MessageListener;

public class JobProcessingState
extends AbstractMiningConnectionState
{
    public JobProcessingState(StratumMiningClient transport)
    {
        super(transport);
    }

    @Override
    protected void initializeHandlers()
    {
        super.initializeHandlers();

        this.registerRequestHandler(
            MiningNotifyRequest.class,
            new MessageListener<MiningNotifyRequest>()
            {
                @Override
                public void onMessageReceived(MiningNotifyRequest message)
                {
                    JobProcessingState.this.handleMiningNotifyRequest(message);
                }
            });

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

    @Override
    protected MessageMarshaller createMarshaller()
    {
        final MessageMarshaller marshaller = super.createMarshaller();

        marshaller.registerRequestHandler(MiningNotifyRequest.METHOD_NAME, MiningNotifyRequest.class);

        return marshaller;
    }

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