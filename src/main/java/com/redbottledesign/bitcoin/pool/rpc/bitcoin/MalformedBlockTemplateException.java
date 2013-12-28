package com.redbottledesign.bitcoin.pool.rpc.bitcoin;

public class MalformedBlockTemplateException
extends RuntimeException
{
    /**
     * Serial version ID.
     */
    private static final long serialVersionUID = 6495786873856729896L;

    public MalformedBlockTemplateException()
    {
        super();
    }

    public MalformedBlockTemplateException(String message)
    {
        super(message);
    }

    public MalformedBlockTemplateException(String message, Exception cause)
    {
        super(message, cause);
    }
}
