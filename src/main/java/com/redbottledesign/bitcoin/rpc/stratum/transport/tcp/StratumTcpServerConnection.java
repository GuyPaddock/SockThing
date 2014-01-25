package com.redbottledesign.bitcoin.rpc.stratum.transport.tcp;

import java.net.Socket;

import com.redbottledesign.bitcoin.rpc.stratum.transport.ConnectionState;

/**
 * <p>A Stratum server connection over TCP.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 */
public class StratumTcpServerConnection
extends AbstractTcpMessageTransport
{
    /**
     * The server to which this connection corresponds.
     */
    protected final StratumTcpServer server;

    /**
     * Constructor for {@link StratumTcpServerConnection} that initializes the
     * connection to wrap the specified connected server-side socket and start
     * in the specified state.
     *
     * @param   server
     *          The server.
     *
     * @param   connectionSocket
     *          The server connection socket.
     */
    public StratumTcpServerConnection(StratumTcpServer server, Socket connectionSocket)
    {
        this(server, connectionSocket, null);
    }

    /**
     * Constructor for {@link StratumTcpServerConnection} that initializes the
     * connection to wrap the specified connected server-side socket and start
     * in the specified state.
     *
     * @param   server
     *          The server.
     *
     * @param   connectionSocket
     *          The server connection socket.
     *
     * @param   postConnectState
     *          The state that the connection should enter when the client
     *          connects.
     */
    public StratumTcpServerConnection(StratumTcpServer server, Socket connectionSocket,
                                      ConnectionState postConnectState)
    {
        super(postConnectState);

        if (connectionSocket == null)
            throw new IllegalArgumentException("connectionSocket cannot be null.");

        this.server = server;

        this.setSocket(connectionSocket);
    }

    /**
     * Gets the server to which this connection corresponds.
     *
     * @return  The server.
     */
    public StratumTcpServer getServer()
    {
        return this.server;
    }

    /**
     * Opens this connection and starts servicing it.
     */
    public void open()
    {
        ConnectionState postConnectState = this.getPostConnectState();

        if (this.isOpen())
            throw new IllegalStateException("The connection is already open.");

        this.setConnectionState(postConnectState);

        this.getOutputThread().start();
        this.getInputThread().start();
    }
}