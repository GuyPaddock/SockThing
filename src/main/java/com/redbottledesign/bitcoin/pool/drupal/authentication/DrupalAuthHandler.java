package com.redbottledesign.bitcoin.pool.drupal.authentication;

import java.util.StringTokenizer;

import com.github.fireduck64.sockthing.PoolUser;
import com.github.fireduck64.sockthing.StratumServer;
import com.github.fireduck64.sockthing.authentication.AuthHandler;
import com.redbottledesign.bitcoin.pool.drupal.DrupalSession;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.WorkersSummaryRequestor;
import com.redbottledesign.bitcoin.pool.drupal.summary.WorkersSummary.UserWorkerSummary;
import com.redbottledesign.drupal.User;
import com.redbottledesign.drupal.gson.requestor.UserRequestor;

public class DrupalAuthHandler
implements AuthHandler
{
  private final WorkersSummaryRequestor workerRequestor;
  private final UserRequestor userRequestor;

  public DrupalAuthHandler(StratumServer server)
  {
    DrupalSession drupalSession = server.getSession();

    this.workerRequestor  = drupalSession.getWorkersRequestor();
    this.userRequestor    = drupalSession.getUserRequestor();
  }

  /**
   * Return PoolUser object if the user is legit.
   * Return null if the user is unknown/not allowed/incorrect
   */
  @Override
  public PoolUser authenticate(String userName, String password)
  {
    PoolUser result = null;

    try
    {
      StringTokenizer userNameTokenizer = new StringTokenizer(userName, ".");

      if (userNameTokenizer.countTokens() == 2)
      {
        String            drupalUserName    = userNameTokenizer.nextToken().toLowerCase(),
                          workerName        = userNameTokenizer.nextToken();
        UserWorkerSummary drupalWorkerInfo  = this.workerRequestor.getUserWorkerSummary(drupalUserName, workerName);

        if (drupalWorkerInfo != null)
        {
          String  drupalWorkerPassword    = drupalWorkerInfo.getWorkerPassword();
          int     drupalWorkerDifficulty  = drupalWorkerInfo.getWorkerMinimumDifficulty();

          if (((password == null) && (drupalWorkerPassword == null)) || password.equals(drupalWorkerPassword))
          {
            User drupalUser = this.userRequestor.requestUserByUid(drupalWorkerInfo.getUserId());

            result = new DrupalPoolUser(drupalUser, workerName, drupalWorkerDifficulty);
          }
        }
      }
    }

    catch (Exception ex)
    {
      ex.printStackTrace();
    }

    return result;
  }
}
