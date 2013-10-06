
package com.github.fireduck64.sockthing;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Listens on a UDP port for a packet which indicates there is a new block
 * Completely optional to use.
 */
public class NotifyListenerUDP extends Thread
{
    private final StratumServer server;
    private final int port;

    public NotifyListenerUDP(StratumServer server)
    {
        this.server = server;
        server.getConfig().require("notify_port");

        port = server.getConfig().getInt("notify_port");

        this.setName("NotifyListenerUDP");
        this.setDaemon(true);


    }
    @Override
    public void run()
    {
        try
        {
            DatagramSocket ds = new DatagramSocket(port);

            while(true)
            {
                DatagramPacket dp = new DatagramPacket(new byte[1024], 1024);

                ds.receive(dp);
                server.getEventLog().log("UDP Block notify received");
                server.notifyNewBlock();
            }
        }
        catch(java.io.IOException e)
        {
            System.out.println("Unable to continue notify listen");
        }

    }


}
