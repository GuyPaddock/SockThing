package com.redbottledesign.bitcoin.pool;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.Config;
import com.github.fireduck64.sockthing.rpc.bitcoin.BitcoinDaemonBlockTemplate;
import com.github.fireduck64.sockthing.rpc.bitcoin.BitcoinDaemonConnection;
import com.github.fireduck64.sockthing.rpc.bitcoin.BlockTemplate;
import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.NetworkParameters;

public class PiggyBackedBitcoinDaemonConnection
extends BitcoinDaemonConnection
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PiggyBackedBitcoinDaemonConnection.class);

    protected String secondHost;
    protected int secondPort;
    protected String secondUsername;
    protected String secondPassword;

    public PiggyBackedBitcoinDaemonConnection(NetworkParameters networkParams, Config config)
    {
        super(networkParams, config);
    }

    @Override
    public BlockTemplate getCurrentBlockTemplate()
    throws IOException, JSONException
    {
        JSONArray   params          = new JSONArray();
        JSONObject  msg             = new JSONObject(),
                    caps            = new JSONObject(),
                    requestResult,
                    result;

        msg.put("method", "getblocktemplate");
        msg.put("id",     Integer.toString(this.getRequestId()));

        caps.put("capabilities", new JSONArray());
        params.put(caps);

        msg.put("params", params);

        requestResult = this.sendSecondaryPost(msg);

        if (!requestResult.isNull("error"))
            throw new RuntimeException("Block template retrieval failed: " + requestResult.get("error"));

        result = requestResult.getJSONObject("result");

        // FIXME: Need a custom block template for piggy-backed connections
        return new BitcoinDaemonBlockTemplate(this.getNetworkParams(), result);
    }

    @Override
    public boolean submitBlock(Block block)
    throws IOException, JSONException
    {
        boolean     wasSuccessful;
        JSONArray   params          = new JSONArray();
        JSONObject  message         = new JSONObject(),
                    result;

        message.put("method", "submitblock");
        message.put("id",     Integer.toString(this.getRequestId()));

        params.put(Hex.encodeHexString(block.bitcoinSerialize()));

        message.put("params", params);

        result = this.sendSecondaryPost(message);

        wasSuccessful = (result.isNull("error") && result.isNull("result"));

        if (!wasSuccessful)
        {
            if (LOGGER.isErrorEnabled())
                LOGGER.error("Block submit error:  "+ result.get("error"));
        }

        return wasSuccessful;
    }

    public JSONObject sendSecondaryPost(JSONObject post)
    throws IOException, JSONException
    {
        String response;

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Secondary bitcoin RPC POST: " + post.toString(2));

        response = this.sendSecondaryPost(this.getSecondaryUrl(), post.toString());

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Secondary bitcoin RPC response: " + response);

        return new JSONObject(response);
    }

    @Override
    protected void validateAndLoadConfig(Config config)
    {
        super.validateAndLoadConfig(config);

        config.require("piggy_back_pool_host");
        config.require("piggy_back_pool_port");
        config.require("piggy_back_pool_username");
        config.require("piggy_back_pool_password");

        this.secondHost     = config.get("piggy_back_pool_host");
        this.secondPort     = config.getInt("piggy_back_pool_port");
        this.secondUsername = config.get("piggy_back_pool_username");
        this.secondPassword = config.get("piggy_back_pool_password");
    }

    protected String getSecondaryUrl()
    {
        return "http://" + this.secondHost + ":" + this.secondPort + "/";
    }

    protected String sendSecondaryPost(String url, String postdata)
    throws IOException
    {
        URL u = new URL(url);

        HttpURLConnection connection = (HttpURLConnection) u.openConnection();

        String basic = this.secondUsername + ":" + this.secondPassword;
        String encoded = Base64.encodeBase64String(basic.getBytes());
        connection.setRequestProperty("Authorization", "Basic " + encoded);
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("charset", "utf-8");
        connection.setRequestProperty("Content-Length", "" + Integer.toString(postdata.getBytes().length));
        connection.setUseCaches(false);

        OutputStream wr = connection.getOutputStream();

        wr.write(postdata.getBytes());
        wr.flush();
        wr.close();

        Scanner scan;

        if (connection.getResponseCode() != 500)
            scan = new Scanner(connection.getInputStream());
        else
            scan = new Scanner(connection.getErrorStream());

        StringBuilder sb = new StringBuilder();

        while (scan.hasNextLine())
        {
            String line = scan.nextLine();
            sb.append(line);
            sb.append('\n');
        }

        scan.close();

        return sb.toString();
    }
}