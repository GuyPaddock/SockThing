package com.github.fireduck64.sockthing;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

import org.spongycastle.util.Arrays;

public class UserSessionData
{
    private static final int EXTRANONCE_BYTES = 4;

    private static AtomicLong nextExtraNonce1;

    static
    {
        nextExtraNonce1 = new AtomicLong(0);
    }

    /**
     * Hopefully the user is submitting shares and this keeping the interesting
     * jobs in memory.
     */
    private final LRUCache<String, JobInfo> open_jobs = new LRUCache<String, JobInfo>(25);

    private final AtomicLong next_job_id = new AtomicLong(0);
    private final String job_session_str;
    private final StratumServer server;

    public UserSessionData(StratumServer server)
    {
        this.server = server;
        Random rnd = new Random();

        long session_id = Math.abs(rnd.nextInt());

        job_session_str = Long.toString(session_id, 16);
    }

    public JobInfo getJobInfo(String job_id)
    {
        synchronized(open_jobs)
        {
            return open_jobs.get(job_id);
        }
    }

    public String getNextJobId()
    {
        return "job_" + job_session_str +"_" + next_job_id.getAndIncrement();
    }

    public void saveJobInfo(String job_id, JobInfo ji)
    {
        synchronized(open_jobs)
        {
            open_jobs.put(job_id, ji);
        }
    }

    public void clearAllJobs()
    {
        this.open_jobs.clear();
    }

    /**
     * Always use the same one to make reconnects work right
     * Instead we get protection from using a random number in each coinbase
     * which is unique to each job
     */
    public static byte[] getExtranonce1()
    {
        byte[] extraNonce1Bytes = Long.toString(nextExtraNonce1.get()).getBytes();

        return Arrays.copyOfRange(extraNonce1Bytes, 0, EXTRANONCE_BYTES);

//        return "SOCK".getBytes();
    }

    /**
     * Prune the jobs in this user session data
     *
     * Return true if no connections have referenced this in a while
     */
    public boolean prune()
        throws org.json.JSONException, java.io.IOException
    {
        TreeSet<String> to_delete =new TreeSet<String>();
        long current_block_height = server.getCurrentBlockTemplate().getHeight();

        synchronized(open_jobs)
        {
            if (open_jobs.size() == 0) return true;

            for(Map.Entry<String, JobInfo> me : open_jobs.entrySet())
            {
                JobInfo ji = me.getValue();

                if ((ji.getBlockHeight() + 1) < current_block_height)
                {
                    to_delete.add(me.getKey());
                }
            }
            for(String job_id : to_delete)
            {
                open_jobs.remove(job_id);
            }

        }
        return false;

    }

    public int getJobCount()
    {
        synchronized(open_jobs)
        {
            return open_jobs.size();
        }
    }

}
