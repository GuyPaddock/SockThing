package com.redbottledesign.bitcoin.pool.tool;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class QueueManagementTool
{
    private static final String QUEUE_NAME_PERSISTENCE  = "persistence";
    private static final String QUEUE_NAME_PPLNS        = "pplns";

    private static final String ACTION_QUEUE_EVICT_ALL  = "queue-evict-all";
    private static final String ACTION_QUEUE_EVICT      = "queue-evict";

    public static void main(String[] args)
    throws UnknownHostException, IOException, JSONException
    {
        if (args.length < 5)
        {
            printUsage();
        }

        else
        {
            String  hostName    = args[0];
            int     port        = Integer.valueOf(args[1]);
            String  password    = args[2],
                    queueName   = args[3],
                    action      = args[4];

            if (!Arrays.asList(QUEUE_NAME_PERSISTENCE, QUEUE_NAME_PPLNS).contains(queueName) ||
                !Arrays.asList(ACTION_QUEUE_EVICT, ACTION_QUEUE_EVICT_ALL).contains(action))
            {
                printUsage();
            }

            else if (action.equals(ACTION_QUEUE_EVICT))
            {
                if (args.length != 6)
                {
                    printUsage();
                }

                else
                {
                    Long        queueItemId = Long.valueOf(args[4]);
                    JSONObject  message     = new JSONObject();
                    JSONArray   params      = new JSONArray();

                    message.put("id",     JSONObject.NULL);
                    message.put("method", String.format("mining.pool.queue.%s.evict", queueName));

                    params.put(password);
                    params.put(queueItemId);

                    message.put("params", params);

                    runRequest(hostName, port, message);
                }
            }

            else if (action.equals(ACTION_QUEUE_EVICT_ALL))
            {
                if (args.length != 5)
                {
                    printUsage();
                }

                else
                {
                    JSONObject  message = new JSONObject();
                    JSONArray   params  = new JSONArray();

                    message.put("id",     JSONObject.NULL);
                    message.put("method", String.format("mining.pool.queue.%s.evict-all", queueName));

                    params.put(password);

                    message.put("params", params);

                    runRequest(hostName, port, message);
                }
            }
        }
    }

    protected static void printUsage()
    {
        System.err.println("Usage: hostname port-number pool-control-password (persistence|pplns) queue-evict queue-item-id");
        System.err.println("       hostname port-number pool-control-password (persistence|pplns) queue-evict-all");
        System.err.println("");
    }

    protected static void runRequest(String hostName, int port, JSONObject message)
    throws IOException
    {
        try (Socket         clientSocket    = new Socket(hostName, port);
             BufferedWriter requestWriter   = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
             Reader         responseReader  = new InputStreamReader(clientSocket.getInputStream());
             PrintWriter    standardOut     = new PrintWriter(System.out))
        {
            int     bytesRead;
            char[]  buffer  = new char[4096];

            requestWriter.write(message.toString());
            requestWriter.newLine();
            requestWriter.flush();

//          while ((bytesRead = responseReader.read(buffer)) > -1)
//          {
//              standardOut.write(buffer, 0, bytesRead);
//          }
        }
    }
}
