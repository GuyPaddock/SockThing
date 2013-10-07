package com.github.fireduck64.sockthing;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.Scanner;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Block;

public class BitcoinRPC
{
    private final String username;
    private final String password;
    private final String host;
    private final int port;

    public BitcoinRPC(Config config)
    {
        config.require("bitcoind_username");
        config.require("bitcoind_password");
        config.require("bitcoind_host");
        config.require("bitcoind_port");

        username=config.get("bitcoind_username");
        password=config.get("bitcoind_password");
        host=config.get("bitcoind_host");
        port=config.getInt("bitcoind_port");
    }


    private String getUrl()
    {
        return "http://" + host + ":" + port + "/";
    }

    private String getUrlCommand(String cmd)
    {
        return getUrl();
    }

    public JSONObject sendPost(JSONObject post)
    throws IOException, JSONException
    {
        //System.out.println(post.toString(2));
        String str = sendPost(getUrl(), post.toString());
        return new JSONObject(str);
    }

    protected String sendPost(String url, String postdata)
    throws IOException
    {
        URL u = new URL(url);

        HttpURLConnection connection = (HttpURLConnection) u.openConnection();

        String basic = username+":"+password;
        String encoded = Base64.encodeBase64String(basic.getBytes());
        connection.setRequestProperty("Authorization", "Basic "+encoded);
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("charset", "utf-8");
        connection.setRequestProperty("Content-Length", "" + Integer.toString(postdata.getBytes().length));
        connection.setUseCaches (false);

        OutputStream wr = connection.getOutputStream ();
        wr.write(postdata.getBytes());
        wr.flush();
        wr.close();

        Scanner scan;

        if (connection.getResponseCode() != 500)
        {
            scan = new Scanner(connection.getInputStream());
        } else {
            scan = new Scanner(connection.getErrorStream());
        }

        StringBuilder sb = new StringBuilder();

        while(scan.hasNextLine())
        {
            String line = scan.nextLine();
            sb.append(line);
            sb.append('\n');
        }


        scan.close();
        return sb.toString();

    }

    public static String getSimplePostRequest(String cmd)
    {
        return "{\"method\":\""+cmd+"\",\"params\":[],\"id\":1}\n";
    }

    public JSONObject doSimplePostRequest(String cmd)
    throws IOException, JSONException
    {
        return sendPost(new JSONObject(getSimplePostRequest(cmd)));
    }

    public JSONObject submitBlock(Block blk)
    throws IOException, JSONException
    {
        Random rnd = new Random();

        JSONObject msg = new JSONObject();
        msg.put("method", "submitblock");
        msg.put("id", "" + rnd.nextInt());

        JSONArray params = new JSONArray();
        params.put(Hex.encodeHexString(blk.bitcoinSerialize()));
        msg.put("params", params);
        //System.out.println(msg.toString(2));
        return sendPost(msg);
    }

    public JSONObject sendPayment(double amount, Address payFromAddress, Address payToAddress)
    throws IOException, JSONException
    {
      Random      rnd           = new Random();
      JSONObject  msg           = new JSONObject();
      JSONArray   params        = new JSONArray();

      msg.put("method", "sendtoaddress");
      msg.put("id", "" + rnd.nextInt());

      params.put(payToAddress.toString());    // <bitcoinaddress>
      params.put(amount);                     // <amount>

      msg.put("params", params);

      return sendPost(msg);
    }
}