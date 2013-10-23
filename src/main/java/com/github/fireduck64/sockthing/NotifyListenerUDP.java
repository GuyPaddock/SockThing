
package com.github.fireduck64.sockthing;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens on a UDP port for a packet which indicates there is a new block
 * Completely optional to use.
 */
public class NotifyListenerUDP extends Thread
{
    private static final Logger LOGGER = LoggerFactory.getLogger(NotifyListenerUDP.class);

    private final StratumServer server;
    private final int port;

    public NotifyListenerUDP(StratumServer server)
    {
        this.setName("NotifyListenerUDP");
        this.setDaemon(true);

        this.server = server;

        server.getConfig().require("notify_port");
        this.port = server.getConfig().getInt("notify_port");
    }

    @Override
    public void run()
    {
        try (DatagramSocket ds = new DatagramSocket(port))
        {
            while (true)
            {
                DatagramPacket dp = new DatagramPacket(new byte[1024], 1024);

                ds.receive(dp);

                if (LOGGER.isDebugEnabled())
                  LOGGER.debug("UDP Block notify received");

                this.server.notifyNewBlock();
            }
        }

        catch (IOException ex)
        {
            if (LOGGER.isErrorEnabled())
            {
                LOGGER.error(
                    String.format(
                        "Unable to continue listening for block notifications: %s\n%s",
                        ex.getMessage(),
                        ExceptionUtils.getStackTrace(ex)));
            }
        }
    }
}