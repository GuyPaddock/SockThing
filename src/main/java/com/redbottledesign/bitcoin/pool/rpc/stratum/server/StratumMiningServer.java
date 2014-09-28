package com.redbottledesign.bitcoin.pool.rpc.stratum.server;

import java.net.Socket;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(StratumMiningServer.class);

    /**
     * The set of mining server event listeners.
     */
    protected Set<MiningServerEventListener> serverEventListeners;

    /**
     * Default constructor for {@link StratumMiningServer}.
     */
    public StratumMiningServer()
    {
        this.serverEventListeners = new LinkedHashSet<>();
    }

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
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(
                String.format(
                    "Notifying all event listeners about event (%s): %s",
                    notifier.getClass().getName(),
                    notifier.toString()));
        }

        for (MiningServerEventListener listener : this.serverEventListeners)
        {
            notifier.notifyListener(listener);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected MiningServerConnection createConnection(Socket connectionSocket)
    {
        return new MiningServerConnection(this, connectionSocket);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation notifies all listeners subscribed to the
     * {@link MiningServerEventListener#onClientConnecting(StratumTcpServerConnection)}
     * event about the new connection.</p>
     *
     * @param   connection
     *          The connection being accepted.
     */
    @Override
    protected void acceptConnection(final StratumTcpServerConnection connection)
    {
        this.notifyEventListeners(
            new MiningServerEventNotifier()
            {
                @Override
                public void notifyListener(MiningServerEventListener listener)
                {
                    listener.onClientConnecting(connection);
                }
            });
    }
}