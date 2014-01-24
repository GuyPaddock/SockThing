package com.redbottledesign.bitcoin.rpc.stratum.transport.tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redbottledesign.bitcoin.rpc.stratum.transport.ConnectionState;

/**
 * <p>A TCP implementation of a Stratum server.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 */
public class StratumTcpServer
{
    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(StratumTcpServer.class);

    /**
     * The state that each connection will enter upon receiving a connection from a client.
     */
    private ConnectionState postConnectState;

    /**
     * The server socket.
     */
    private ServerSocket serverSocket;

    /**
     * Default constructor for {@link StratumTcpServer}.
     */
    public StratumTcpServer()
    {
        this(null);
    }

    /**
     * Constructor for {@link StratumTcpServer} that configures a new server
     * with the specified post-connection state.
     *
     * @param   postConnectState
     *          The state that each connection should enter upon receiving a
     *          connection from a client.
     */
    public StratumTcpServer(ConnectionState postConnectState)
    {
        this.postConnectState = postConnectState;
    }

    /**
     * Gets the state that each connection will enter upon receiving a
     * connection from a client.
     *
     * @return  The post-connection state.
     */
    public ConnectionState getPostConnectState()
    {
        return this.postConnectState;
    }

    /**
     * Sets the state that each connection should enter upon receiving a
     * connection from a client.
     *
     * @param   postConnectState
     *          The new post-connect state.
     */
    public void setPostConnectState(ConnectionState postConnectState)
    {
        if (this.isListening())
            throw new IllegalStateException("The post-connect state cannot be set once the server is listening.");

        this.postConnectState = postConnectState;
    }

    /**
     * Returns whether or not the server is listening for connections.
     *
     * @return  {@code true} if the server is listening; {@code false}
     *          otherwise.
     */
    public boolean isListening()
    {
        final ServerSocket  socket = this.getServerSocket();
        final boolean       result = (socket != null) && !socket.isClosed();

        if (LOGGER.isTraceEnabled())
            LOGGER.trace("isListening(): " + result);

        return result;
    }

    /**
     * Starts listening for connections on the specified port.
     *
     * @param   port
     *          The port on which to listen for connections.
     *
     * @throws  IOException
     *          If the socket cannot be opened.
     */
    public void startListening(int port)
    throws IOException
    {
        final ConnectionState postConnectState = this.getPostConnectState();

        if (postConnectState == null)
        {
            throw new IllegalStateException(
                "The post-connect state must be specified through the constructor or set with setPostConnectState() " +
                "before attempting to start listening.");
        }

        if (this.isListening())
            throw new IllegalStateException("The server is already listening for connections.");

        this.setServerSocket(new ServerSocket(port));

        while (this.isListening())
        {
            Socket connectionSocket = this.serverSocket.accept();

            new StratumTcpServerConnection(connectionSocket, postConnectState).open();
        }
    }

    /**
     * Stops the server from listening for additional connections, if it is currently listening.
     */
    public void stopListening()
    {
        if (this.isListening())
        {
            try
            {
                this.getServerSocket().close();
            }
    
            catch (IOException ex)
            {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Exception encountered while closing server socket: " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * Gets the server socket.
     *
     * @return  The server socket.
     */
    protected ServerSocket getServerSocket()
    {
        return this.serverSocket;
    }

    /**
     * Sets the server socket.
     *
     * @param   serverSocket
     *          The new server socket.
     */
    protected void setServerSocket(final ServerSocket serverSocket)
    {
        this.serverSocket = serverSocket;
    }
}