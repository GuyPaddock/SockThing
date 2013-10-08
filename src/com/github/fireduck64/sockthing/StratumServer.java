package com.github.fireduck64.sockthing;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

import com.github.fireduck64.sockthing.authentication.AuthHandler;
import com.github.fireduck64.sockthing.output.OutputMonster;
import com.github.fireduck64.sockthing.output.OutputMonsterSimple;
import com.github.fireduck64.sockthing.sharesaver.ShareSaver;
import com.github.fireduck64.sockthing.util.HexUtil;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.NetworkParameters;
import com.redbottledesign.bitcoin.pool.FallbackShareSaver;
import com.redbottledesign.bitcoin.pool.PayoutAgent;
import com.redbottledesign.bitcoin.pool.PersistenceAgent;
import com.redbottledesign.bitcoin.pool.RoundAgent;
import com.redbottledesign.bitcoin.pool.drupal.DrupalPplnsAgent;
import com.redbottledesign.bitcoin.pool.drupal.DrupalSession;
import com.redbottledesign.bitcoin.pool.drupal.DrupalShareSaver;
import com.redbottledesign.bitcoin.pool.drupal.authentication.DrupalAuthHandler;

public class StratumServer
{
    private static final String CONFIG_VALUE_POOL_PAYMENT_ADDRESS = "pay_to_address";
    private static final long MAX_IDLE_TIME = 300L * 1000L * 1000L * 1000L;//5 minutes in nanos

    private final BitcoinRPC bitcoinRpc;
    //private long max_idle_time = 3L * 1000L * 1000L * 1000L;//3 seconds

    private final Map<String, StratumConnection> conn_map=new HashMap<String, StratumConnection>(1024, 0.5f);

    private final Config config;
    private AuthHandler authHandler;
    private NetworkParameters networkParams;
    private Address poolAddress;
    private ShareSaver shareSaver;
    private OutputMonster outputMonster;
    private MetricsReporter metricsReporter;
    private DrupalSession session;
    private PersistenceAgent persistenceAgent;
    private WittyRemarksAgent wittyRemarksAgent;
    private PplnsAgent pplnsAgent;
    private RoundAgent roundAgent;
    private PayoutAgent payoutAgent;
    private EventLog eventLog;

    private String instanceId;

    /**
     * Nothing should read this, anything interested should call getCurrentBlockTemplate() instead
     */
    private JSONObject cachedBlockTemplate;

    private final Map<String, UserSessionData> userSessionDataMap=new HashMap<String, UserSessionData>(1024, 0.5f);

    private volatile int currentBlock;
    private volatile long currentBlockUpdateTime;

    private final Semaphore newBlockNotifyObject;

    private volatile double blockDifficulty;

    private volatile long blockReward;
    private final StratumServer server;
    private final Object blockTemplateLock;

    public StratumServer(Config config)
    {
        this.newBlockNotifyObject = new Semaphore(0);
        this.blockTemplateLock = new Object();
        this.config = config;

        config.require("port");

        this.bitcoinRpc = new BitcoinRPC(config);
        this.server = this;
    }

    public void start()
    {
        String poolAddressString;

        this.getEventLog().log("SERVER START");

        poolAddressString = this.config.get(CONFIG_VALUE_POOL_PAYMENT_ADDRESS);

        try
        {
          this.poolAddress = new Address(this.networkParams, poolAddressString);
        }

        catch (AddressFormatException ex)
        {
          throw new RuntimeException(
            String.format("Bad pool 'pay to' address '%s': %s", poolAddressString, ex.getMessage()),
            ex);
        }

        new NotifyListenerUDP(this).start();
        new TimeoutThread().start();
        new NewBlockThread().start();
        new PruneThread().start();

        List<String> ports = this.config.getList("port");

        for (String s : ports)
        {
            int port = Integer.parseInt(s);
            new ListenThread(port).start();
        }

        if (this.persistenceAgent != null)
          this.persistenceAgent.start();

        if (this.wittyRemarksAgent != null)
          this.wittyRemarksAgent.start();

        if (this.pplnsAgent != null)
          this.pplnsAgent.start();

        if (this.roundAgent != null)
          this.roundAgent.start();

        if (this.payoutAgent != null)
          this.payoutAgent.start();
    }

    public void setAuthHandler(AuthHandler authHandler)
    {
        this.authHandler = authHandler;
    }

    public AuthHandler getAuthHandler()
    {
        return this.authHandler;
    }

    public void setMetricsReporter(MetricsReporter mr)
    {
        this.metricsReporter = mr;
    }

    public MetricsReporter getMetricsReporter()
    {
        return this.metricsReporter;
    }

    public Config getConfig()
    {
        return this.config;
    }

    public Double getBlockDifficulty()
    {
        return this.blockDifficulty;
    }

