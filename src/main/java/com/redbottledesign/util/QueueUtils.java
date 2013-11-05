package com.redbottledesign.util;

import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueUtils
{
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueUtils.class);

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
                if (LOGGER.isTraceEnabled())
                    LOGGER.trace("ensureQueued(): put() interrupted.");
            }
        }
        while (!saved);
    }
}
