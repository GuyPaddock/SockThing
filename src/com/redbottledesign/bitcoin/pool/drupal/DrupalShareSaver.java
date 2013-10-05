package com.redbottledesign.bitcoin.pool.drupal;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import com.github.fireduck64.sockthing.Config;
import com.github.fireduck64.sockthing.PoolUser;
import com.github.fireduck64.sockthing.SubmitResult;
import com.github.fireduck64.sockthing.sharesaver.ShareSaveException;
import com.github.fireduck64.sockthing.sharesaver.ShareSaver;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.SolvedBlockRequestor;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.WorkShareRequestor;
import com.redbottledesign.drupal.Node;
import com.redbottledesign.drupal.User;
import com.redbottledesign.drupal.gson.SessionManager;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;
import com.redbottledesign.drupal.gson.requestor.UserRequestor;

public class DrupalShareSaver
implements ShareSaver
{
  private static final User.Reference TEST_USER = new User.Reference(16);
  private static final Node.Reference TEST_ROUND = new Node.Reference(26);
  private static final Node.Reference TEST_REMARK = new Node.Reference(11);

  private static final String CONFIG_VALUE_DRUPAL_SITE_URI = "drupal_site_uri";
  private static final String CONFIG_VALUE_DAEMON_USERNAME = "drupal_site_daemon_username";
  private static final String CONFIG_VALUE_DAEMON_PASSWORD = "drupal_site_daemon_password";

  private UserRequestor userRequestor;
  private WorkShareRequestor shareRequestor;
  private SolvedBlockRequestor blockRequestor;

  private User poolDaemonUser;

  public DrupalShareSaver(Config config)
  {
    this.initialize(config);
  }

  @Override
  public void saveShare(PoolUser pu, SubmitResult submitResult, String source, String uniqueJobString,
                        Double blockDifficulty, Long blockReward)
  throws ShareSaveException
  {
    WorkShare       newShare              = new WorkShare();
    String          statusString          = null;
    Node.Reference  solvedBlockReference  = null;

    if ("Y".equals(submitResult.getUpstreamResult()) && (submitResult.getHash() != null))
    {
      SolvedBlock newBlock = new SolvedBlock();

      newBlock.setHash(submitResult.getHash().toString());
      newBlock.setHeight(submitResult.getHeight());
      newBlock.setStatus(SolvedBlock.Status.UNCONFIRMED);
      newBlock.setCreationTime(new Date());
      newBlock.setDifficulty(blockDifficulty);
      newBlock.setRound(TEST_ROUND);
      newBlock.setReward(BigDecimal.valueOf(blockReward));
      newBlock.setSolvingMember(TEST_USER);
      newBlock.setWittyRemark(TEST_REMARK);

      try
      {
        this.blockRequestor.createNode(newBlock);
      }

      catch (IOException | DrupalHttpException ex)
      {
        throw new ShareSaveException(ex);
      }

      solvedBlockReference = newBlock.asReference();
    }

    if (submitResult.getReason() != null)
    {
      statusString = submitResult.getReason();

      if (statusString.length() > 50)
        statusString = statusString.substring(0, 50);

      System.out.println("Reason: " + statusString);
    }

    newShare.setAuthor(this.poolDaemonUser.asReference());
    newShare.setJobHash(uniqueJobString);
    newShare.setBlock(solvedBlockReference);
    newShare.setRound(TEST_ROUND);
    newShare.setSubmitter(TEST_USER);
    newShare.setDateSubmitted(new Date());
    newShare.setClientSoftwareVersion(submitResult.getClientVersion());
    newShare.setPoolHost(source);
    newShare.setVerifiedByPool("Y".equals(submitResult.getOurResult()));
    newShare.setVerifiedByNetwork("Y".equals(submitResult.getUpstreamResult()));
    newShare.setStatus(statusString);

    try
    {
      this.shareRequestor.createNode(newShare);
    }

    catch (IOException | DrupalHttpException ex)
    {
      throw new ShareSaveException(ex);
    }
  }

  protected void initialize(Config config)
  {
    SingletonSessionFactory sessionFactory = SingletonSessionFactory.getInstance();
    SessionManager          sessionManager;
    String                  drupalSiteUri,
                            daemonUserName,
                            daemonPassword;
    URI                     siteUri;

    config.require(CONFIG_VALUE_DRUPAL_SITE_URI);
    config.require(CONFIG_VALUE_DAEMON_USERNAME);
    config.require(CONFIG_VALUE_DAEMON_PASSWORD);

    drupalSiteUri   = config.get(CONFIG_VALUE_DRUPAL_SITE_URI);
    daemonUserName  = config.get(CONFIG_VALUE_DAEMON_USERNAME);
    daemonPassword  = config.get(CONFIG_VALUE_DAEMON_PASSWORD);

    try
    {
      siteUri = new URI(drupalSiteUri);
    }

    catch (URISyntaxException ex)
    {
      throw new RuntimeException("Invalid Drupal site URI: " + drupalSiteUri, ex);
    }

    sessionFactory.initializeSession(siteUri, daemonUserName, daemonPassword);

    sessionManager = sessionFactory.getSessionManager();

    this.userRequestor  = new UserRequestor(sessionManager);
    this.shareRequestor = new WorkShareRequestor(sessionManager);
    this.blockRequestor = new SolvedBlockRequestor(sessionManager);

    try
    {
      this.poolDaemonUser = userRequestor.requestUserByUsername(daemonUserName);
    }

    catch (IOException | DrupalHttpException ex)
    {
      throw new RuntimeException("Failed to look-up pool daemon user account: " + ex.getMessage(), ex);
    }
  }
}
