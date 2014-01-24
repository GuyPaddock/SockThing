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
class StratumTcpServerConnection
extends AbstractTcpMessageTransport
{
    /**
     * Constructor for {@link StratumTcpServerConnection} that initializes the
     * connection to wrap the specified connected server-side socket.
     *
     * @param   connectionSocket
     *          The server connection socket.
     *
     * @param   postConnectState
     *          The state that the connection should enter when the client
     *          connects.
     */
    public StratumTcpServerConnection(Socket connectionSocket, ConnectionState postConnectState)
    {
        super(postConnectState);

        if (connectionSocket == null)
            throw new IllegalArgumentException("connectionSocket cannot be null.");

        this.setSocket(connectionSocket);
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