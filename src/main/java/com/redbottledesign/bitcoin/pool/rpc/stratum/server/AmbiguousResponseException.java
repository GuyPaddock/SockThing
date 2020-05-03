package com.redbottledesign.bitcoin.pool.rpc.stratum.server;

/**
 * <p>Exception thrown when a request that requires a single response receives
 * multiple responses from registered {@link MiningServerEventListener}s.</p>
 *
 * <p>© 2013 - 2014 RedBottle Design, LLC.</p>
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 */
public class AmbiguousResponseException
extends RuntimeException
{
    /**
     * Serial version ID.
     */
    private static final long serialVersionUID = -3432162677370976885L;

    /**
     * Default constructor for {@link AmbiguousResponseException}.
     */
    public AmbiguousResponseException()
    {
        super();
    }

    /**
     * Constructor for {@link AmbiguousResponseException} that initializes
     * the new exception with the specified message.
     *
     * @param   message
     *          The message to include in the new exception.
     */
    public AmbiguousResponseException(String message)
    {
        super(message);
    }
}
