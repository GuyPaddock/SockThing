
package com.github.fireduck64.sockthing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.persistence.db.DB;
import com.redbottledesign.bitcoin.pool.Agent;

/**
 * Optional database of witty remarks to be injected into Coinbase transactions
 *
 * Assumes using a table from the sharedb.
 */
public class WittyRemarksAgent
extends Agent
{
    private static final Logger LOGGER = LoggerFactory.getLogger(WittyRemarksAgent.class);

    public static final long DB_CHECK_MS = 120000L;

    private long last_check;
    private String last_remark;

    public WittyRemarksAgent()
    {
        this.setDaemon(true);
        this.setName("WittyRemarks");
    }

    public synchronized String getNextRemark()
    {
        this.notifyAll();

        return last_remark;
    }

    /**
     * Do the actual update in this thread to avoid ever blocking work generation
     */
    @Override
    public void run()
    {
        while(true)
        {
            try
            {
                this.updateLastRemark();

                synchronized(this)
                {
                    this.wait(DB_CHECK_MS / 4);
                }
            }

            catch (Throwable ex)
            {
                if (LOGGER.isErrorEnabled())
                {
                    LOGGER.error(
                        String.format("Error while running processing witty remarks: %s\n%s",
                        ex.getMessage(),
                        ExceptionUtils.getStackTrace(ex)));
                }
            }
        }
    }

    private void updateLastRemark()
    {
        if (System.currentTimeMillis() > last_check + DB_CHECK_MS)
        {
            Connection conn = null;

            try
            {
                conn = DB.openConnection("share_db");

                PreparedStatement ps = conn.prepareStatement("select * from witty_remarks where used=false order by order_id asc limit 1");
                ResultSet rs = ps.executeQuery();

                if (rs.next())
                {

                    last_remark = rs.getString("remark");
                    int order = rs.getInt("order_id");

                    if (LOGGER.isInfoEnabled())
                        LOGGER.info("Witty remark selected (" + order + ") - '" + last_remark + "'");
                }
                else
                {
                    if (LOGGER.isInfoEnabled())
                        LOGGER.info("No more witty remarks");

                    last_remark=null;
                }

                rs.close();
                ps.close();


                last_check=System.currentTimeMillis();
            }

            catch (SQLException ex)
            {
                if (LOGGER.isErrorEnabled())
                {
                    LOGGER.error(
                        String.format(
                            "Error getting remark: %s\n%s",
                            ex.getMessage(),
                            ExceptionUtils.getStackTrace(ex)));
                }
            }

            finally
            {
                DB.safeClose(conn);
            }
        }
    }

    public void markUsed(String remark)
    {
        Connection conn = null;

        try
        {
            conn = DB.openConnection("share_db");

            PreparedStatement ps = conn.prepareStatement("update witty_remarks set used=true where remark=?");
            ps.setString(1, remark);
            ps.execute();
            ps.close();

        }

        catch (SQLException ex)
        {
            if (LOGGER.isErrorEnabled())
            {
                LOGGER.error(
                    String.format(
                        "Failed to mark remark as no longer remarkable: %s\n%s",
                        ex.getMessage(),
                        ExceptionUtils.getStackTrace(ex)));
            }
        }

        finally
        {
            DB.safeClose(conn);
        }

        synchronized (this)
        {
            last_check = 0L;

            this.notifyAll();
        }
    }

    @Override
    protected void runPeriodicTask()
    throws Exception
    {
        // TODO Auto-generated method stub

    }
}
