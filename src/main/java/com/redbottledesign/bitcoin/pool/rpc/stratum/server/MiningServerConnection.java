package com.redbottledesign.bitcoin.pool.rpc.stratum.server;

import java.net.Socket;

import com.redbottledesign.bitcoin.rpc.stratum.transport.tcp.StratumTcpServerConnection;

/**
 * <p>A Stratum mining server connection over TCP.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 */
public class MiningServerConnection
extends StratumTcpServerConnection
{
    /**
     * Constructor for {@link MiningServerConnection} that initializes the
     * connection to wrap the specified connected server-side socket and start
     * in the specified state.
     *
     * @param   server
     *          The server.
     *
     * @param   connectionSocket
     *          The server connection socket.
     */
    public MiningServerConnection(StratumMiningServer server, Socket connectionSocket)
    {
        super(server, connectionSocket);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This override narrows the type of object being returned to be of the
     * type {@link StratumMiningServer}, to eliminate unnecessary casts
     * elsewhere in this implementation.</p>
     */
    @Override
    public StratumMiningServer getServer()
    {
        return (StratumMiningServer)super.getServer();
    }
}
