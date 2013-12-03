package com.github.fireduck64.sockthing;

import java.util.HashMap;
import java.util.Map;


/**
 * If you have any other user settings, you can just extend this and add them on.
 * The same PoolUser from the AuthHandler will be sent into the ShareSaver.
 */
public class PoolUser
{
    /**
     * The name used to submit work units.
     */
    private final String worker_name;

    /**
     * Name that work is credited to.
     */
    private String name;

    /**
     * The minimum difficulty of the worker's shares.
     */
    private int difficulty = 1;

    /**
     * A map of optional extra data about this pool user.
     */
    private final Map<Class<? extends PoolUserExtension>, PoolUserExtension> extensions;

    public PoolUser(String worker_name)
    {
        this.worker_name = worker_name;

        this.extensions = new HashMap<Class<? extends PoolUserExtension>, PoolUserExtension>();
    }

    public void setName(String name)
    {
        this.name = name;
    }
    public void setDifficulty(int difficulty)
    {
        this.difficulty = difficulty;
    }

    public int getDifficulty()
    {
        return this.difficulty;
    }

    public String getName()
    {
        return this.name;
    }

    public String getWorkerName()
    {
        return this.worker_name;
    }

    @SuppressWarnings("unchecked")
    public <T extends PoolUserExtension> T getExtension(Class<T> extensionType)
    {
        return (T)this.extensions.get(extensionType);
    }

    public void putExtension(PoolUserExtension extension)
    {
        Class<? extends PoolUserExtension> extensionType = extension.getClass();

        if (this.extensions.containsKey(extensionType))
        {
            throw new IllegalArgumentException(
                String.format(
                    "An extension of the specified type (%s) is already defined for this worker (%s.%s).",
                    extensionType.getName(),
                    this.name,
                    this.worker_name));
        }

        this.extensions.put(extensionType, extension);
    }
}