package com.redbottledesign.bitcoin.pool.rpc.bitcoin.piggyback;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.codec.binary.Hex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.Config;
import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.NetworkParameters;
import com.redbottledesign.bitcoin.pool.rpc.bitcoin.BitcoinDaemonConnection;
import com.redbottledesign.bitcoin.pool.rpc.bitcoin.BlockTemplate;

public class PiggyBackedBitcoinDaemonConnection
extends BitcoinDaemonConnection
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PiggyBackedBitcoinDaemonConnection.class);

    protected String secondHost;
    protected int secondPort;
    protected String secondUsername;
    protected String secondPassword;
    protected PiggyBackedBlockTemplate lastBlockTemplate;

    public PiggyBackedBitcoinDaemonConnection(NetworkParameters networkParams, Config config)
    {
        super(networkParams, config);
    }

    @Override
    public BlockTemplate getCurrentBlockTemplate()
    throws IOException, JSONException
    {
        JSONArray                   params          = new JSONArray();
        JSONObject                  msg             = new JSONObject(),
                                    caps            = new JSONObject(),
                                    requestResult,
                                    result;
        PiggyBackedBlockTemplate    blockTemplate;

        this.switchLogger();

        try
        {
            msg.put("method", "getblocktemplate");
            msg.put("id",     Integer.toString(this.getRequestId()));

            caps.put("capabilities", new JSONArray());
            params.put(caps);

            msg.put("params", params);

            requestResult = this.sendSecondaryPost(msg);

            if (!requestResult.isNull("error"))
                throw new RuntimeException("Block template retrieval failed: " + requestResult.get("error"));

            result          = requestResult.getJSONObject("result");
            blockTemplate   = new PiggyBackedBlockTemplate(this.getNetworkParams(), result);

            this.lastBlockTemplate = blockTemplate;

            return blockTemplate;
        }

        finally
        {
            this.resetLogger();
        }
    }

    @Override
    public boolean submitBlock(BlockTemplate blockTemplate, Block block)
    throws IOException, JSONException
    {
        boolean     wasSuccessful;
        JSONArray   params          = new JSONArray();
        JSONObject  message         = new JSONObject(),
                    result;

        this.switchLogger();

        try
        {
            message.put("method", "submitblock");
            message.put("id",     Integer.toString(this.getRequestId()));

            params.put(Hex.encodeHexString(block.bitcoinSerialize()));

            // Send across work ID if we have it
            if (blockTemplate instanceof PiggyBackedBlockTemplate)
            {
                String workId = ((PiggyBackedBlockTemplate)blockTemplate).getWorkId();

                if (workId != null)
                {
                    JSONObject  additionalParams = new JSONObject();

                    additionalParams.put("workid", workId);

                    params.put(additionalParams);
                }
            }

            message.put("params", params);

            result = this.sendSecondaryPost(message);

            wasSuccessful = (result.isNull("error") && result.isNull("result"));

            if (!wasSuccessful)
            {
                Logger logger;

                this.switchLogger();

                logger = this.getLogger();

                if (logger.isErrorEnabled())
                    logger.error("Block submit error:  " + result.toString());

                if (result.has("result") && result.getString("result").equals("unknown-work"))
                {
                    if (logger.isDebugEnabled())
                        logger.debug("Upstream pool reports work is unknown. Marking block template as stale.");

                    // Force refresh
                    this.lastBlockTemplate.setStale(true);
                }
            }

            return wasSuccessful;
        }

        finally
        {
            this.resetLogger();
        }
    }

    public JSONObject sendSecondaryPost(JSONObject post)
    throws IOException, JSONException
    {
        String response;

        response = this.sendSecondaryPost(this.getSecondaryUrl(), post.toString());

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
        this.switchLogger();

        try
        {
            return sendPost(new URL(url), this.secondUsername, this.secondPassword, postdata);
        }

        finally
        {
            this.resetLogger();
        }
    }

    protected void switchLogger()
    {
        this.setLogger(LOGGER);
    }
}