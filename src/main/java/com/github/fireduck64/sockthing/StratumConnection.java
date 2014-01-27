package com.github.fireduck64.sockthing;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.util.HexUtil;
import com.redbottledesign.bitcoin.pool.agent.persistence.PersistenceAgent;
import com.redbottledesign.bitcoin.pool.rpc.bitcoin.BlockTemplate;
import com.redbottledesign.bitcoin.pool.rpc.bitcoin.Coinbase;
import com.redbottledesign.bitcoin.pool.rpc.bitcoin.CoinbaseFactory;
import com.redbottledesign.bitcoin.pool.util.queue.EvictableQueue;

public class StratumConnection
{
    private static final Logger LOGGER = LoggerFactory.getLogger(StratumConnection.class);

    private static final String CONFIG_VALUE_POOL_CONTROL_PASSWORD = "pool_control_password";

    /** At least for now the job info is held in memory
     * so generate a new session id on JVM restart since all the jobs
     * from the old one are certainly gone.  Also, helps with switching nodes.
     * This way, we reject resumes from other runs.
     */
    public static final String RUNTIME_SESSION=HexUtil.sha256("" + new Random().nextLong());

    private final StratumServer server;
    private final Socket sock;
    private final String connection_id;
    private final AtomicLong last_network_action;
    private volatile boolean open;
    private volatile boolean mining_subscribe=false;
    private PoolUser user;

    private UserSessionData user_session_data;

    private final AtomicLong next_request_id=new AtomicLong(10000);

    private final LinkedBlockingQueue<JSONObject> out_queue = new LinkedBlockingQueue<JSONObject>();

    private long get_client_id=-1;
    private String client_version;

    private Coinbase coinbase;

    public StratumConnection(StratumServer server, Socket sock, String connection_id)
    {
        this.server              = server;
        this.sock                = sock;
        this.connection_id       = connection_id;
        this.open                = true;
        this.last_network_action = new AtomicLong(System.nanoTime());

        this.refreshCoinbase();

        new OutThread().start();
        new InThread().start();
    }

    public void close()
    {
        open = false;

        try
        {
            sock.close();
        }

        catch (Throwable t)
        {
            // Suppressed
        }
    }

    public long getLastNetworkAction()
    {
        return last_network_action.get();
    }

    public long getNextRequestId()
    {
        return next_request_id.getAndIncrement();
    }

    public void sendMessage(JSONObject msg)
    {
        try
        {
            out_queue.put(msg);
        }

        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }


    public void sendRealJob(BlockTemplate blockTemplate, boolean clean)
    throws Exception
    {
        String      jobId;
        JobInfo     jobInfo;
        JSONObject  miningNotifyMessage;

        if (this.user_session_data == null)
            return;

        if (!this.mining_subscribe)
            return;

        if (clean)
            this.user_session_data.clearAllJobs();

        this.refreshCoinbase();

        jobId   = this.user_session_data.getNextJobId();
        jobInfo = new JobInfo(this.server, this.user, jobId, blockTemplate, this.coinbase);

        this.user_session_data.saveJobInfo(jobId, jobInfo);

        miningNotifyMessage = jobInfo.getMiningNotifyMessage(clean);

        this.sendMessage(miningNotifyMessage);
    }

    protected void updateLastNetworkAction()
    {
        this.last_network_action.set(System.nanoTime());
    }

