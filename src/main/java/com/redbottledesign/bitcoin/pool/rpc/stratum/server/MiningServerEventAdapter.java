package com.redbottledesign.bitcoin.pool.rpc.stratum.server;

import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningAuthorizeRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningAuthorizeResponse;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningResumeRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningResumeResponse;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubmitRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubmitResponse;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubscribeRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubscribeResponse;
import com.redbottledesign.bitcoin.rpc.stratum.transport.tcp.StratumTcpServerConnection;


/**
 * <p>An abstract adapter class for receiving stratum mining server events.
 * The methods in this class are empty. This class exists as convenience for
 * creating listener objects.</p>
 *
 * <p>Extend this class and override the methods for the events of interest.
 * (If you implement the {@link MiningServerEventListener} interface, you have
 * to define all of the methods in it. This abstract class defines null methods
 * for them all, so you only have to define methods for events you care about.)
 * </p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 */
public class MiningServerEventAdapter
implements MiningServerEventListener
{
    /**
     * {@inheritDoc}
     */
    @Override
    public void onClientConnecting(StratumTcpServerConnection connection)
    {
        // Default, no-op implementation
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MiningResumeResponse onClientResumingSession(StratumTcpServerConnection connection,
                                                        MiningResumeRequest request)
    {
        // Default, no-op implementation
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MiningAuthorizeResponse onClientAuthenticating(StratumTcpServerConnection connection,
                                                          MiningAuthorizeRequest request)
    {
        // Default, no-op implementation
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MiningSubscribeResponse onClientSubscribing(StratumTcpServerConnection connection,
                                                       MiningSubscribeRequest request)
    {
        // Default, no-op implementation
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MiningSubmitResponse onClientSubmittingWork(StratumTcpServerConnection connection,
                                                       MiningSubmitRequest request)
    {
        // Default, no-op implementation
        return null;
    }
}