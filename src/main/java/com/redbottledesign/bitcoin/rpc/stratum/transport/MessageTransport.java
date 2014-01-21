package com.redbottledesign.bitcoin.rpc.stratum.transport;

import java.io.IOException;

import com.redbottledesign.bitcoin.rpc.stratum.message.Message;
import com.redbottledesign.bitcoin.rpc.stratum.message.RequestMessage;

/**
 * <p>Common interface Stratum transports.</p>
 *
 * <p>A transport is responsible for sending and receiving {@link Message}s
 * over a protocol like HTTP or TCP sockets.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 */
public interface MessageTransport
{
    /**
     * Registers the specified listener to be informed whenever one or more
     * responses are received over the transport.
     *
     * @param   listener
     *          The listener that will be notified when one or more responses
     *          are received.
     */
    public void registerResponseListener(ResponseListener listener);

    /**
     * Unregisters the specified listener from being informed whenever one or
     * more responses are received over the transport.
     *
     * @param   listener
     *          The listener that will no longer be notified when responses are
     *          received.
     */
    public void unregisterResponseListener(ResponseListener listener);

    /**
     * Sends the specified request over the transport.
     *
     * @param   message
     *          The message to send.
     *
     * @throws  IOException
     *          If the transport connection fails for any reason, such as an
     *          interrupted connection or any other type of unexpected
     *          connection drop-out or failure.
     */
    public void sendRequest(RequestMessage message)
    throws IOException;

    /**
     * Polls the transport for any pending, unacknowledged responses from prior
     * requests.
     *
     * @throws  IOException
     *          If the transport connection fails for any reason, such as an
     *          interrupted connection or any other type of unexpected
     *          connection drop-out or failure.
     */
    public void pollForResponses()
    throws IOException;
}