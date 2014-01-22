package com.redbottledesign.bitcoin.pool.rpc.stratum.client;

import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningAuthorizeResponse;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningNotifyRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSetDifficultyRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubmitResponse;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubscribeResponse;

/**
 * <p>An abstract adapter class for receiving stratum mining client events.
 * The methods in this class are empty. This class exists as convenience for
 * creating listener objects.</p>
 *
 * <p>Extend this class and override the methods for the events of interest.
 * (If you implement the {@link MiningClientEventListener} interface, you have
 * to define all of the methods in it. This abstract class defines null methods
 * for them all, so you only have to define methods for events you care about.)
 * </p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 *
 */
public class MiningClientEventAdapter
implements MiningClientEventListener
{
    /**
     * {@inheritDoc}
     */
    @Override
    public void onAuthenticated(MiningAuthorizeResponse response)
    {
        // Default, no-op implementation
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSubscribed(MiningSubscribeResponse response)
    {
        // Default, no-op implementation
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDifficultySet(MiningSetDifficultyRequest request)
    {
        // Default, no-op implementation
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNewWorkReceived(MiningNotifyRequest request)
    {
        // Default, no-op implementation
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onWorkSubmitted(MiningSubmitResponse response)
    {
        // Default, no-op implementation
    }
}