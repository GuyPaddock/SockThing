package com.redbottledesign.bitcoin.pool.rpc.stratum.server;

import java.net.Socket;
import java.util.Set;

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
}