    public Long getBlockReward()
    {
        return this.blockReward;
    }

    public String getInstanceId()
    {
        return this.instanceId;
    }

    public void setInstanceId(String instanceId)
    {
        this.instanceId = instanceId;
    }

    public DrupalSession getSession()
    {
      return this.session;
    }

    public void setSession(DrupalSession session)
    {
      this.session = session;
    }

    public PersistenceAgent getPersistenceAgent()
    {
      return this.persistenceAgent;
    }

    public void setPersistenceAgent(PersistenceAgent persistenceAgent)
    {
      this.persistenceAgent = persistenceAgent;
    }

    public void setShareSaver(ShareSaver shareSaver)
    {
        this.shareSaver = shareSaver;
    }

    public ShareSaver getShareSaver()
    {
        return this.shareSaver;
    }

    public void setOutputMonster(OutputMonster output_monster)
    {
        this.outputMonster = output_monster;
    }
    public OutputMonster getOutputMonster()
    {
        return this.outputMonster;
    }

    public void setWittyRemarksAgent(WittyRemarksAgent remarks)
    {
        this.wittyRemarksAgent = remarks;
    }

    public WittyRemarksAgent getWittyRemarksAgent()
    {
        return this.wittyRemarksAgent;
    }

    public void setPplnsAgent(PplnsAgent pplnsAgent)
    {
        this.pplnsAgent = pplnsAgent;
    }

    public PplnsAgent getPplnsAgent()
    {
        return this.pplnsAgent;
    }

    public PayoutAgent getPayoutAgent()
    {
      return this.payoutAgent;
    }

    public void setPayoutAgent(PayoutAgent payoutAgent)
    {
      this.payoutAgent = payoutAgent;
    }

    public void setEventLog(EventLog eventLog)
    {
      this.eventLog = eventLog;
    }

    public EventLog getEventLog()
    {
      return eventLog;
    }

    public void setRoundAgent(RoundAgent roundAgent)
    {
      this.roundAgent = roundAgent;
    }

    public RoundAgent getRoundAgent()
    {
      return this.roundAgent;
    }

    public NetworkParameters getNetworkParameters()
    {
      return this.networkParams;
    }

    public void setNetworkParameters(NetworkParameters networkParams)
    {
      this.networkParams = networkParams;
    }

    public static void main(String args[])
    throws Exception
    {
        if (args.length != 1)
        {
            System.out.println("Expected exactly one argument, a config file");
            System.out.println("java -jar SockThing.jar pool.cfg");
            return;
        }

        Config conf = new Config(args[0]);

        conf.require("pay_to_address");
        conf.require("network");
        conf.require("instance_id");
        conf.require("coinbase_text");
        conf.require("saver_messaging_enabled");
        conf.require("witty_remarks_enabled");

        StratumServer server = new StratumServer(conf);

        server.setEventLog(new EventLog(conf));
        server.setInstanceId(conf.get("instance_id"));
        server.setMetricsReporter(new MetricsReporter(server));

        server.setSession(new DrupalSession(conf));
        server.setAuthHandler(new DrupalAuthHandler(server));

        server.setPersistenceAgent(new PersistenceAgent(server));

//        if (conf.getBoolean("saver_messaging_enabled"))
//        {
//            server.setShareSaver(new ShareSaverMessaging(server, new DBShareSaver(conf)));
//        }
//        else
//        {
//            server.setShareSaver(new DBShareSaver(conf));
//        }

        server.setShareSaver(
          new FallbackShareSaver(conf, server, new DrupalShareSaver(conf, server)));

        String network = conf.get("network");
        if (network.equals("prodnet"))
        {
            server.setNetworkParameters(NetworkParameters.prodNet());
        }
        else if (network.equals("testnet"))
        {
            server.setNetworkParameters(NetworkParameters.testNet3());
        }

//        server.setOutputMonster(new OutputMonsterShareFees(conf, server.getNetworkParameters()));

        // Fee sharing is done elsewhere.
        server.setOutputMonster(new OutputMonsterSimple(conf, server.getNetworkParameters()));
        server.setPayoutAgent(new PayoutAgent(server));

        if (conf.getBoolean("witty_remarks_enabled"))
        {
            server.setWittyRemarksAgent(new WittyRemarksAgent());
        }

        server.setPplnsAgent(new DrupalPplnsAgent(server));
        server.setRoundAgent(new RoundAgent(server));

        server.start();
    }
    /**
     * 0 - not stale (current)
     * 1 - slightly stale
     * 2 - really stale
     */
    public int checkStale(int nextBlock)
    {
        if (nextBlock == this.currentBlock + 1)
        {
            return 0;
        }

        if (nextBlock == this.currentBlock)
        {
            if (this.currentBlockUpdateTime + 10000 > System.currentTimeMillis())
                return 1;
        }

        return 2;
    }

