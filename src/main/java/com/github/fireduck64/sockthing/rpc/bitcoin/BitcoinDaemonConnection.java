package com.github.fireduck64.sockthing.rpc.bitcoin;

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
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Block;

public class BitcoinDaemonConnection
implements BitcoinRpcConnection
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BitcoinDaemonConnection.class);

    private String username;
    private String password;
    private String host;
    private int port;
    private int requestNumber;

    public BitcoinDaemonConnection(Config config)
    {
        this.validateAndLoadConfig(config);

        this.requestNumber = 1;
    }

    public JSONObject sendPost(JSONObject post)
    throws IOException, JSONException
    {
        String response;

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Bitcoin RPC POST: " + post.toString(2));

        response = this.sendPost(this.getUrl(), post.toString());

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Bitcoin RPC response: " + response);

        return new JSONObject(response);
    }

    protected String getUsername()
    {
        return this.username;
    }

    protected String getPassword()
    {
        return this.password;
    }

    protected String getHost()
    {
        return this.host;
    }

    protected int getPort()
    {
        return this.port;
    }

    protected String getUrl()
    {
        return "http://" + host + ":" + port + "/";
    }

    protected void validateAndLoadConfig(Config config)
    {
        config.require("bitcoind_username");
        config.require("bitcoind_password");
        config.require("bitcoind_host");
        config.require("bitcoind_port");

        this.username = config.get("bitcoind_username");
        this.password = config.get("bitcoind_password");
        this.host     = config.get("bitcoind_host");
        this.port     = config.getInt("bitcoind_port");
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

        while(scan.hasNextLine())
        {
            String line = scan.nextLine();
            sb.append(line);
            sb.append('\n');
        }

        scan.close();
        return sb.toString();

    }

    public String getSimplePostRequest(String cmd)
    {
        return "{\"method\":\"" + cmd + "\",\"params\":[],\"id\":" + this.getRequestId() + "}\n";
    }

    public JSONObject doSimplePostRequest(String cmd)
    throws IOException, JSONException
    {
        return this.sendPost(new JSONObject(this.getSimplePostRequest(cmd)));
    }

    @Override
    public boolean submitBlock(Block block)
    throws IOException, JSONException
    {
        boolean     wasSuccessful;
        JSONArray   params          = new JSONArray();
        JSONObject  msg             = new JSONObject(),
                    result;

        msg.put("method", "submitblock");
        msg.put("id",     Integer.toString(this.getRequestId()));

        params.put(Hex.encodeHexString(block.bitcoinSerialize()));

        msg.put("params", params);

        result = this.sendPost(msg);

        wasSuccessful = (result.isNull("error") && result.isNull("result"));

        if (!wasSuccessful)
        {
            if (LOGGER.isErrorEnabled())
                LOGGER.error("Block submit error:  "+ result.get("error"));
        }

        return wasSuccessful;
    }

    @Override
    public JSONObject getCurrentBlockTemplate()
    throws IOException, JSONException
    {
        JSONObject  post    = new JSONObject(this.getSimplePostRequest("getblocktemplate")),
                    result;

        result = this.sendPost(post).getJSONObject("result");

        return result;
    }

    @Override
    public double getDifficulty()
    throws IOException, JSONException
    {
        JSONObject  post    = new JSONObject(this.getSimplePostRequest("getdifficulty"));
        double      result  = this.sendPost(post).getDouble("result");

        result = this.sendPost(post).getDouble("result");

        return result;
    }

    @Override
    public int getBlockCount()
    throws IOException, JSONException
    {
        JSONObject  post    = new JSONObject(this.getSimplePostRequest("getblockcount"));
        int         result  = this.sendPost(post).getInt("result");

        result = this.sendPost(post).getInt("result");

        return result;
    }

    @Override
    public long getBlockConfirmationCount(String blockHash)
    throws IOException, JSONException
    {
        JSONObject  blockInfo = this.getBlockInfo(blockHash);
        long        result;

        result = blockInfo.getLong("confirmations");

        return result;
    }

    @Override
    public JSONObject getBlockInfo(String blockHash)
    throws IOException, JSONException
    {
        JSONArray   params          = new JSONArray();
        JSONObject  msg             = new JSONObject(),
                    requestResult,
                    result;

        msg.put("method", "getblock");
        msg.put("id",     Integer.toString(this.getRequestId()));

        params.put(blockHash);

        msg.put("params", params);

        requestResult = this.sendPost(msg);

        if (!requestResult.isNull("error"))
            throw new RuntimeException("Block retrieval failed: " + requestResult.get("error"));

        result = requestResult.getJSONObject("result");

        return result;
    }

    @Override
    public String sendPayment(double amount, Address payFromAddress, Address payToAddress)
    throws IOException, JSONException
    {
        String      paymentHash;
        JSONArray   params      = new JSONArray();
        JSONObject  msg         = new JSONObject(),
                    result;

        msg.put("method", "sendtoaddress");
        msg.put("id",     Integer.toString(this.getRequestId()));

        params.put(payToAddress.toString()); // <bitcoinaddress>
        params.put(amount);                  // <amount>

        msg.put("params", params);

        result = this.sendPost(msg);

        if (!result.isNull("error"))
            throw new RuntimeException("Payment failed: " + result.get("error"));

        paymentHash = result.getString("result");

        return paymentHash;
    }

//    public Block getBlock(String blockHash)
//    throws IOException, JSONException
//    {
//        Block               blockResult;
//        JSONObject          responseResult       = this.getBlockInfo(blockHash),
//                            responseResultObject;
//        JSONArray           responseTransactions;
//        int                 responseVersion;
//        List<Transaction>   transactions;
//
//        if (!responseResult.isNull("error"))
//            throw new RuntimeException("Block retrieval failed: " + responseResult.get("error"));
//
//        responseResultObject = responseResult.getJSONObject("result");
//        responseTransactions = responseResultObject.getJSONArray("tx");
//        responseVersion      = responseResultObject.getInt("version");
//
//        transactions = new ArrayList<>(responseTransactions.length());
//
//        try
//        {
//            for (int resultIndex = 0; resultIndex < responseTransactions.length(); ++resultIndex)
//            {
//                transactions.add(
//                    new Transaction(
//                        this.networkParams,
//                        responseVersion,
//                        HexUtil.hexToHash(responseTransactions.getString(resultIndex))));
//            }
//
//            blockResult = new Block(
//                this.networkParams,
//                responseVersion,
//                HexUtil.hexToHash(responseResultObject.getString("hash")),
//                HexUtil.hexToHash(responseResultObject.getString("merkleroot")),
//                responseResultObject.getLong("time"),
//                responseResultObject.getLong("difficulty"),
//                responseResultObject.getLong("nonce"),
//                transactions);
//        }
//
//        catch (DecoderException ex)
//        {
//            throw new JSONException(ex);
//        }
//
//        return blockResult;
//    }

    protected int getRequestId()
    {
        return (this.requestNumber++);
    }
}