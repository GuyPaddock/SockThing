package com.redbottledesign.bitcoin.pool.rpc.stratum.server;

import java.net.Socket;
import java.util.Set;

import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningAuthorizeRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningAuthorizeResponse;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningResumeRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningResumeResponse;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubmitRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubmitResponse;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubscribeRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubscribeResponse;
import com.redbottledesign.bitcoin.pool.rpc.stratum.server.state.PendingAuthorizationOrSubscriptionState;
import com.redbottledesign.bitcoin.rpc.stratum.transport.ConnectionState;
import com.redbottledesign.bitcoin.rpc.stratum.transport.tcp.StratumTcpServer;
import com.redbottledesign.bitcoin.rpc.stratum.transport.tcp.StratumTcpServerConnection;

/**
 * <p>Stateful Stratum mining server implementation over TCP.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 */
public class StratumMiningServer
extends StratumTcpServer
{
    /**
     * The set of mining server event listeners.
     */
    protected Set<MiningServerEventListener> serverEventListeners;

    /**
     * Registers a new server event listener, which will be informed about all
     * future Stratum mining events that affect this server.
     *
     * @param   listener
     *          The listener to register.
     */
    public void registerEventListener(MiningServerEventListener listener)
    {
        this.serverEventListeners.add(listener);
    }

    /**
     * Unregisters a new server event listener, which will no longer be
     * informed about future Stratum mining events that affect this server.
     *
     * @param   listener
     *          The listener to register.
     */
    public void unregisterEventListener(MiningServerEventListener listener)
    {
        this.serverEventListeners.remove(listener);
    }

    /**
     * Method used by the components of this server to notify mining server
     * event listeners when an mining server event occurs.
     *
     * @param   notifier
     *          The notifier callback that will be invoked to fire the
     *          appropriate event on each listener.
     */
    public void notifyEventListeners(MiningServerEventNotifier notifier)
    {
        for (MiningServerEventListener listener : this.serverEventListeners)
        {
            notifier.notifyListener(listener);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation also notifies all listeners subscribed to the
     * {@link MiningServerEventListener#onClientConnecting(StratumTcpServerConnection)}
     * event about the new connection.</p>
     */
    @Override
    protected StratumTcpServerConnection createConnection(Socket connectionSocket)
    {
        final StratumTcpServerConnection connection = new MiningServerConnection(this, connectionSocket);

        this.notifyEventListeners(
            new MiningServerEventNotifier()
            {
                @Override
                public void notifyListener(MiningServerEventListener listener)
                {
                    listener.onClientConnecting(connection);
                }
            });

        return connection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ConnectionState getPostConnectState(StratumTcpServerConnection connection)
    {
        return new PendingAuthorizationOrSubscriptionState((MiningServerConnection)connection);
    }

    /**
     * FIXME: Remove Test code.
     */
    public static void main(String[] args)
    throws Exception
    {
        StratumMiningServer server = new StratumMiningServer();

        server.registerEventListener(
            new MiningServerEventAdapter()
            {
                @Override
                public MiningAuthorizeResponse onClientAuthenticating(StratumTcpServerConnection connection,
                                                                      MiningAuthorizeRequest request)
                {
                    return new MiningAuthorizeResponse(request, true);
                }

                @Override
                public void onClientConnecting(StratumTcpServerConnection connection)
                {
                    System.out.println("Client connected.");
                }

                @Override
                public MiningSubscribeResponse onClientSubscribing(StratumTcpServerConnection connection,
                                                                   MiningSubscribeRequest request)
                {
                    System.out.println("Worker subscribing work: " + request.toJson());

                    return new MiningSubscribeResponse(request, "abc123", new byte[] {'B', 'E', 'E', 'F'}, 2);
                }

                @Override
                public MiningResumeResponse onClientResumingSession(StratumTcpServerConnection connection,
                                                                    MiningResumeRequest request)
                {
                    System.out.println("Worker resuming session: " + request.toJson());

                    return new MiningResumeResponse(request, false);
                }

                @Override
                public MiningSubmitResponse onClientSubmittingWork(StratumTcpServerConnection connection,
                                                                   MiningSubmitRequest request)
                {
                    System.out.println("Worker submitting work: " + request.toJson());

                    return new MiningSubmitResponse(request, false);
                }
            });

        server.startListening(3333);
    }
}