package com.github.fireduck64.sockthing.shares;

import com.github.fireduck64.sockthing.PoolUser;
import com.github.fireduck64.sockthing.ShareSaveException;
import com.github.fireduck64.sockthing.SubmitResult;
import com.google.bitcoin.core.Sha256Hash;


/**
 * This interface is for saving credit for a user after the worker submits a work unit
 */
public interface ShareSaver
{
    /**
     * The unique_job_string is what this share unit will be deduped on.  It must be unique for each valid submit.
     */
    public void saveShare(PoolUser pu, SubmitResult submit_result, String source, String unique_job_string, Double block_difficulty, Long block_reward) throws ShareSaveException;

}

