package com.redbottledesign.bitcoin.pool.rpc.stratum.server.state;

import com.redbottledesign.bitcoin.pool.rpc.stratum.server.MiningServerConnection;
import com.redbottledesign.bitcoin.rpc.stratum.transport.ConnectionState;

/**
 * <p>The connection state for a Stratum mining server connection after
 * the worker has authenticated but before it has subscribed to work.</p>
 *
 * <p>Aside from the standard requests that are accepted in all connection
 * states ({@code mining.subscribe}, {@code mining.authorize}, and
 * {@code mining.resume}), this state does not accept any requests.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 */
public class AuthorizedPendingSubscriptionState
extends PendingAuthorizationOrSubscriptionState
{
    /**
     * Constructor for {@link AuthorizedPendingSubscriptionState} that
     * configures the connection state for the specified Stratum mining
     * transport.
     *
     * @param   transport
     *          The Stratum TCP server connection, which serves as a message
     *          transport.
     */
    public AuthorizedPendingSubscriptionState(MiningServerConnection transport)
    {
        super(transport);
    }

    /**
     * {@inheritDoc}
     *
     * <p>In this implementation, the worker is always considered authorized,
     * regardless of what is passed-in for {@code authorized}.</p>
     */
    @Override
    protected ConnectionState determineNextConnectionState(boolean subscribed, boolean authorized)
    {
        /* When in this state, the worker is already authorized; the
         * 'authorized' parameter is ignored.
         */
        return super.determineNextConnectionState(subscribed, true);
    }
}
