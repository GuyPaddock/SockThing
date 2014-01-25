package com.redbottledesign.bitcoin.pool.rpc.stratum.server.state;

import com.redbottledesign.bitcoin.pool.rpc.stratum.server.MiningServerConnection;

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
}