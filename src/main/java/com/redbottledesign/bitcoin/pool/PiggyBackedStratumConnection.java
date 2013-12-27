package com.redbottledesign.bitcoin.pool;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.Config;
import com.github.fireduck64.sockthing.bitcoin.BitcoinDaemonConnection;
import com.google.bitcoin.core.Block;

public class PiggyBackedStratumConnection
extends BitcoinDaemonConnection
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PiggyBackedStratumConnection.class);

    protected String stratumHost;
    protected int stratumPort;
    protected String stratumSession;
    protected String stratumUsername;
    protected String stratumPassword;

    public PiggyBackedStratumConnection(Config config)
    {
        super(config);
    }

    @Override
    public JSONObject getCurrentBlockTemplate()
    throws IOException, JSONException
    {
        return super.getCurrentBlockTemplate();
    }

    @Override
    public boolean submitBlock(Block block)
    throws IOException, JSONException
    {
        return super.submitBlock(block);
    }

    @Override
    protected void validateAndLoadConfig(Config config)
    {
        super.validateAndLoadConfig(config);

        config.require("piggy_back_pool_host");
        config.require("piggy_back_pool_port");
        config.require("piggy_back_pool_username");
        config.require("piggy_back_pool_password");

        this.stratumHost = config.get("piggy_back_pool_host");
        this.stratumPort = config.getInt("piggy_back_pool_port");
    }

    protected void checkConnection()
    {
        if (this.stratumSession == null)
            this.connect();
    }

    protected void connect()
    {
    }
}