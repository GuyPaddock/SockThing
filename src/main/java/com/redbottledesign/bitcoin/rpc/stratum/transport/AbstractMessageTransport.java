package com.redbottledesign.bitcoin.rpc.stratum.transport;

import java.util.HashSet;
import java.util.Set;

import com.redbottledesign.bitcoin.rpc.stratum.message.ResponseMessage;

/**
 * <p>Abstract base class for {@link MessageTransport} implementations.</p>
 *
 * <p>This base implementation is optional. {@link MessageTransport} consumers
 * should only refer to the {@link MessageTransport} interface and never make a
 * direct reference to this class.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 *
 */
public abstract class AbstractMessageTransport
implements MessageTransport
{
    /**
     * The set of response listeners to notify when responses are received.
     */
    protected Set<ResponseListener> responseListeners;

    /**
     * Default constructor for {@link AbstractMessageTransport}.
     */
    public AbstractMessageTransport()
    {
        this.responseListeners = new HashSet<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerResponseListener(ResponseListener listener)
    {
        this.responseListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterResponseListener(ResponseListener listener)
    {
        this.responseListeners.remove(listener);
    }

    /**
     * Notifies response listeners about the provided response.
     *
     * @param   response
     *          The response to notify listeners about.
     */
    protected void notifyResponseListenersResponseReceived(ResponseMessage response)
    {
        for (ResponseListener listener : this.responseListeners)
        {
            listener.onResponseReceived(response);
        }
    }
}