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
        final String className = this.getClass().getSimpleName();

        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace(
                String.format(
                    "%s constructed with a frequency of %d milliseconds.",
                    className,
                    frequencyInMilliseconds));
        }

        this.setDaemon(true);
        this.setName(className);

        this.lastCheck                  = 0;
        this.frequencyInMilliseconds    = frequencyInMilliseconds;
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
                if (System.currentTimeMillis() > (this.lastCheck + this.frequencyInMilliseconds))
                {
                    if (LOGGER.isTraceEnabled())
                    {
                        LOGGER.trace(
                            String.format(
                                "Timer threshold of %d milliseconds since last check at %d reached.",
                                this.frequencyInMilliseconds,
                                this.lastCheck));

                        LOGGER.trace(
                            String.format(
                                "Calling %s.runPeriodicTask()",
                                this.getClass().getSimpleName()));
                    }

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
                /* NOTE: This may block if this agent was interrupted to
                 * modify with its data.
                 */
                synchronized (this)
                {
                    try
                    {
                        final long waitDuration = this.frequencyInMilliseconds / 4L;

                        if (LOGGER.isTraceEnabled())
                        {
                            LOGGER.trace(
                                String.format(
                                    "%s.wait(%d)",
                                    this.getClass().getSimpleName(),
                                    waitDuration));
                        }

                        // FIXME: Switch to scheduled threads.
                        this.wait(waitDuration);
                    }

                    catch (InterruptedException ex)
                    {
                        if (LOGGER.isTraceEnabled())
                            LOGGER.trace("run(): wait() interrupted.");
                    }
                }
            }
        }
    }

    @Override
    public synchronized void stopProcessing()
    {
        this.isStopping = true;

        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace(
                String.format("%s.stopProcessing() called; calling interrupt().", this.getClass().getSimpleName()));
        }

        this.interrupt();

        try
        {
            if (LOGGER.isTraceEnabled())
                LOGGER.trace(String.format("Calling %s.join().", this.getClass().getSimpleName()));

            // Block indefinitely until thread stops
            this.join();
        }

        catch (InterruptedException e)
        {
            if (LOGGER.isTraceEnabled())
                LOGGER.trace("stopProcessing(): join() interrupted.");
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