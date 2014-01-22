package com.redbottledesign.bitcoin.pool.rpc.stratum.client.state;

import com.redbottledesign.bitcoin.pool.rpc.stratum.client.MiningClientEventListener;
import com.redbottledesign.bitcoin.pool.rpc.stratum.client.MiningClientEventNotifier;
import com.redbottledesign.bitcoin.pool.rpc.stratum.client.StratumMiningClient;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.ClientGetVersionRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.ClientGetVersionResponse;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSetDifficultyRequest;
import com.redbottledesign.bitcoin.rpc.stratum.message.MessageMarshaller;
import com.redbottledesign.bitcoin.rpc.stratum.transport.AbstractConnectionState;
import com.redbottledesign.bitcoin.rpc.stratum.transport.MessageListener;

public abstract class AbstractMiningConnectionState
extends AbstractConnectionState
{
    public AbstractMiningConnectionState(StratumMiningClient transport)
    {
        super(transport);
    }

    @Override
    protected void initializeHandlers()
    {
        super.initializeHandlers();

        this.registerRequestHandler(
            MiningSetDifficultyRequest.class,
            new MessageListener<MiningSetDifficultyRequest>()
            {
                @Override
                public void onMessageReceived(MiningSetDifficultyRequest message)
                {
                    AbstractMiningConnectionState.this.handleSetDifficulty(message);
                }
            });

        this.registerRequestHandler(
            ClientGetVersionRequest.class,
            new MessageListener<ClientGetVersionRequest>()
            {
                @Override
                public void onMessageReceived(ClientGetVersionRequest message)
                {
                    AbstractMiningConnectionState.this.handleGetVersion(message);
                }
            });
    }

    @Override
    public StratumMiningClient getTransport()
    {
        return (StratumMiningClient)super.getTransport();
    }

    @Override
    protected MessageMarshaller createMarshaller()
    {
        final MessageMarshaller marshaller = super.createMarshaller();

        marshaller.registerRequestHandler(
            MiningSetDifficultyRequest.METHOD_NAME,
            MiningSetDifficultyRequest.class);

        marshaller.registerRequestHandler(
            ClientGetVersionRequest.METHOD_NAME,
            ClientGetVersionRequest.class);

        return marshaller;
    }

    protected void handleSetDifficulty(final MiningSetDifficultyRequest message)
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

    protected void handleGetVersion(ClientGetVersionRequest message)
    {
        final StratumMiningClient transport = this.getTransport();

        transport.sendResponse(
            new ClientGetVersionResponse(
                message.getId(),
                transport.getClientVersionString()));
    }
}