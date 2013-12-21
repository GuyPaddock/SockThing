package com.redbottledesign.bitcoin.pool.drupal.authentication;

import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.PoolUser;
import com.github.fireduck64.sockthing.authentication.AuthHandler;
import com.redbottledesign.bitcoin.pool.drupal.DrupalSession;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.WorkersSummaryRequestor;
import com.redbottledesign.bitcoin.pool.drupal.summary.WorkersSummary.UserWorkerSummary;
import com.redbottledesign.drupal.User;
import com.redbottledesign.drupal.gson.requestor.UserRequestor;

public class DrupalAuthHandler
implements AuthHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DrupalAuthHandler.class);

    private final WorkersSummaryRequestor workerRequestor;
    private final UserRequestor userRequestor;

    public DrupalAuthHandler(DrupalSession drupalSession)
    {
        this.workerRequestor = drupalSession.getWorkersRequestor();
        this.userRequestor   = drupalSession.getUserRequestor();
    }

    /**
     * Return PoolUser object if the user is legit. Return null if the user is
     * unknown/not allowed/incorrect
     */
    @Override
    public PoolUser authenticate(String userName, String password)
    {
        PoolUser result = null;

        try
        {
            StringTokenizer userNameTokenizer = new StringTokenizer(userName, ".");

            if (userNameTokenizer.countTokens() != 2)
            {
                if (LOGGER.isErrorEnabled())
                {
                    LOGGER.error(
                        String.format("Authentication failure - could not parse worker user name '%s'", userName));
                }
            }
            else
            {
                String              drupalUserName      = userNameTokenizer.nextToken().toLowerCase(),
                                    workerName          = userNameTokenizer.nextToken();
                UserWorkerSummary   drupalWorkerInfo    = this.workerRequestor.getUserWorkerSummary(drupalUserName, workerName);

                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Authenticating worker: " + drupalWorkerInfo);

                if (drupalWorkerInfo == null)
                {
                    LOGGER.error(
                        String.format(
                            "Authentication failure - worker not found for username '%s' and worker name '%s'",
                            drupalUserName,
                            workerName));
                }

                else
                {
                    String  drupalWorkerPassword    = drupalWorkerInfo.getWorkerPassword();
                    int     drupalWorkerDifficulty  = drupalWorkerInfo.getWorkerMinimumDifficulty();

                    if (((password == null) && (drupalWorkerPassword == null)) || password.equals(drupalWorkerPassword))
                    {
                        User drupalUser = this.userRequestor.requestUserByUid(drupalWorkerInfo.getUserId());

                        result = new DrupalPoolUser(drupalUser, workerName, drupalWorkerDifficulty);
                    }

                    else
                    {
                        LOGGER.error(
                            String.format(
                                "Authentication failure - bad worker password for username '%s' and worker name '%s'",
                                drupalUserName,
                                workerName));
                    }
                }
            }
        }

        catch (Exception ex)
        {
            if (LOGGER.isErrorEnabled())
            {
                LOGGER.error(
                    String.format(
                        "Failed to authenticate user '%s' with password '%s': %s",
                        userName,
                        password,
                        ex.getMessage()),
                    ex);
            }
        }

        return result;
    }
}
