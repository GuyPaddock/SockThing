package com.redbottledesign.bitcoin.pool.agent.persistence;

import java.lang.reflect.Type;

public interface PersistenceCallbackFactory<T extends PersistenceCallback<?>>
{
    public T createCallback(Type desiredType);
}
