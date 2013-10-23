package com.redbottledesign.util;

import java.util.concurrent.BlockingQueue;

public class QueueUtils
{
  public static <T> void ensureQueued(BlockingQueue<T> queue, T object)
  {
    boolean saved = false;

    do
    {
      try
      {
        queue.put(object);

        saved = true;
      }

      catch (InterruptedException innerEx)
      {
        // Suppressed; expected
      }
    }
    while (!saved);
  }
}
