package com.redbottledesign.bitcoin.pool.rpc.stratum.client;

import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningAuthorizeResponse;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningNotifyRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSetDifficultyRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubmitResponse;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubscribeResponse;

/**
 * <p>An interface for classes interested in tracking stratum mining client
 * events.</p>
 *
 * <p>Stratum mining client events let you track when the mining client
 * subscribes to work, receives new work, authenticates, receives a new target
 * difficulty, and submits work.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 */
public interface MiningClientEventListener
{
    /**
     * Event that is fired as soon as the Stratum mining client successfully
     * authenticates with the mining pool.
     *
     * @param   response
     *          A representation of the response from the server.
     */
    public void onAuthenticated(MiningAuthorizeResponse response);

    /**
     * Event that is fired as soon as the Stratum mining client successfully
     * subscribes to work from the mining pool.
     *
     * @param   response
     *          A representation of the response from the server.
     */
    public void onSubscribed(MiningSubscribeResponse response);

    /**
     * Event that is fired whenever the Stratum mining client receives a new
     * target difficulty from the mining pool.
     *
     * @param   request
     *          A representation of the difficulty request from the server.
     */
    public void onDifficultySet(MiningSetDifficultyRequest request);

    /**
     * Event that is fired whenever the Stratum mining client receives new work
     * from the mining pool.
     *
     * @param   request
     *          A representation of the work request from the server.
     */
    public void onNewWorkReceived(MiningNotifyRequest request);

    /**
     * <p>Event that is fired as soon as the mining pool responds to work that
     * the Stratum mining client has submitted.</p>
     *
     * <p>This event is fired even if the work is rejected. Whether or not the
     * work was accepted can be found by calling
     * {@link MiningSubmitResponse#wasAccepted()} on the response. If the work
     * was rejected, the reason can be obtained by calling
     * {@link MiningSubmitResponse#getError()}.</p>
     *
     * @param   response
     *          A representation of the response from the server.
     */
    public void onWorkSubmitted(MiningSubmitResponse response);
}
