package com.redbottledesign.bitcoin.rpc.stratum.transport;

import com.redbottledesign.bitcoin.rpc.stratum.message.MessageMarshaller;

/**
 * <p>Abstract base implementation for Stratum {@link ConnectionState}
 * implementations.</p>
 *
 * <p>This base implementation is optional. {@link ConnectionState} consumers
 * should only refer to the {@link ConnectionState} interface and never make a
 * direct reference to this class.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 */
public abstract class AbstractConnectionState
implements ConnectionState
{
    /**
     * The Stratum message transport to which this state corresponds.
     */
    private final StatefulMessageTransport transport;

    /**
     * The marshaller that will be used in this state to receive and send
     * messages.
     */
    private final MessageMarshaller marshaller;

    /**
     * <p>Constructor for {@link AbstractConnectionState}.</p>
     *
     * <p>Initializes a new instance that corresponds to the specified
     * connection.</p>
     *
     * @param   transport
     *          The Stratum message transport to which the state corresponds.
     */
    public AbstractConnectionState(StatefulMessageTransport transport)
    {
        this.transport  = transport;
        this.marshaller = this.createMarshaller();
    }

    /**
     * Gets the Stratum message transport to which this state corresponds.
     *
     * @return  The message transport.
     */
    public StatefulMessageTransport getTransport()
    {
        return this.transport;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageMarshaller getMarshaller()
    {
        return this.marshaller;
    }

    /**
     * Factory method invoked to give sub-classes a chance to customize the
     * marshaller with the messages that it is expecting.
     *
     * @return  The Stratum message marshaller that should be used while in this
     *          connection state.
     */
    protected MessageMarshaller createMarshaller()
    {
        return new MessageMarshaller();
    }

    /**
     * <p>Convenience method for changing the state of the current
     * connection.</p>
     *
     * <p>After this method is called, this state will no longer handle
     * marshalling of messages.</p>
     *
     * @param   newState
     *          The new state for the connection.
     */
    protected void moveToState(ConnectionState newState)
    {
        this.getTransport().setConnectionState(newState);
    }
}