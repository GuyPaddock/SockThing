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
 * <p>An interface for classes interested in tracking stratum mining server
 * events.</p>
 *
 * <p>Stratum mining server events let you track when mining clients connect,
 * resume work, subscribe to work, authenticate, and submit work.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 */
public interface MiningServerEventListener
{
    /**
     * Event that is fired as soon as a Stratum mining client connects to the
     * mining pool.
     *
     * @param   connection
     *          The connection over which the client is communicating with the
     *          server.
     */
    public void onClientConnecting(StratumTcpServerConnection connection);

    /**
     * <p>Event that is fired as soon as a Stratum mining client attempts to
     * resume a prior connection on the mining pool.</p>
     *
     * <p>At least one listener should respond to this event by providing a
     * response that either approves or denies the resume request. If multiple
     * listeners provide a response, the first response encountered that
     * approves the request will be sent back to the client and all others will
     * be discarded. If no listener provides a response, an exception will be
     * thrown after all listeners receive this event.</p>
     *
     * @param   connection
     *          The connection over which the client is communicating with the
     *          server.
     *
     * @param   request
     *          A representation of the request from the client.
     *
     * @return  Either a response to the resume request; or, {@code null} if
     *          this listener can neither approve nor deny resume requests.
     */
    public MiningResumeResponse onClientResumingSession(StratumTcpServerConnection connection,
                                                        MiningResumeRequest request);

    /**
     * <p>Event that is fired as soon as a Stratum mining client attempts to
     * authenticate with the mining pool.</p>
     *
     * <p>At least one listener should respond to this event by providing a
     * response that either approves or denies the authorization request. If
     * multiple listeners provide a response, the first response encountered
     * that approves the request will be sent back to the client and all others
     * will be discarded. If no listener provides a response, an exception will be
     * thrown after all listeners receive this event.</p>
     *
     * @param   connection
     *          The connection over which the client is communicating with the
     *          server.
     *
     * @param   request
     *          A representation of the request from the client.
     *
     * @return  Either a response to the authentication request; or,
     *          {@code null} if this listener can neither approve nor deny
     *          authorization requests.
     */
    public MiningAuthorizeResponse onClientAuthenticating(StratumTcpServerConnection connection,
                                                          MiningAuthorizeRequest request);

    /**
     * <p>Event that is fired as soon as a Stratum mining client attempts to
     * subscribe to work from the mining pool.</p>
     *
     * <p>At least one listener should respond to this event by providing a
     * response that contains the subscription details. If multiple listeners
     * provide a response, or no listener provides a response, an exception
     * will be thrown after all listeners receive this event.</p>
     *
     * @param   connection
     *          The connection over which the client is communicating with the
     *          server.
     *
     * @param   request
     *          A representation of the request from the client.
     *
     * @return  Either a response to the subscription request; or,
     *          {@code null} if this listener does not respond to subscription
     *          requests.
     */
    public MiningSubscribeResponse onClientSubscribing(StratumTcpServerConnection connection,
                                                       MiningSubscribeRequest request);

    /**
     * <p>Event that is fired as soon as a Stratum mining client attempts to
     * submit work to the pool.</p>
     *
     * <p>At least one listener should respond to this event by providing a
     * response that either accepts or refuses the submitted work. If multiple
     * listeners provide a response, the first response encountered that
     * accepts the work will be sent back to the client and all others
     * will be discarded. If no listener provides a response, an exception will
     * be thrown after all listeners receive this event.</p>
     *
     * @param   connection
     *          The connection over which the client is communicating with the
     *          server.
     *
     * @param   request
     *          A representation of the request from the client.
     *
     * @return  Either a response to the authentication request; or,
     *          {@code null} if this listener can neither accept nor refuse
     *          work.
     */
    public MiningSubmitResponse onClientSubmittingWork(StratumTcpServerConnection connection,
                                                       MiningSubmitRequest request);
}
