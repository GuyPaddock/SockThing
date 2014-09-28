package com.redbottledesign.bitcoin.pool.rpc.stratum.server.state;

import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningAuthorizeRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningResumeRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.message.MiningSubscribeRequest;
import com.redbottledesign.bitcoin.pool.rpc.stratum.server.MiningServerConnection;
import com.redbottledesign.bitcoin.rpc.stratum.transport.MessageListener;

/**
 * <p>The connection state for a Stratum mining server connection prior to the
 * worker subscribing to work or authenticating.</p>
 *
 * <p>Aside from the standard requests that are accepted in all connection
 * states ({@code mining.subscribe}, {@code mining.authorize}, and
 * {@code mining.resume}), this state does not accept any requests.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 */
public class PendingAuthorizationOrSubscriptionState
extends AbstractMiningServerConnectionState
{
    /**
     * Constructor for {@link PendingAuthorizationOrSubscriptionState} that
     * configures the connection state for the specified Stratum mining
     * transport.
     *
     * @param   transport
     *          The Stratum TCP server connection, which serves as a message
     *          transport.
     */
    public PendingAuthorizationOrSubscriptionState(MiningServerConnection transport)
    {
        super(transport);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initializeHandlers()
    {
        super.initializeHandlers();

        // mining.subscribe
        this.registerRequestHandler(
            MiningSubscribeRequest.METHOD_NAME,
            MiningSubscribeRequest.class,
            new MessageListener<MiningSubscribeRequest>()
            {
                @Override
                public void onMessageReceived(MiningSubscribeRequest message)
                {
                    PendingAuthorizationOrSubscriptionState.this.handleMiningSubscribeRequest(message);
                }
            });

        // mining.resume
        this.registerRequestHandler(
            MiningResumeRequest.METHOD_NAME,
            MiningResumeRequest.class,
            new MessageListener<MiningResumeRequest>()
            {
                @Override
                public void onMessageReceived(MiningResumeRequest message)
                {
                    PendingAuthorizationOrSubscriptionState.this.handleMiningResumeRequest(message);
                }
            });

        // mining.authorize
        this.registerRequestHandler(
            MiningAuthorizeRequest.METHOD_NAME,
            MiningAuthorizeRequest.class,
            new MessageListener<MiningAuthorizeRequest>()
            {
                @Override
                public void onMessageReceived(MiningAuthorizeRequest message)
                {
                    PendingAuthorizationOrSubscriptionState.this.handleMiningAuthorizeRequest(message);
                }
            });
    }
}