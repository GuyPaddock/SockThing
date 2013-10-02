package com.github.fireduck64.sockthing.persistence.db;
import java.sql.Connection;
import java.sql.PreparedStatement;

import com.github.fireduck64.sockthing.Config;
import com.github.fireduck64.sockthing.PoolUser;
import com.github.fireduck64.sockthing.SubmitResult;
import com.github.fireduck64.sockthing.sharesaver.ShareSaveException;
import com.github.fireduck64.sockthing.sharesaver.ShareSaver;

// import org.json.JSONObject;

public class DBShareSaver implements ShareSaver
{
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
    public void saveShare(PoolUser pu, SubmitResult submit_result, String source, String unique_job_string, Double block_difficulty, Long block_reward) throws ShareSaveException
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

            String reason_str = null;
            if (submit_result.getReason() != null)
            {
                reason_str = submit_result.getReason();
                if (reason_str.length() > 50)
                {
                    reason_str = reason_str.substring(0, 50);
                }
                System.out.println("Reason: " + reason_str);
            }
            ps.setString(1, source);
            ps.setString(2, pu.getName());
            ps.setString(3, submit_result.getOurResult());
            ps.setString(4, submit_result.getUpstreamResult());
            ps.setString(5, reason_str);
            ps.setDouble(6, pu.getDifficulty());

            if (submit_result.getHash() != null)
            {
                ps.setString(7, submit_result.getHash().toString());
            }
            else
            {
                ps.setString(7, null);
            }
            ps.setString(8, submit_result.getClientVersion());

            ps.setString(9, unique_job_string);
            ps.setDouble(10, block_difficulty);
            ps.setLong(11, block_reward);

            ps.execute();
            ps.close();

            if (submit_result.getUpstreamResult() != null
                && submit_result.getUpstreamResult().equals("Y")
                && submit_result.getHash() != null)
            {
                PreparedStatement blockps = conn.prepareStatement("insert into blocks (hash, difficulty, reward, height) values (?,?,?,?)");
                blockps.setString(1, submit_result.getHash().toString());
                blockps.setDouble(2, block_difficulty);
                blockps.setLong(3, block_reward);
                blockps.setInt(4, submit_result.getHeight());

                blockps.execute();
                blockps.close();

                //for(TransactionOutput out : priortx.getOutputs())

            }
        }
        catch(java.sql.SQLIntegrityConstraintViolationException e)
        {
            System.out.println("Duplicate save - calling good");
        }
        catch(java.sql.SQLException e)
        {
            throw new ShareSaveException(e);
        }
        finally
        {
            DB.safeClose(conn);
        }

    }

}
