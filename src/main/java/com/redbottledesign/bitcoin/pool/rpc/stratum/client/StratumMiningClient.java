package com.redbottledesign.bitcoin.pool.rpc.stratum.client;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.LinkedHashSet;
import java.util.Set;

import com.redbottledesign.bitcoin.pool.rpc.stratum.client.state.PendingSubscriptionState;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningAuthorizeResponse;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningNotifyRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSetDifficultyRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubmitResponse;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubscribeResponse;
import com.redbottledesign.bitcoin.rpc.stratum.transport.tcp.StratumTcpClient;

/**
 * <p>Stratum mining protocol implementation over TCP.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 *
 */
public class StratumMiningClient
extends StratumTcpClient
{
    protected static final String DEFAULT_CLIENT_VERSION_STRING = "redbottle_java_miner/1.0";

    protected Set<MiningClientEventListener> clientEventListeners;

    private String clientVersionString;
    private String workerUsername;
    private String workerPassword;

    public StratumMiningClient()
    {
        this(null, null);
    }

    public StratumMiningClient(String workerUsername, String workerPassword)
    {
        this(workerUsername, workerPassword, DEFAULT_CLIENT_VERSION_STRING);
    }

    public StratumMiningClient(String workerUsername, String workerPassword, String versionString)
    {
        super();

        this.setClientVersionString(versionString);
        this.setWorkerUsername(workerUsername);
        this.setWorkerPassword(workerPassword);

        this.clientEventListeners = new LinkedHashSet<>();

        this.setPostConnectState(
            new PendingSubscriptionState(this));
    }

    public String getClientVersionString()
    {
        return this.clientVersionString;
    }

    public void setClientVersionString(String versionString)
    {
        this.clientVersionString = versionString;
    }

    public String getWorkerUsername()
    {
        return this.workerUsername;
    }

    public void setWorkerUsername(String workerUsername)
    {
        this.workerUsername = workerUsername;
    }

    public String getWorkerPassword()
    {
        return this.workerPassword;
    }

    public void setWorkerPassword(String workerPassword)
    {
        this.workerPassword = workerPassword;
    }

    public void notifyEventListeners(MiningClientEventNotifier notifier)
    {
        for (MiningClientEventListener listener : this.clientEventListeners)
        {
            notifier.notifyListener(listener);
        }
    }

    public void registerEventListener(MiningClientEventListener listener)
    {
        this.clientEventListeners.add(listener);
    }

    public void unregisterEventListener(MiningClientEventListener listener)
    {
        this.clientEventListeners.remove(listener);
    }

    public static void main(String[] args)
    throws UnknownHostException, IOException, InterruptedException
    {
        StratumMiningClient client = new StratumMiningClient("GuyPaddock_Jupiter", "x");

        client.registerEventListener(new MiningClientEventListener()
        {
            @Override
            public void onWorkerAuthenticated(MiningAuthorizeResponse response)
            {
                System.out.println("Worker authenticated: " + response.toJson());
            }

            @Override
            public void onWorkSubmitted(MiningSubmitResponse response)
            {
                System.out.println("Work submitted: " + response.toJson());
            }

            @Override
            public void onNewWorkReceived(MiningNotifyRequest request)
            {
                System.out.println("New work received: " + request.toJson());
            }

            @Override
            public void onMinerSubscribed(MiningSubscribeResponse response)
            {
                System.out.println("Miner subscribed: " + response.toJson());
            }

            @Override
            public void onDifficultySet(MiningSetDifficultyRequest request)
            {
                System.out.println("Difficulty set: " + request.toJson());
            }
        });

        client.connect("mint.bitminter.com", 3333);

        client.getInThread().join();
        client.getOutThread().join();
    }
}