    protected void refreshCoinbase()
    {
        // Get from user session for now.  Might do something fancy with resume later.
        byte[]          userNonce = UserSessionData.getExtranonce1();
        BlockTemplate   blockTemplate;

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Refreshing Stratum coinbase.");

        try
        {
            blockTemplate = server.getCurrentBlockTemplate();
        }

        catch (IOException | JSONException ex)
        {
            throw new RuntimeException("Failed to obtain current block template: " + ex.getMessage(), ex);
        }

        this.coinbase =
            CoinbaseFactory.getInstance().generateCoinbase(
                server,
                blockTemplate,
                userNonce);
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
                    JSONObject msg = out_queue.poll(30, TimeUnit.SECONDS);

                    if (msg != null)
                    {
                        String msg_str = msg.toString();

                        out.println(msg_str);
                        out.flush();

                        if (LOGGER.isTraceEnabled())
                            LOGGER.trace("Stratum [out]: " + msg.toString());

                        updateLastNetworkAction();
                    }
                }
            }

            catch (Exception ex)
            {
                if (LOGGER.isErrorEnabled())
                {
                    LOGGER.error(
                        String.format(
                            "Error on connection '%s': %s\n%s",
                            connection_id,
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

                    StratumConnection.this.updateLastNetworkAction();

                    line = line.trim();

                    if (line.length() > 0)
                    {
                        JSONObject msg = new JSONObject(line);

                        if (LOGGER.isTraceEnabled())
                            LOGGER.trace("Stratum [in]: " + msg.toString());

                        processInMessage(msg);
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
                            "Error on connection '%s': %s",
                            connection_id,
                            ex.getMessage()),
                        ex);
                }
            }

            finally
            {
                close();
            }
        }
    }

    private void processInMessage(JSONObject msg)
    throws Exception
    {
        long idx = msg.optLong("id", -1);

        if (idx != -1 && idx == get_client_id && msg.has("result"))
        {
            client_version = msg.getString("result");
            return;
        }

        Object id = msg.opt("id");

        if (!msg.has("method"))
        {
            if (LOGGER.isErrorEnabled())
                LOGGER.error("Unknown Stratum JSON message received: " + msg.toString());

            return;
        }

        String method = msg.getString("method");

        if (method.equals("mining.subscribe"))
        {
            JSONObject reply = new JSONObject();
            reply.put("id", id);
            reply.put("error", JSONObject.NULL);
            JSONArray lst2 = new JSONArray();
            lst2.put("mining.notify");
            lst2.put("hhtt");
            JSONArray lst = new JSONArray();
            lst.put(lst2);
            lst.put(Hex.encodeHexString(this.coinbase.getExtraNonce1()));
            lst.put(this.coinbase.getExtraNonce1Length());
            lst.put(RUNTIME_SESSION);
            reply.put("result", lst);

            sendMessage(reply);
            mining_subscribe=true;
        }
        else if (method.equals("mining.authorize"))
        {
            JSONArray params = msg.getJSONArray("params");
            String username = (String)params.get(0);
            String password = (String)params.get(1);

            PoolUser pu = server.getAuthHandler().authenticate(username, password);

            JSONObject reply = new JSONObject();
            reply.put("id", id);

            if (pu==null)
            {
                reply.put("error", "unknown user");
                reply.put("result", false);
                sendMessage(reply);
            }
            else
            {
                reply.put("result", true);
                reply.put("error", JSONObject.NULL);
                //reply.put("difficulty", pu.getDifficulty());
                //reply.put("user", pu.getName());
                user = pu;

                sendMessage(reply);
                sendDifficulty();
                sendGetClient();

                user_session_data = server.getUserSessionData(pu);

                sendRealJob(server.getCurrentBlockTemplate(), false);
            }

        }
        else if (method.equals("mining.resume"))
        {
            JSONArray params = msg.getJSONArray("params");
            String session_id = params.getString(0);

            JSONObject reply = new JSONObject();
            reply.put("id", id);

            // should be the same as mining.subscribe
            if (!session_id.equals(RUNTIME_SESSION))
            {
                reply.put("error", "bad session id");
                reply.put("result", false);

                sendMessage(reply);
            }
            else
            {
                reply.put("result", true);
                reply.put("error", JSONObject.NULL);

                sendMessage(reply);

                mining_subscribe = true;
            }
        }
        else if (method.equals("mining.submit"))
        {
            JSONArray params = msg.getJSONArray("params");

            String job_id = params.getString(1);

            JobInfo ji = user_session_data.getJobInfo(job_id);

            if (ji == null)
            {
                JSONObject reply = new JSONObject();
                reply.put("id", id);
                reply.put("result", false);
                reply.put("error", "unknown-work");
                sendMessage(reply);
            }
            else
            {
                SubmitResult res = new SubmitResult();
                res.setClientVersion(client_version);

                ji.validateSubmit(params,res);
                JSONObject reply = new JSONObject();
                reply.put("id", id);

                if (res.getOurResult().equals("Y"))
                {
                    reply.put("result", true);
                }

                else
                {
                    reply.put("result", false);
                }

                if (res.getReason()==null)
                {
                    reply.put("error", JSONObject.NULL);
                }
                else
                {
                    reply.put("error", res.getReason());
                }

                sendMessage(reply);

                if ((res !=null) &&
                    (((res.getReason() != null) && (res.getReason().equals("H-not-zero"))) ||
                     (res.shouldSendDifficulty())))
                {
                    // Time to remind user about their minimum difficulty
                    sendDifficulty();
                }
            }
        }
        else if (method.equals("mining.pool.stop"))
        {
            JSONArray   params          = msg.getJSONArray("params");
            JSONObject  reply           = new JSONObject();
            String      passwordParam   = params.get(0).toString();

            reply.put("id", id);

            if (!passwordParam.equals(this.server.getConfig().get(CONFIG_VALUE_POOL_CONTROL_PASSWORD)))
            {
                reply.put("error", "incorrect password");
                reply.put("result", false);
            }

            else
            {
                reply.put("result", "true");
                reply.put("error", JSONObject.NULL);

                this.server.stop();
            }

            this.sendMessage(reply);
        }
        else if (method.equals("mining.pool.queue.persistence.evict"))
        {
            JSONArray   params            = msg.getJSONArray("params");
            JSONObject  reply             = new JSONObject();
            String      passwordParam     = params.get(0).toString();
            String      queueItemIdParam  = params.get(1).toString();
            Long        queueItemId       = null;

            reply.put("id", id);

            if (!passwordParam.equals(this.server.getConfig().get(CONFIG_VALUE_POOL_CONTROL_PASSWORD)))
            {
                reply.put("error", "incorrect password");
                reply.put("result", false);
            }

            else
            {
                try
                {
                    queueItemId = Long.parseLong(queueItemIdParam);
                }

                catch (NumberFormatException ex)
                {
                    reply.put("error", "not a queue item ID: " + queueItemIdParam);
                    reply.put("result", false);
                }

                if (queueItemId != null)
                {
                    boolean evicted = this.server.getAgent(PersistenceAgent.class).evictQueueItem(queueItemId);

                    reply.put("result", evicted);
                    reply.put("error", JSONObject.NULL);
                }
            }

            this.sendMessage(reply);
        }
        else if (method.equals("mining.pool.queue.persistence.evict-all"))
        {
            JSONArray   params          = msg.getJSONArray("params");
            JSONObject  reply           = new JSONObject();
            String      passwordParam   = (String) params.get(0);

            reply.put("id", id);

            if (!passwordParam.equals(this.server.getConfig().get(CONFIG_VALUE_POOL_CONTROL_PASSWORD)))
            {
                reply.put("error", "incorrect password");
                reply.put("result", false);
            }

            else
            {
                boolean evicted = this.server.getAgent(PersistenceAgent.class).evictAllQueueItems();

                reply.put("result", evicted);
                reply.put("error", JSONObject.NULL);
            }

            this.sendMessage(reply);
        }
        else if (method.equals("mining.pool.queue.pplns.evict"))
        {
            JSONArray   params              = msg.getJSONArray("params");
            JSONObject  reply               = new JSONObject();
            String      passwordParam       = params.get(0).toString();
            String      queueItemIdParam    = params.get(1).toString();
            PplnsAgent  pplnsAgent          = this.server.getAgent(PplnsAgent.class);

            reply.put("id", id);

            if (!passwordParam.equals(this.server.getConfig().get(CONFIG_VALUE_POOL_CONTROL_PASSWORD)))
            {
                reply.put("error", "incorrect password");
                reply.put("result", false);
            }

            else if (!(pplnsAgent instanceof EvictableQueue))
            {
                reply.put("error", "PPLNS agent does not support eviction");
                reply.put("result", false);
            }

            else
            {
                if ((queueItemIdParam != null) && !queueItemIdParam.isEmpty())
                {
                    @SuppressWarnings("unchecked")
                    boolean evicted = ((EvictableQueue<String>)pplnsAgent).evictQueueItem(queueItemIdParam);

                    reply.put("result", evicted);
                    reply.put("error", JSONObject.NULL);
                }
            }

            this.sendMessage(reply);
        }
        else if (method.equals("mining.pool.queue.pplns.evict-all"))
        {
            JSONArray   params          = msg.getJSONArray("params");
            JSONObject  reply           = new JSONObject();
            String      passwordParam   = (String) params.get(0);
            PplnsAgent  pplnsAgent      = this.server.getAgent(PplnsAgent.class);

            reply.put("id", id);

            if (!passwordParam.equals(this.server.getConfig().get(CONFIG_VALUE_POOL_CONTROL_PASSWORD)))
            {
                reply.put("error", "incorrect password");
                reply.put("result", false);
            }

            else if (!(pplnsAgent instanceof EvictableQueue))
            {
                reply.put("error", "PPLNS agent does not support eviction");
                reply.put("result", false);
            }

            else
            {
                @SuppressWarnings("unchecked")
                boolean evicted = ((EvictableQueue<String>)pplnsAgent).evictAllQueueItems();

                reply.put("result", evicted);
                reply.put("error", JSONObject.NULL);
            }

            this.sendMessage(reply);
        }
    }

    private void sendDifficulty()
    throws Exception
    {
        JSONObject msg = new JSONObject();
        msg.put("id", JSONObject.NULL);
        msg.put("method","mining.set_difficulty");

        JSONArray lst = new JSONArray();
        lst.put(user.getDifficulty());
        msg.put("params", lst);

        sendMessage(msg);
    }

    private void sendGetClient()
    throws Exception
    {
        long id = getNextRequestId();

        get_client_id = id;

        JSONObject msg = new JSONObject();
        msg.put("id", id);
        msg.put("method","client.get_version");

        sendMessage(msg);
    }
}