package com.redbottledesign.bitcoin.pool.util.queue;

import java.lang.reflect.Type;

public interface QueueItemCallbackFactory<T extends QueueItemCallback<?>>
{
    public T createCallback(Type desiredType);
}
