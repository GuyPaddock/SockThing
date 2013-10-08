package com.redbottledesign.bitcoin.pool;

import java.util.concurrent.TimeUnit;

public abstract class Agent
extends Thread
implements Stoppable
{
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

    this.lastCheck                = 0;
    this.frequencyInMilliseconds  = frequencyInMilliseconds;
  }

  public long getFrequencyInMilliseconds()
  {
    return this.frequencyInMilliseconds;
  }

  @Override
  public void run()
  {
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

      catch (Throwable t)
      {
        // Top-level handler
        t.printStackTrace();
      }

      if (!this.isStopping)
      {
        synchronized(this)
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

  protected abstract void runPeriodicTask()
  throws Exception;
}