package com.redbottledesign.bitcoin.rpc.stratum.transport;

/**
 * <p>Base class for Stratum message transports that vary the types of
 * messages they accept based on the state of their conversation with the
 * other system.</p>
 *
 * <p>� 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 *
 */
public abstract class StatefulMessageTransport
extends AbstractMessageTransport
{
    /**
     * The state of this transport's current connection.
     */
    protected volatile ConnectionState connectionState;

    /**
     * Constructor for {@link StatefulMessageTransport}.
     */
    public StatefulMessageTransport()
    {
        super();

        this.connectionState = null;
    }

    /**
     * <p>Gets the current state of this transport's connection.</p>
     *
     * <p>The state controls what messages the transport expects to receive at
     * any given point in time.</p>
     *
     * @return  Either the current connection state; or {@code null} if this
     *          transport is not connected.
     */
    protected ConnectionState getConnectionState()
    {
        return this.connectionState;
    }

    /**
     * <p>Notifies the current connection state (if any) that it is about to
     * end, sets the current state of this transport's connection to the
     * specified state, and starts the new state.</p>
     *
     * <p>The connection state controls what messages the transport expects to
     * receive at any given point in time.</p>
     *
     * <p>If {@code state} is already the active state of the connection, this
     * method returns without having any effect.</p>
     *
     * @param   state
     *          The new state for the connection.
     *
     * @throws  IllegalArgumentException
     *          If {@code state} is {@code null}.
     */
    protected void setConnectionState(ConnectionState state)
    throws IllegalArgumentException
    {
        if (state == null)
            throw new IllegalArgumentException("state cannot be null.");

        if (this.connectionState != state)
        {
            if (this.connectionState != null)
                this.connectionState.end();

            this.connectionState = state;

            this.connectionState.start();
        }
    }
}