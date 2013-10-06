package com.redbottledesign.bitcoin.pool.drupal;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import com.github.fireduck64.sockthing.Config;
import com.github.fireduck64.sockthing.PoolUser;
import com.github.fireduck64.sockthing.StratumServer;
import com.github.fireduck64.sockthing.SubmitResult;
import com.github.fireduck64.sockthing.sharesaver.ShareSaveException;
import com.github.fireduck64.sockthing.sharesaver.ShareSaver;
import com.redbottledesign.bitcoin.pool.drupal.node.SolvedBlock;
import com.redbottledesign.bitcoin.pool.drupal.node.WorkShare;
import com.redbottledesign.drupal.Node;
import com.redbottledesign.drupal.User;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;

public class DrupalShareSaver
implements ShareSaver
{
  private static final String CONFIRM_YES = "Y";

  private static final User.Reference TEST_USER = new User.Reference(16);
  private static final Node.Reference TEST_REMARK = new Node.Reference(11);

  private static final String CONFIG_VALUE_DRUPAL_SITE_URI = "drupal_site_uri";
  private static final String CONFIG_VALUE_DAEMON_USERNAME = "drupal_site_daemon_username";
  private static final String CONFIG_VALUE_DAEMON_PASSWORD = "drupal_site_daemon_password";

  private static final int SATOSHIS_PER_BITCOIN = 100000000;

  private final StratumServer server;
  private final SingletonDrupalSessionFactory sessionFactory;
  private User poolDaemonUser;

  public DrupalShareSaver(Config config, StratumServer server)
  {
    this.server         = server;
    this.sessionFactory = SingletonDrupalSessionFactory.getInstance();

    this.initialize(config);
  }

  @Override
  public void saveShare(PoolUser pu, SubmitResult submitResult, String source, String uniqueJobString, Long blockReward)
  throws ShareSaveException
  {
    RoundAgent      roundAgent            = this.server.getRoundAgent();
    Node.Reference  currentRoundReference = roundAgent.getCurrentRoundSynchronized().asReference();
    WorkShare       newShare              = new WorkShare();
    String          statusString          = null;
    Node.Reference  solvedBlockReference  = null;
    User.Reference  daemonUserReference   = this.poolDaemonUser.asReference();
    double          blockDifficulty       = submitResult.getNetworkDifficulty(),
                    workDifficulty        = submitResult.getOurDifficulty();

    System.out.println(blockDifficulty + " " + blockReward);

    if (CONFIRM_YES.equals(submitResult.getUpstreamResult()) && (submitResult.getHash() != null))
    {
      SolvedBlock newBlock = new SolvedBlock();

      newBlock.setAuthor(daemonUserReference);
      newBlock.setHash(submitResult.getHash().toString());
      newBlock.setHeight(submitResult.getHeight());
      newBlock.setStatus(SolvedBlock.Status.UNCONFIRMED);
      newBlock.setCreationTime(new Date());
      newBlock.setDifficulty(blockDifficulty);
      newBlock.setRound(currentRoundReference);
      newBlock.setReward(BigDecimal.valueOf(blockReward).divide(BigDecimal.valueOf(SATOSHIS_PER_BITCOIN)));
      newBlock.setSolvingMember(TEST_USER);
      newBlock.setWittyRemark(TEST_REMARK);

      try
      {
        this.sessionFactory.getBlockRequestor().createNode(newBlock);
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

    newShare.setAuthor(daemonUserReference);
    newShare.setJobHash(uniqueJobString);
    newShare.setRound(currentRoundReference);
    newShare.setShareDifficulty(workDifficulty);
    newShare.setSubmitter(TEST_USER);
    newShare.setDateSubmitted(new Date());
    newShare.setClientSoftwareVersion(submitResult.getClientVersion());
    newShare.setPoolHost(source);
    newShare.setVerifiedByPool(CONFIRM_YES.equals(submitResult.getOurResult()));
    newShare.setVerifiedByNetwork(CONFIRM_YES.equals(submitResult.getUpstreamResult()));
    newShare.setStatus(statusString);
    newShare.setBlock(solvedBlockReference);

    try
    {
      this.sessionFactory.getShareRequestor().createNode(newShare);
    }

    catch (IOException | DrupalHttpException ex)
    {
      throw new ShareSaveException(ex);
    }
  }

  protected void initialize(Config config)
  {
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

    this.sessionFactory.initializeSession(siteUri, daemonUserName, daemonPassword);

    this.poolDaemonUser = this.sessionFactory.getPoolDaemonUser();
  }
}
