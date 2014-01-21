package com.redbottledesign.bitcoin.rpc.stratum.transport;

import com.redbottledesign.bitcoin.rpc.stratum.message.MessageMarshaller;
import com.redbottledesign.bitcoin.rpc.stratum.message.RequestMessage;


/**
 * <p>Interface for the various states of a
 * {@link StatefulMessageTransport}.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 */
public interface ConnectionState
{
    /**
     * Sets in motion any of the actions that should be performed when the
     * connection enters this state.
     */
    public abstract void start();

    /**
     * Notifies this connection state that the connection is about to
     * transition to a different state.
     */
    public abstract void end();

    /**
     * <p>Gets the message marshaller that should be used to marshal and
     * unmarshal messages while in this state.</p>
     *
     * <p>The configuration of the marshaller will determine which messages
     * are accepted while the transport is in this state.</p>
     *
     * @return  The message marshaller to use while in this state.
     */
    public abstract MessageMarshaller getMarshaller();

    /**
     * Notifies this connection state to process the provided request message.
     *
     * @param   message
     *          The message to process.
     */
    public abstract void processRequest(RequestMessage message);
}