    public void notifyNewBlock()
    {
        this.newBlockNotifyObject.release(1);
    }

    public JSONObject getCurrentBlockTemplate()
        throws java.io.IOException, org.json.JSONException
    {
        JSONObject c = this.cachedBlockTemplate;

        if (c != null)
          return c;

        synchronized(this.blockTemplateLock)
        {
          JSONObject post;

          c = this.cachedBlockTemplate;

          if (c != null)
            return c;

          post  = new JSONObject(this.bitcoinRpc.getSimplePostRequest("getblocktemplate"));
          c     = this.bitcoinRpc.sendPost(post).getJSONObject("result");

          this.cachedBlockTemplate = c;

          getEventLog().log("new block template: " + c.getLong("height"));
          getMetricsReporter().metricCount("getblocktemplate", 1.0);

          return c;
        }
    }

    public double getDifficulty()
        throws java.io.IOException, org.json.JSONException
    {
        JSONObject post = new JSONObject(this.bitcoinRpc.getSimplePostRequest("getdifficulty"));

        return this.bitcoinRpc.sendPost(post).getDouble("result");
    }

    public String submitBlock(Block blk)
    {
        String result="N";

        for (int i=0; i<10; i++)
        {
            System.out.println("Attempting block submit");
            result = submitBlockAttempt(blk);

            if (result.equals("Y"))
              return result;
        }

        return result;
    }

    public String sendPayment(BigDecimal amount, Address payee)
    throws IOException, JSONException
    {
      JSONObject  resultObject = this.bitcoinRpc.sendPayment(amount.doubleValue(), this.poolAddress, payee);
      String      paymentHash;

      if (!resultObject.isNull("error"))
        throw new RuntimeException("Payment failed: " + resultObject.get("error"));

      paymentHash = resultObject.getString("result");

      return paymentHash;
    }

    public UserSessionData getUserSessionData(PoolUser pu)
    {
        synchronized(userSessionDataMap)
        {
            UserSessionData ud = userSessionDataMap.get(pu.getWorkerName());

            if (ud == null)
              ud = new UserSessionData(this);

            userSessionDataMap.put(pu.getWorkerName(), ud);

            return ud;
        }
    }

    private void updateBlockReward()
    throws Exception
    {
      this.blockReward = this.getCurrentBlockTemplate().getLong("coinbasevalue");
    }

    private void updateBlockDifficulty()
        throws Exception
    {
        String hexString = "0x" + getCurrentBlockTemplate().getString("bits");
        Long hexInt = Long.decode(hexString).longValue();

        this.blockDifficulty = HexUtil.difficultyFromHex(hexInt);
    }

    private void triggerUpdate(boolean clean)
        throws Exception
    {
        getEventLog().log("Update triggered. Clean: " + clean);
        System.out.println("Update triggered. Clean: " + clean);

        this.cachedBlockTemplate = null;

        long t1_get_block = System.currentTimeMillis();
        JSONObject block_template = getCurrentBlockTemplate();
        long t2_get_block = System.currentTimeMillis();

        getMetricsReporter().metricTime("GetBlockTemplateTime", t2_get_block - t1_get_block);

        if (clean)
        {
            // Needs the new block template cached before we update.
            updateBlockReward();
            updateBlockDifficulty();
        }

        long t1_update_connection = System.currentTimeMillis();

        LinkedList<Map.Entry<String, StratumConnection> > lst= new LinkedList<Map.Entry<String, StratumConnection> >();

        synchronized(this.conn_map)
        {
            lst.addAll(this.conn_map.entrySet());
        }

        for(Map.Entry<String, StratumConnection> me : lst)
        {
            me.getValue().sendRealJob(block_template, clean);
        }

        long t2_update_connection = System.currentTimeMillis();
        getEventLog().log("Update Complete");

        getMetricsReporter().metricTime("UpdateConnectionsTime", t2_update_connection - t1_update_connection);
    }


    private String submitBlockAttempt(Block blk)
    {
        try
        {
            JSONObject result = this.bitcoinRpc.submitBlock(blk);

            System.out.println("Block result: " + result.toString(2));

            if (result.isNull("error") && result.isNull("result"))
            {
                return "Y";
            }
            else
            {
                System.out.println("Block submit error:  "+ result.get("error"));
                return "N";
            }

        }
        catch(Throwable t)
        {
            t.printStackTrace();
            return "N";
        }

    }

    protected class ListenThread extends Thread
    {
        private final int port;
        public ListenThread(int port)
        {
            this.port = port;
            setName("Listen:"+port);
        }


