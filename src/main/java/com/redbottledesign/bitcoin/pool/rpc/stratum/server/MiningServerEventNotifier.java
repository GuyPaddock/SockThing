package com.redbottledesign.bitcoin.pool.rpc.stratum.server;

/**
 * <p>A simple callback interface used to decouple the
 * {@link StratumMiningServer} from each event defined in the
 * {@link MiningServerEventListener} interface.</p>
 *
 * <p>This enables event publishers (typically connection states) to broadcast
 * an event by sub-classing this interface in an anonymous inner class instance
 * that is passed into
 * {@link StratumMiningServer#notifyEventListeners(MiningServerEventNotifier)}.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 *
 */
public interface MiningServerEventNotifier
{
    /**
     * <p>Method called when each event listener is being visited by this
     * instance.</p>
     *
     * <p>The implementation should invoke the correct method on the listener
     * for the type of event that is being broadcast.</p>
     *
     * @param   listener
     *          The listener that will be receiving the event notification.
     */
    public abstract void notifyListener(MiningServerEventListener listener);
}