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
import com.redbottledesign.bitcoin.rpc.stratum.message.ResponseMessage;
import com.redbottledesign.bitcoin.rpc.stratum.transport.ConnectionState;
import com.redbottledesign.bitcoin.rpc.stratum.transport.StatefulMessageTransport;

public class StratumTcpClient
extends StatefulMessageTransport
{
    private static final Logger LOGGER = LoggerFactory.getLogger(StratumTcpClient.class);

    private final OutputThread outThread;
    private final InputThread inThread;

    private ConnectionState postConnectState;
    private Socket socket;

    public StratumTcpClient()
    {
        this(null);
    }

    public StratumTcpClient(ConnectionState postConnectState)
    {
        super();

        this.postConnectState = postConnectState;

        this.outThread = new OutputThread();
        this.inThread  = new InputThread();
    }

    public void setPostConnectState(ConnectionState postConnectState)
    {
        this.postConnectState = postConnectState;
    }

    public boolean isOpen()
    {
        return !this.getSocket().isClosed();
    }

    public void connect(String address, int port)
    throws UnknownHostException, IOException
    {
        this.connect(Inet4Address.getByName(address), port);
    }

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

        this.socket = new Socket(address, port);

        this.setConnectionState(postConnectState);

        this.outThread.start();
        this.inThread.start();
    }

    @Override
    public void sendRequest(RequestMessage message)
    throws IOException
    {
        this.outThread.queueMessage(message);
    }

    @Override
    public void pollForResponses()
    throws IOException
    {
        /* No polling is needed with this transport. Messages are sent whenever
         * they are available.
         */
    }

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

    protected InputThread getInThread()
    {
        return this.inThread;
    }

    protected OutputThread getOutThread()
    {
        return this.outThread;
    }

    protected ConnectionState getPostConnectState()
    {
        return this.postConnectState;
    }

    protected Socket getSocket()
    {
        return this.socket;
    }

    protected void dispatchMessages(ConnectionState state, List<Message> messages)
    {
        for (Message message : messages)
        {
            if (message instanceof RequestMessage)
                state.processRequest((RequestMessage)message);

            else
                this.notifyResponseListenersResponseReceived((ResponseMessage)message);
        }
    }

    protected class OutputThread
    extends Thread
    {
        private final Logger LOGGER = LoggerFactory.getLogger(OutputThread.class);

        private static final int QUEUE_POLL_TIMEOUT_SECONDS = 30;

        private final LinkedBlockingQueue<Message> queue;

        public OutputThread()
        {
            this.setName(this.getClass().getSimpleName());
            this.setDaemon(true);

            this.queue = new LinkedBlockingQueue<Message>();
        }

        public void queueMessage(Message message)
        {
            this.queue.add(message);
        }

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

    public class InputThread
    extends Thread
    {
        private final Logger LOGGER = LoggerFactory.getLogger(InputThread.class);

        public InputThread()
        {
            this.setName(this.getClass().getSimpleName());
            this.setDaemon(true);
        }

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
                        ConnectionState   currentState = StratumTcpClient.this.getConnectionState();
                        MessageMarshaller marshaller   = currentState.getMarshaller();
                        List<Message>     messages;

                        if (LOGGER.isTraceEnabled())
                            LOGGER.trace("Stratum [in]: " + jsonLine);

                        messages = marshaller.marshalMessages(jsonLine);

                        StratumTcpClient.this.dispatchMessages(currentState, messages);
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