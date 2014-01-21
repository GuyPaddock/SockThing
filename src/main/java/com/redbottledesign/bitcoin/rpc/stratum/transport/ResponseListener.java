package com.redbottledesign.bitcoin.rpc.stratum.transport;

import com.redbottledesign.bitcoin.rpc.stratum.message.ResponseMessage;

/**
 * <p>Abstract interface for objects interested in being notified when one or
 * more results is received over a Stratum {@link MessageTransport}.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 *
 */
public interface ResponseListener
{
    /**
     * Method invoked when a response is received over a message transport.
     *
     * @param   response
     *          The response that was received.
     */
    public abstract void onResponseReceived(ResponseMessage response);
}
