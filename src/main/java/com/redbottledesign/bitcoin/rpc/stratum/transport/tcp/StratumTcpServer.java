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
public abstract class StratumTcpServer
{
    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(StratumTcpServer.class);

    /**
     * The server socket.
     */
    private ServerSocket serverSocket;

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
        if (this.isListening())
            throw new IllegalStateException("The server is already listening for connections.");

        this.setServerSocket(new ServerSocket(port));

        while (this.isListening())
        {
            final Socket                        connectionSocket = this.serverSocket.accept();
            final StratumTcpServerConnection    connection       = new StratumTcpServerConnection(connectionSocket);
            final ConnectionState               postConnectState = this.getPostConnectState(connection);

            if (postConnectState == null)
                throw new IllegalStateException("postConnectState cannot be null.");

            connection.setPostConnectState(postConnectState);
            connection.open();
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

    /**
     * Gets the state that the provided connection should enter upon being
     * opened.
     *
     * @param   connection
     *          The connection being opened.
     *
     * @return  The post-connection state.
     */
    protected abstract ConnectionState getPostConnectState(StratumTcpServerConnection connection);
}