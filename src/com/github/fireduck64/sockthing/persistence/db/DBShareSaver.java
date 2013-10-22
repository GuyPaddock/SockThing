package com.github.fireduck64.sockthing.persistence.db;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.Config;
import com.github.fireduck64.sockthing.PoolUser;
import com.github.fireduck64.sockthing.SubmitResult;
import com.github.fireduck64.sockthing.sharesaver.ShareSaveException;
import com.github.fireduck64.sockthing.sharesaver.ShareSaver;

class DBShareSaver
implements ShareSaver
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DBShareSaver.class);

    public DBShareSaver(Config config)
    throws java.sql.SQLException
    {
        config.require("share_db_driver");
        config.require("share_db_uri");
        config.require("share_db_username");
        config.require("share_db_password");

        DB.openConnectionPool(
            "share_db",
            config.get("share_db_driver"),
            config.get("share_db_uri"),
            config.get("share_db_username"),
            config.get("share_db_password"),
            64,
            16);

    }

    @Override
    public void saveShare(PoolUser pu, SubmitResult submitResult, String source, String uniqueJobString,
                          long blockReward, long feeTotal)
    throws ShareSaveException
    {

        Connection conn = null;

        try
        {
            conn = DB.openConnection("share_db");

/**
 *  rem_host varchar(128),        Pool host
    client varchar(128),          Client software
    username varchar(128),        Submitter
    our_result varchar(16),       Verified by pool
    upstream_result varchar(16),  Verified by network
    reason varchar(64),           Reason
    time timestamp default now(), Time submitted
    unique_id varchar(64),        Job hash
 */

            PreparedStatement ps = conn.prepareStatement("insert into shares (rem_host, username, our_result, upstream_result, reason, difficulty, hash, client, unique_id, block_difficulty, block_reward) values (?,?,?,?,?,?,?,?,?,?,?)");
            double block_difficulty = submitResult.getNetworkDifficulty();
            String reason_str = null;

            if (submitResult.getReason() != null)
            {
                reason_str = submitResult.getReason();

                if (reason_str.length() > 50)
                {
                    reason_str = reason_str.substring(0, 50);
                }

                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Reason: " + reason_str);
            }
            ps.setString(1, source);
            ps.setString(2, pu.getName());
            ps.setString(3, submitResult.getOurResult());
            ps.setString(4, submitResult.getUpstreamResult());
            ps.setString(5, reason_str);
            ps.setDouble(6, pu.getDifficulty());

            if (submitResult.getHash() != null)
            {
                ps.setString(7, submitResult.getHash().toString());
            }
            else
            {
                ps.setString(7, null);
            }
            ps.setString(8, submitResult.getClientVersion());

            ps.setString(9, uniqueJobString);
            ps.setDouble(10, block_difficulty);
            ps.setLong(11, blockReward);

            ps.execute();
            ps.close();

            if (submitResult.getUpstreamResult() != null
                && submitResult.getUpstreamResult().equals("Y")
                && submitResult.getHash() != null)
            {
                PreparedStatement blockps = conn.prepareStatement("insert into blocks (hash, difficulty, reward, height) values (?,?,?,?)");
                blockps.setString(1, submitResult.getHash().toString());
                blockps.setDouble(2, block_difficulty);
                blockps.setLong(3, blockReward);
                blockps.setInt(4, submitResult.getHeight());

                blockps.execute();
                blockps.close();

                //for(TransactionOutput out : priortx.getOutputs())

            }
        }

        catch (SQLIntegrityConstraintViolationException ex)
        {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug(
                    String.format(
                        "Duplicate save - calling good: %s\n%s",
                        ex.getMessage(),
                        ExceptionUtils.getStackTrace(ex)));
            }
        }

        catch (SQLException ex)
        {
            throw new ShareSaveException(ex);
        }

        finally
        {
            DB.safeClose(conn);
        }
    }
}