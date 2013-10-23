package com.github.fireduck64.sockthing.authentication;

import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.Config;
import com.github.fireduck64.sockthing.PoolUser;
import com.github.fireduck64.sockthing.StratumServer;
import com.google.bitcoin.core.Address;

public class AddressDifficultyAuthHandler implements AuthHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AddressDifficultyAuthHandler.class);

    protected StratumServer server;
    private int default_difficulty;

    public AddressDifficultyAuthHandler(StratumServer server)
    {
        this.server = server;

        Config config = server.getConfig();

        //if (config.get("default_difficulty") != null && !config.get("default_difficulty").isEmpty())
        if (config.isSet("default_difficulty"))
        {
            int diff = config.getInt("default_difficulty");

            if (diff < 1 || diff > 65536)
            {
                default_difficulty = 32;

                if (LOGGER.isErrorEnabled())
                {
                    LOGGER.error(
                        String.format(
                            "Config default_difficulty %s invalid. Setting default difficulty to %d.",
                            diff,
                            default_difficulty));
                }
            }
            else
            {
                default_difficulty = diff;

                if (LOGGER.isErrorEnabled())
                    LOGGER.error(String.format("Config default_difficulty found. Setting to %d.", diff));
            }
        }

        else
        {
            default_difficulty = 32;

            if (LOGGER.isErrorEnabled())
            {
                LOGGER.error(
                    String.format(
                        "Config default_difficulty not found. Setting default difficulty to %d.",
                        default_difficulty));
            }
        }
    }

    /**
     * Return PoolUser object if the user is legit.
     * Return null if the user is unknown/not allowed/incorrect
     */
    @Override
    public PoolUser authenticate(String username, String password)
    {
        PoolUser pu = new PoolUser(username);
        StringTokenizer stok = new StringTokenizer(username, "_");

        if (stok.countTokens()==2)
        {
            String  addr = stok.nextToken();
            int     diff = Integer.parseInt(stok.nextToken());

            pu.setName(addr);
            pu.setDifficulty(diff);

            if (!checkAddress(addr))
                return null;

            if (diff < 1)
                return null;

            if (diff > 65536)
                return null;

            return pu;
        }

        if (stok.countTokens() == 1)
        {
            String addr = stok.nextToken();

            pu.setName(addr);
            pu.setDifficulty(default_difficulty);

            if (!checkAddress(addr))
                return null;

            return pu;
        }
        return null;
    }

    public boolean checkAddress(String addr)
    {
        try
        {
            Address a = new Address(server.getNetworkParameters(), addr);

            return true;
        }
        catch(Exception e)
        {
            return false;
        }
    }
}