        @Override
        public void run()
        {
            System.out.println("Listening on port: " + port);

            try
            {
                ServerSocket ss = new ServerSocket(port, 256);
                ss.setReuseAddress(true);


                while(ss.isBound())
                {
                    try
                    {
                        Socket sock = ss.accept();
                        sock.setTcpNoDelay(true);

                        String id = UUID.randomUUID().toString();

                        StratumConnection conn = new StratumConnection(server, sock, id);
                        synchronized(conn_map)
                        {
                            conn_map.put(id, conn);
                        }
                    }
                    catch(Throwable t)
                    {
                        t.printStackTrace();
                    }

                }
            }
            catch(java.io.IOException e)
            {
                throw new RuntimeException(e);
            }

        }
    }

    protected class TimeoutThread extends Thread
    {
        public TimeoutThread()
        {
            setName("TimeoutThread");
            setDaemon(true);
        }

        @Override
        public void run()
        {
            while(true)
            {
                LinkedList<Map.Entry<String, StratumConnection> > lst= new LinkedList<Map.Entry<String, StratumConnection> >();

                synchronized(conn_map)
                {
                    lst.addAll(conn_map.entrySet());
                }

                getMetricsReporter().metricCount("connections", lst.size());

                for (Map.Entry<String, StratumConnection> me : lst)
                {
                    if (me.getValue().getLastNetworkAction() + MAX_IDLE_TIME < System.nanoTime())
                    {
                        System.out.println("Closing connection due to inactivity: " + me.getKey());
                        me.getValue().close();
                        synchronized(conn_map)
                        {
                            conn_map.remove(me.getKey());
                        }
                    }
                }

                try
                {
                  Thread.sleep(30000);
                }

                catch (InterruptedException ex)
                {
                  // Expected
                }
            }

        }

    }

    /**
     * Prunes jobs out of user_session_data_map
     */
    protected class PruneThread extends Thread
    {
        public PruneThread()
        {
            setName("PruneThread");
            setDaemon(true);

        }
        @Override
        public void run()
        {
            while(true)
            {
                try
                {
                    Thread.sleep(43000);
                    doRun();
                }
                catch(Throwable t)
                {
                    t.printStackTrace();
                }
            }

        }

        private void doRun()
            throws Exception
        {
            TreeSet<String> to_delete = new TreeSet<String>();
            int user_sessions=0;
            int user_jobs=0;
            synchronized(userSessionDataMap)
            {
                user_sessions = userSessionDataMap.size();

                for(Map.Entry<String, UserSessionData> me : userSessionDataMap.entrySet())
                {
                    user_jobs += me.getValue().getJobCount();

                    if (me.getValue().prune())
                    {
                        to_delete.add(me.getKey());
                    }
                }

                for(String id : to_delete)
                {
                    userSessionDataMap.remove(id);
                }


            }

            metricsReporter.metricCount("usersessions", user_sessions);
            metricsReporter.metricCount("userjobs", user_jobs);

        }
    }

    public class NewBlockThread extends Thread
    {
        int last_block;
        long last_update_time;
        long last_success_time;

        public static final long MAX_TIME_WITHOUT_SUCCESS=2L*60L*1000L; //2 minutes
        public static final long TEMPLATE_REFRESH_TIME=30L * 1000L; //30 seconds

        public NewBlockThread()
        {
            setDaemon(true);
            setName("NewBlockThread");
            last_update_time=System.currentTimeMillis();
            last_success_time=System.currentTimeMillis();

        }

        @Override
        public void run()
        {
            while(true)
            {
                try
                {
                    if (last_success_time + MAX_TIME_WITHOUT_SUCCESS < System.currentTimeMillis())
                    {
                        System.out.println("MAX_TIME_WITHOUT_SUCCESS EXCEEDED.  Giving up.  Failure.");
                        System.exit(-1);
                    }
                    if (newBlockNotifyObject.tryAcquire(1, 1000, TimeUnit.MILLISECONDS))
                    {
                        System.out.println("New block notify");
                    }
                    doRun();

                }
                catch(Throwable t)
                {
                    t.printStackTrace();
                }

            }

        }

        private void doRun()
        throws Exception
        {
            JSONObject reply = bitcoinRpc.doSimplePostRequest("getblockcount");

            int block_height = reply.getInt("result");

            if (block_height != last_block)
            {
                /* Using target high (next block height)
                 * for logging because that is what the block template,
                 * submitted shares and next found blocks all use.
                 */
                int target_height = block_height+1;
                getEventLog().log("New target height: " + target_height);

                System.out.println(reply);
                triggerUpdate(true);

                last_block = block_height;
                last_update_time = System.currentTimeMillis();
                last_success_time = System.currentTimeMillis();

                currentBlockUpdateTime = System.currentTimeMillis();
                currentBlock = block_height;

            }

            if (last_update_time + TEMPLATE_REFRESH_TIME < System.currentTimeMillis())
            {

                triggerUpdate(false);
                last_update_time = System.currentTimeMillis();
                last_success_time = System.currentTimeMillis();

            }

        }
    }

}
