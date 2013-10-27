package com.redbottledesign.bitcoin.pool;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Agent
extends Thread
implements Stoppable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Agent.class);

    private static final long DEFAULT_FREQUENCY_IN_MILLISECONDS = TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS);

    private long lastCheck;
    private final long frequencyInMilliseconds;
    private volatile boolean isStopping;

    public Agent()
    {
        this(DEFAULT_FREQUENCY_IN_MILLISECONDS);
    }

    public Agent(long frequencyInMilliseconds)
    {
        this.setDaemon(true);
        this.setName(this.getClass().getSimpleName());

        this.lastCheck = 0;
        this.frequencyInMilliseconds = frequencyInMilliseconds;
    }

    public long getFrequencyInMilliseconds()
    {
        return this.frequencyInMilliseconds;
    }

    @Override
    public void run()
    {
        this.loadConfig();

        while (!this.isStopping)
        {
            try
            {
                if (System.currentTimeMillis() > (lastCheck + this.frequencyInMilliseconds))
                {
                    this.runPeriodicTask();

                    this.lastCheck = System.currentTimeMillis();
                }
            }

            catch (Throwable ex)
            {
                // Top-level handler
                if (LOGGER.isErrorEnabled())
                {
                    LOGGER.error(
                        String.format(
                            "Unhandled exception: %s\n%s",
                            ex.getMessage(),
                            ExceptionUtils.getStackTrace(ex)));
                }
            }

            if (!this.isStopping)
            {
                synchronized (this)
                {
                    try
                    {
                        // FIXME: Switch to scheduled threads.
                        this.wait(this.frequencyInMilliseconds / 4);
                    }

                    catch (InterruptedException e)
                    {
                        // Suppressed; expected
                    }
                }
            }
        }
    }

    @Override
    public synchronized void stopProcessing()
    {
        this.isStopping = true;

        this.interrupt();

        // Block indefinitely until thread stops
        try
        {
            this.join();
        }

        catch (InterruptedException e)
        {
            // Suppress; expected.
        }
    }

    public boolean isStopping()
    {
        return this.isStopping;
    }

    protected void loadConfig()
    {
        // By default, there's nothing to check.
    }

    protected abstract void runPeriodicTask()
    throws Exception;
}