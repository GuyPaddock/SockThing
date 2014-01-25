package com.redbottledesign.bitcoin.pool.rpc.stratum.server.state;

import com.redbottledesign.bitcoin.pool.rpc.stratum.server.MiningServerConnection;
import com.redbottledesign.bitcoin.rpc.stratum.transport.ConnectionState;

/**
 * <p>The connection state for a Stratum mining server connection when the
 * worker is eligible to begin submitting work to the mining pool.</p>
 *
 * <p>Aside from the standard requests that are accepted in all connection
 * states ({@code mining.subscribe}, {@code mining.authorize}, and
 * {@code mining.resume}), this state provides handling for the following type
 * of request:</p>
 *
 * <dl>
 *    <dt>{@code mining.submit}</dt>
 *    <dd>Used to submit shares.</dd>
 * </dl>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 */
public class MiningState
extends PendingAuthorizationOrSubscriptionState
{
    /**
     * Constructor for {@link MiningState} that configures
     * the connection state for the specified Stratum mining transport.
     *
     * @param   transport
     *          The Stratum TCP server connection, which serves as a message
     *          transport.
     *
     */
    public MiningState(MiningServerConnection transport)
    {
        super(transport);
    }

    /**
     * {@inheritDoc}
     *
     * <p>In this implementation, the worker is always considered both
     * subscribed and authorized, regardless of what is passed-in.
     * Consequently, the worker will just return to a new instance of
     * this state if the worker was just subscribed or authorized a second
     * time.</p>
     */
    @Override
    protected ConnectionState determineNextConnectionState(boolean subscribed, boolean authorized)
    {
        /* When in this state, the worker is already subscribed and authorized;
         * both parameters are ignored.
         */
        return super.determineNextConnectionState(true, true);
    }
}