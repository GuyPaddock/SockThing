package com.redbottledesign.bitcoin.pool.rpc.stratum.client;

import java.util.LinkedHashSet;
import java.util.Set;

import com.github.fireduck64.sockthing.Config;
import com.github.fireduck64.sockthing.StratumServer;
import com.redbottledesign.bitcoin.pool.rpc.bitcoin.PrefabCoinbase;
import com.redbottledesign.bitcoin.pool.rpc.stratum.client.state.PendingAuthorizationState;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningAuthorizeResponse;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningNotifyRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSetDifficultyRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubmitResponse;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubscribeResponse;
import com.redbottledesign.bitcoin.rpc.stratum.transport.tcp.StratumTcpClient;

/**
 * <p>Stateful Stratum mining client implementation over TCP.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 */
public class StratumMiningClient
extends StratumTcpClient
{
    /**
     * The default client version reported back to mining pools that
     * issue a {@code client.get_version} request.
     */
    public static final String DEFAULT_CLIENT_VERSION_STRING = "redbottle_java_miner/1.0";

    /**
     * The set of mining client event listeners.
     */
    protected Set<MiningClientEventListener> clientEventListeners;

    /**
     * The version string that will be reported back to mining pools that issue
     * a {@code client.get_version} request.
     */
    private String clientVersionString;

    /**
     * The username that this client will use to authenticate with a mining
     * pool.
     */
    private String workerUsername;

    /**
     * The password that this client will use to authenticate with a mining
     * pool.
     */
    private String workerPassword;

    /**
     * <p>Constructor for {@link StratumMiningClient} that initializes a new
     * Stratum TCP mining client without any worker username or password
     * configured, and using the default client version string.</p>
     *
     * <p>The worker username and/or password should be set by a subsequent
     * call to {@link #setWorkerUsername(String)} and possibly
     * {@link #setWorkerPassword(String)} before calling {@code connect()}.</p>
     *
     * @see #DEFAULT_CLIENT_VERSION_STRING
     * @see #setWorkerUsername(String)
     * @see #setWorkerPassword(String)
     * @see #setClientVersionString(String)
     */
    public StratumMiningClient()
    {
        this(null, null);
    }

    /**
     * <p>Constructor for {@link StratumMiningClient} that initializes a new
     * Stratum TCP mining client with the specified worker username, an empty
     * password, and using the default client version string.</p>
     *
     * <p>If the pool requires a password, it should be set by a subsequent
     * call to {@link #setWorkerPassword(String)} before calling
     * {@code connect()}.</p>
     *
     * @param   workerUsername
     *          The username of the worker being authenticated upon connection
     *          to the pool.
     *
     * @see #DEFAULT_CLIENT_VERSION_STRING
     * @see #setWorkerPassword(String)
     * @see #setClientVersionString(String)
     */
    public StratumMiningClient(String workerUsername)
    {
        this(workerUsername, "", DEFAULT_CLIENT_VERSION_STRING);
    }

    /**
     * <p>Constructor for {@link StratumMiningClient} that initializes a new
     * Stratum TCP mining client with the specified worker username, specified
     * password, and the default client version string.</p>
     *
     * @param   workerUsername
     *          The username of the worker being authenticated upon connection
     *          to the pool.
     *
     * @param   workerPassword
     *          The password of the worker being authenticated upon connection
     *          to the pool.
     *
     * @see #DEFAULT_CLIENT_VERSION_STRING
     * @see #setClientVersionString(String)
     */
    public StratumMiningClient(String workerUsername, String workerPassword)
    {
        this(workerUsername, workerPassword, DEFAULT_CLIENT_VERSION_STRING);
    }

    /**
     * <p>Constructor for {@link StratumMiningClient} that initializes a new
     * Stratum TCP mining client with the specified worker username and
     * password, and using the specified client version string.</p>
     *
     * @param   workerUsername
     *          The username of the worker being authenticated upon connection
     *          to the pool.
     *
     * @param   workerPassword
     *          The password of the worker being authenticated upon connection
     *          to the pool.
     *
     * @param   versionString
     *          The version string to report back to mining pools that issue
     *          a {@code client.get_version} request.
     */
    public StratumMiningClient(String workerUsername, String workerPassword, String versionString)
    {
        super();

        this.setClientVersionString(versionString);
        this.setWorkerUsername(workerUsername);
        this.setWorkerPassword(workerPassword);

        this.clientEventListeners = new LinkedHashSet<>();

        this.setPostConnectState(
            new PendingAuthorizationState(this));
    }

    /**
     * Gets the client version string that this client will report back to
     * mining pools that issue a {@code client.get_version} request.
     *
     * @return  The current client version string.
     */
    public String getClientVersionString()
    {
        return this.clientVersionString;
    }

    /**
     * Sets the client version string that this client should report back to
     * mining pools that issue a {@code client.get_version} request.
     *
     * @param   versionString
     *          The new version string.
     */
    public void setClientVersionString(String versionString)
    {
        this.clientVersionString = versionString;
    }

    /**
     * Gets the username that this client will use to authenticate with a
     * mining pool.
     *
     * @return  The worker user name.
     */
    public String getWorkerUsername()
    {
        return this.workerUsername;
    }

    /**
     * Sets the username that this client will use to authenticate with a
     * mining pool.
     *
     * @param   workerUsername
     *          The new worker username.
     */
    public void setWorkerUsername(String workerUsername)
    {
        this.workerUsername = workerUsername;
    }

    /**
     * Gets the password (if any) that this client will use to authenticate
     * with a mining pool.
     *
     * @return  The worker password.
     */
    public String getWorkerPassword()
    {
        return this.workerPassword;
    }

    /**
     * Sets the password that this client will use to authenticate with a
     * mining pool.
     *
     * @param   workerPassword
     *          The new worker password. This can be {@code null}.
     */
    public void setWorkerPassword(String workerPassword)
    {
        this.workerPassword = workerPassword;
    }

    /**
     * Registers a new client event listener, which will be informed about all
     * future Stratum mining events that affect this client.
     *
     * @param   listener
     *          The listener to register.
     */
    public void registerEventListener(MiningClientEventListener listener)
    {
        this.clientEventListeners.add(listener);
    }

    /**
     * Unregisters a new client event listener, which will no longer be
     * informed about future Stratum mining events that affect this client.
     *
     * @param   listener
     *          The listener to register.
     */
    public void unregisterEventListener(MiningClientEventListener listener)
    {
        this.clientEventListeners.remove(listener);
    }

    /**
     * Method used by the components of this client to notify mining client
     * event listeners when an mining client event occurs.
     *
     * @param   notifier
     *          The notifier callback that will be invoked to fire the
     *          appropriate event on each listener.
     */
    public void notifyEventListeners(MiningClientEventNotifier notifier)
    {
        for (MiningClientEventListener listener : this.clientEventListeners)
        {
            notifier.notifyListener(listener);
        }
    }

    /**
     * FIXME: Test code.
     */
    public static void main(String[] args)
    throws Exception
    {
        StratumMiningClient client = new StratumMiningClient("GuyPaddock_Jupiter");

//        StratumMiningClient client = new StratumMiningClient("RedBottleDesign_RedPool");

        client.registerEventListener(new MiningClientEventListener()
        {
            protected byte[] extraNonce1;
            private int extraNonce2Size;

            @Override
            public void onAuthenticated(MiningAuthorizeResponse response)
            {
                System.out.println("Worker authenticated: " + response.isAuthorized());
            }

            @Override
            public void onWorkSubmitted(MiningSubmitResponse response)
            {
                System.out.println("Work submitted: " + response.toJson());
            }

            @Override
            public void onSubscribed(MiningSubscribeResponse response)
            {
                System.out.println("Miner subscribed: " + response.toJson());

                this.extraNonce1        = response.getExtraNonce1();
                this.extraNonce2Size    = response.getExtraNonce2ByteLength();
            }

            @Override
            public void onDifficultySet(MiningSetDifficultyRequest request)
            {
                System.out.println("Difficulty set: " + request.getDifficulty());
            }

            @Override
            public void onNewWorkReceived(MiningNotifyRequest request)
            {
                byte[]          coinbasePart1 = request.getCoinbasePart1(),
                                coinbasePart2 = request.getCoinbasePart2();
                PrefabCoinbase  coinbase;

                System.out.println("New work received: " + request.toJson());

                try
                {
                    coinbase =
                        new PrefabCoinbase(
                            new StratumServer(
                                new Config("C:/Users/gpaddock/Documents/Eclipse workspace/SockThing+/src/main/resources/config/pool_prodnet.cfg")),
                            coinbasePart1,
                            this.extraNonce1,
                            this.extraNonce2Size,
                            coinbasePart2);

                    System.out.println(coinbase.getCoinbaseTransaction());
                }

                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }
        });

        client.connect("mint.bitminter.com", 3333);
//        client.connect("stratum.btcguild.com", 3333);

        client.getInputThread().join();
        client.getOutputThread().join();
    }
}