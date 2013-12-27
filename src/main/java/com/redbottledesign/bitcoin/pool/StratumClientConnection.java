package com.redbottledesign.bitcoin.pool;
import java.io.PrintStream;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.StratumServer;
import com.google.bitcoin.core.Sha256Hash;

public class StratumClientConnection
{
    private static final Logger LOGGER = LoggerFactory.getLogger(StratumClientConnection.class);

    private final Socket sock;
    private final String connectionId;
    private final AtomicLong nextRequestId;

    private volatile boolean open;
    private volatile boolean isSubscribed;
    private volatile String subscriptionId;
    private volatile int extraNonce1;
    private volatile int extraNonce2Size;
    private volatile JSONObject blockTemplate;

    private final LinkedBlockingQueue<JSONObject> outQueue = new LinkedBlockingQueue<JSONObject>();

    public StratumClientConnection(StratumServer server, Socket sock, String connectionId)
    {
        this.sock           = sock;
        this.connectionId   = connectionId;
        this.open           = true;
        this.isSubscribed   = false;
        this.nextRequestId  = new AtomicLong(10000);

        new OutThread().start();
        new InThread().start();
    }

    public void close()
    {
        this.open = false;

        try
        {
            this.sock.close();
        }

        catch (Throwable t)
        {
            // Suppressed
        }
    }

    public long getNextRequestId()
    {
        return this.nextRequestId.getAndIncrement();
    }

    public void sendMessage(JSONObject msg)
    {
        try
        {
            this.outQueue.put(msg);
        }

        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }

    public class OutThread
    extends Thread
    {
        private final Logger LOGGER = LoggerFactory.getLogger(OutThread.class);

        public OutThread()
        {
            this.setName("OutThread");
            this.setDaemon(true);
        }

        @Override
        public void run()
        {
            try
            {
                PrintStream out = new PrintStream(sock.getOutputStream());

                while (open)
                {
                    //Using poll rather than take so this thread will
                    //exit if the connection is closed.  Otherwise,
                    //it would wait forever on this queue
                    JSONObject msg = outQueue.poll(30, TimeUnit.SECONDS);

                    if (msg != null)
                    {
                        String msg_str = msg.toString();

                        out.println(msg_str);
                        out.flush();

                        if (LOGGER.isTraceEnabled())
                            LOGGER.trace("Stratum [out]: " + msg.toString());
                    }
                }
            }

            catch (Exception ex)
            {
                if (LOGGER.isErrorEnabled())
                {
                    LOGGER.error(
                        String.format(
                            "Error on connection %d: %s\n%s",
                            connectionId,
                            ex.getMessage(),
                            ExceptionUtils.getStackTrace(ex)));
                }
            }

            finally
            {
                close();
            }
        }
    }

    public class InThread
    extends Thread
    {
        private final Logger LOGGER = LoggerFactory.getLogger(InThread.class);

        public InThread()
        {
            this.setName("InThread");
            this.setDaemon(true);
        }

        @Override
        public void run()
        {
            try (Scanner scan = new Scanner(sock.getInputStream()))
            {
                while (open)
                {
                    String line = scan.nextLine();

                    line = line.trim();

                    if (line.length() > 0)
                    {
                        JSONObject msg = new JSONObject(line);

                        if (LOGGER.isTraceEnabled())
                            LOGGER.trace("Stratum [in]: " + msg.toString());

                        StratumClientConnection.this.processInMessage(msg);
                    }
                }
            }

            catch (JSONException | NoSuchElementException ex)
            {
              // Suppress syntax errors from connecting clients
            }

            catch (Exception ex)
            {
                if (LOGGER.isErrorEnabled())
                {
                    LOGGER.error(
                        String.format(
                            "Error on connection %d: %s\n%s",
                            connectionId,
                            ex.getMessage(),
                            ExceptionUtils.getStackTrace(ex)));
                }
            }

            finally
            {
                close();
            }
        }
    }

    private void processInMessage(JSONObject msg)
    throws MalformedStratumMessageException, JSONException
    {
        Object id = msg.opt("id");

        if (!msg.has("result"))
        {
            if (LOGGER.isErrorEnabled())
                LOGGER.error("Unknown Stratum JSON message received: " + msg.toString());

            return;
        }

        else
        {
            if (!this.isSubscribed)
                this.handleSubscriptionNotify(msg);

            else
            {
                this.handleReceiveJob(msg);
//                sendMessage(reply);
            }
        }
    }

    protected void handleSubscriptionNotify(JSONObject msg)
    throws MalformedStratumMessageException, JSONException
    {
        JSONArray   result          = msg.getJSONArray("result"),
                    subscriptionKey;

        if (result.length() <= 3)
            throw new MalformedStratumMessageException(msg);

        subscriptionKey = result.getJSONArray(0);

        if ((subscriptionKey.length() <= 2) || !subscriptionKey.getString(0).equals("mining.notify"))
            throw new MalformedStratumMessageException(msg, "mining.notify");

        StratumClientConnection.this.subscriptionId     = subscriptionKey.getString(1);
        StratumClientConnection.this.extraNonce1        = result.getInt(1);
        StratumClientConnection.this.extraNonce2Size    = result.getInt(2);
        StratumClientConnection.this.isSubscribed       = true;
    }

    protected void handleReceiveJob(JSONObject msg)
    throws MalformedStratumMessageException, JSONException
    {
        JSONArray   result              = msg.getJSONArray("result");
        Sha256Hash  previousBlockHash;
        String      jobId,
                    coinbase1,
                    coinbase2,
                    blockVersion,
                    encodedDifficulty,
                    currentTime;
        boolean     cleanJob;
        JSONArray   merkleBranches;

        if (result.length() < 9)
            throw new MalformedStratumMessageException(msg, "work notification");

        jobId               = result.getString(0);
        previousBlockHash   = new Sha256Hash(result.getString(1));
        coinbase1           = result.getString(2);
        coinbase2           = result.getString(3);
        merkleBranches      = result.getJSONArray(4);
        blockVersion        = result.getString(5);
        encodedDifficulty   = result.getString(6);
        currentTime         = result.getString(7);
        cleanJob            = result.getBoolean(8);
    }
}