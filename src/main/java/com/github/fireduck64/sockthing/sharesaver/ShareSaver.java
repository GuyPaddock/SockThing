package com.github.fireduck64.sockthing.sharesaver;

import java.math.BigInteger;

import com.github.fireduck64.sockthing.PoolUser;
import com.github.fireduck64.sockthing.SubmitResult;

/**
 * This interface is for saving credit for a user after the worker submits a work unit
 */
public interface ShareSaver
{
    /**
     * The unique_job_string is what this share unit will be deduped on.  It must be unique for each valid submit.
     */
    public void saveShare(PoolUser pu, SubmitResult submitResult, String source, String uniqueJobString,
                          BigInteger blockReward, BigInteger feeTotal)
    throws ShareSaveException;
}