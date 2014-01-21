package com.redbottledesign.bitcoin.rpc.stratum.transport.tcp;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redbottledesign.bitcoin.rpc.stratum.message.Message;
import com.redbottledesign.bitcoin.rpc.stratum.message.MessageMarshaller;
import com.redbottledesign.bitcoin.rpc.stratum.message.RequestMessage;
import com.redbottledesign.bitcoin.rpc.stratum.transport.ConnectionState;
import com.redbottledesign.bitcoin.rpc.stratum.transport.StatefulMessageTransport;

/**
 * <p>A TCP implementation of a Stratum client.</p>
 *
 * <p>A client can only be used for a single connection; after a connection is
 * closed, it cannot be used to connect again.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 */
public class StratumTcpClient
extends StatefulMessageTransport
{
    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(StratumTcpClient.class);

    /**
     * The thread responsible for sending messages to the remote end.
     */
    private final OutputThread outThread;

    /**
     * The thread responsible for receiving messages from the remote end.
     */
    private final InputThread inThread;

    /**
     * The state that the connection will enter when this client connects.
     */
    private ConnectionState postConnectState;

    /**
     * The connection socket.
     */
    private Socket socket;

    /**
     * Default constructor for {@link StratumTcpClient}.
     */
    public StratumTcpClient()
    {
        this(null);
    }

    /**
     * Constructor for {@link StratumTcpClient} that configures a new client
     * with the specified post-connection state.
     *
     * @param   postConnectState
     *          The state that the connection should enter when the client
     *          connects.
     */
    public StratumTcpClient(ConnectionState postConnectState)
    {
        super();

        this.postConnectState = postConnectState;

        this.outThread = new OutputThread();
        this.inThread  = new InputThread();
    }

    /**
     * Sets the state that the connection should enter when the client
     * connects.
     *
     * @param   postConnectState
     *          The new post-connect state.
     */
    public void setPostConnectState(ConnectionState postConnectState)
    {
        if (this.isOpen())
            throw new IllegalStateException("The post-connect state cannot be set once the connection is open.");

        this.postConnectState = postConnectState;
    }

    /**
     * Returns whether or not the connection is open.
     *
     * @return  {@code true} if the connection is open; {@code false}
     *          otherwise.
     */
    public boolean isOpen()
    {
        return !this.getSocket().isClosed();
    }

    /**
     * Opens a socket to the specified address and port.
     *
     * @param   address
     *          The server address.
     *
     * @param   port
     *          The server port.
     *
     * @throws  UnknownHostException
     *          If the server address cannot be resolved.
     *
     * @throws  IOException
     *          If the connection to the server fails.
     */
    public void connect(String address, int port)
    throws UnknownHostException, IOException
    {
        this.connect(Inet4Address.getByName(address), port);
    }

    /**
     * Opens a socket to the specified address and port.
     *
     * @param   address
     *          The server address.
     *
     * @param   port
     *          The server port.
     *
     * @throws  UnknownHostException
     *          If the server address cannot be resolved.
     *
     * @throws  IOException
     *          If the connection to the server fails.
     */
    public void connect(InetAddress address, int port)
    throws IOException
    {
        ConnectionState postConnectState = this.getPostConnectState();

        if (postConnectState == null)
        {
            throw new IllegalStateException(
                "The post-connect state must be specified through the constructor or set with setPostConnectState() " +
                "before attempting to connect.");
        }

        if (this.isOpen())
            throw new IllegalStateException("The client is already connected.");

        this.socket = new Socket(address, port);

        this.setConnectionState(postConnectState);

        this.outThread.start();
        this.inThread.start();
    }

    /**
     * Queues the specified request to go out on the current connection.
     *
     * @throws  IllegalStateException
     *          If the client is not currently connected.
     */
    @Override
    public void sendRequest(RequestMessage message)
    throws IllegalStateException
    {
        if (!this.isOpen())
            throw new IllegalStateException("The client is not currently connected.");

        this.outThread.queueMessage(message);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Due to the direct nature of TCP, this method does nothing on this
     * transport. The remote end of the connection sends messages as soon as
     * they are ready, rather than relying on polling.</p>
     */
    @Override
    public void pollForMessages()
    {
        /* No polling is needed with this transport. Messages are sent whenever
         * they are available.
         */
    }

    /**
     * Closes the current connection, if it is open.
     */
    public void close()
    {
        if (this.isOpen())
        {
            try
            {
                this.socket.close();
            }

            catch (IOException ex)
            {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Exception encountered while closing socket: " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * Gets the thread responsible for receiving messages from the remote end.
     *
     * @return  The output thread.
     */
    protected OutputThread getOutThread()
    {
        return this.outThread;
    }

    /**
     * Gets the thread responsible for receiving messages from the remote end.
     *
     * @return  The input thread.
     */
    protected InputThread getInThread()
    {
        return this.inThread;
    }

    /**
     * Gets the state that the connection will enter when this client connects.
     *
     * @return  The post-connection state.
     */
    protected ConnectionState getPostConnectState()
    {
        return this.postConnectState;
    }

    /**
     * Gets the connection socket.
     *
     * @return  The connection socket.
     */
    protected Socket getSocket()
    {
        return this.socket;
    }

    /**
     * <p>The Stratum TCP client output thread.</p>
     *
     * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
     *
     * @author Guy Paddock (guy.paddock@redbottledesign.com)
     */
    protected class OutputThread
    extends Thread
    {
        /**
         * The logger for this thread.
         */
        private final Logger LOGGER = LoggerFactory.getLogger(OutputThread.class);

        /**
         * The number of seconds that the output queue will remain blocked
         * before checking the state of the client connection.
         */
        private static final int QUEUE_POLL_TIMEOUT_SECONDS = 30;

        /**
         * The queue of outgoing messages.
         */
        private final LinkedBlockingQueue<Message> queue;

        /**
         * Default constructor for {@link OutputThread}.
         */
        public OutputThread()
        {
            this.setName(this.getClass().getSimpleName());
            this.setDaemon(true);

            this.queue = new LinkedBlockingQueue<Message>();
        }

        /**
         * Queues the specified message to be sent out over the connection
         * socket.
         *
         * @param   message
         *          The message to queue.
         */
        public void queueMessage(Message message)
        {
            this.queue.add(message);
        }

        /**
         * Runs the output thread, continuously dispatching queued messages
         * until the TCP connection is closed.
         */
        @Override
        public void run()
        {
            try (PrintStream outputStream = new PrintStream(StratumTcpClient.this.getSocket().getOutputStream()))
            {
                while (StratumTcpClient.this.isOpen())
                {
                    /* Using poll rather than take so this thread will exit if
                     * the connection is closed.  Otherwise, it would wait
                     * forever on this queue.
                     */
                    Message nextMessage = this.queue.poll(QUEUE_POLL_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                    if (nextMessage != null)
                    {
                        synchronized (StratumTcpClient.this)
                        {
                            ConnectionState     currentState = StratumTcpClient.this.getConnectionState();
                            MessageMarshaller   marshaller   = currentState.getMarshaller();
                            String              jsonMessage  = marshaller.unmarshalMessage(nextMessage);

                            if (LOGGER.isTraceEnabled())
                                LOGGER.trace("Stratum [out]: " + jsonMessage);

                            outputStream.println(jsonMessage);
                            outputStream.flush();
                        }
                    }
                }
            }

            catch (Exception ex)
            {
                if (LOGGER.isErrorEnabled())
                {
                    LOGGER.error(
                        String.format(
                            "Error on connection: %s\n%s",
                            ex.getMessage(),
                            ExceptionUtils.getStackTrace(ex)));
                }
            }

            finally
            {
                StratumTcpClient.this.close();
            }
        }
    }

    /**
     * <p>The Stratum TCP client input thread.</p>
     *
     * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
     *
     * @author Guy Paddock (guy.paddock@redbottledesign.com)
     */
    public class InputThread
    extends Thread
    {
        /**
         * The logger for this thread.
         */
        private final Logger LOGGER = LoggerFactory.getLogger(InputThread.class);

        /**
         * Default constructor for {@link InputThread}.
         */
        public InputThread()
        {
            this.setName(this.getClass().getSimpleName());
            this.setDaemon(true);
        }

        /**
         * Runs the input thread, continuously dispatching incoming messages
         * until the TCP connection is closed.
         */
        @Override
        public void run()
        {
            try (Scanner scan = new Scanner(StratumTcpClient.this.getSocket().getInputStream()))
            {
                while (StratumTcpClient.this.isOpen())
                {
                    String jsonLine = scan.nextLine().trim();

                    if (jsonLine.isEmpty())
                    {
                        synchronized (StratumTcpClient.this)
                        {
                            ConnectionState   currentState = StratumTcpClient.this.getConnectionState();
                            MessageMarshaller marshaller   = currentState.getMarshaller();
                            List<Message>     messages;

                            if (LOGGER.isTraceEnabled())
                                LOGGER.trace("Stratum [in]: " + jsonLine);

                            messages = marshaller.marshalMessages(jsonLine);

                            StratumTcpClient.this.notifyMessageListeners(messages);
                        }
                    }
                }
            }

            catch (Exception ex)
            {
                if (LOGGER.isErrorEnabled())
                {
                    LOGGER.error(
                        String.format(
                            "Error on connection: %s\n%s",
                            ex.getMessage(),
                            ExceptionUtils.getStackTrace(ex)));
                }
            }

            finally
            {
                StratumTcpClient.this.close();
            }
        }
    }
}