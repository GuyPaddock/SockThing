package com.redbottledesign.bitcoin.pool.rpc.stratum.server.state;

import com.redbottledesign.bitcoin.pool.rpc.stratum.server.MiningServerConnection;
import com.redbottledesign.bitcoin.rpc.stratum.transport.ConnectionState;

/**
 * <p>The connection state for a Stratum mining server connection after
 * the worker has subscribed to work but before they have authenticated.</p>
 *
 * <p>Aside from the standard requests that are accepted in all connection
 * states ({@code mining.subscribe}, {@code mining.authorize}, and
 * {@code mining.resume}), this state does not accept any requests.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 */
public class SubscribedPendingAuthorizationState
extends PendingAuthorizationOrSubscriptionState
{
    /**
     * Constructor for {@link SubscribedPendingAuthorizationState} that
     * configures the connection state for the specified Stratum mining
     * transport.
     *
     * @param   transport
     *          The Stratum TCP server connection, which serves as a message
     *          transport.
     */
    public SubscribedPendingAuthorizationState(MiningServerConnection transport)
    {
        super(transport);
    }

    /**
     * {@inheritDoc}
     *
     * <p>In this implementation, the worker is always considered subscribed,
     * regardless of what is passed-in for {@code subscribed}.</p>
     */
    @Override
    protected ConnectionState determineNextConnectionState(boolean subscribed, boolean authorized)
    {
        /* When in this state, the worker is already subscribed; the
         * 'subscribed' parameter is ignored.
         */
        return super.determineNextConnectionState(true, authorized);
    }
}
