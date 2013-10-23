package com.redbottledesign.bitcoin.pool.drupal;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.github.fireduck64.sockthing.Config;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.BalancesSummaryRequestor;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.BlockCreditRequestor;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.PayoutRequestor;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.PayoutsSummaryRequestor;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.RoundRequestor;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.SolvedBlockRequestor;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.WorkShareRequestor;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.WorkersSummaryRequestor;
import com.redbottledesign.drupal.User;
import com.redbottledesign.drupal.gson.SessionManager;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;
import com.redbottledesign.drupal.gson.requestor.UserRequestor;

public class DrupalSession
{
  private static final String CONFIG_VALUE_DRUPAL_SITE_URI = "drupal_site_uri";
  private static final String CONFIG_VALUE_DAEMON_USERNAME = "drupal_site_daemon_username";
  private static final String CONFIG_VALUE_DAEMON_PASSWORD = "drupal_site_daemon_password";

  private SessionManager sessionManager;

  private UserRequestor userRequestor;
  private WorkShareRequestor shareRequestor;
  private SolvedBlockRequestor blockRequestor;
  private RoundRequestor roundRequestor;
  private BlockCreditRequestor creditRequestor;
  private PayoutRequestor payoutRequestor;
  private PayoutsSummaryRequestor payoutsSummaryRequestor;
  private BalancesSummaryRequestor balancesRequestor;
  private WorkersSummaryRequestor workersRequestor;

  private User poolDaemonUser;

  public DrupalSession(Config config)
  {
    this.initializeSession(config);
  }

  public DrupalSession(URI drupalSiteUri, String userName, String password)
  {
    this.initializeSession(drupalSiteUri, userName, password);
  }

  public SessionManager getSessionManager()
  {
    this.assertInitialized();

    return this.sessionManager;
  }

  public UserRequestor getUserRequestor()
  {
    this.assertInitialized();

    return this.userRequestor;
  }

  public WorkShareRequestor getShareRequestor()
  {
    this.assertInitialized();

    return this.shareRequestor;
  }

  public SolvedBlockRequestor getBlockRequestor()
  {
    this.assertInitialized();

    return this.blockRequestor;
  }

  public RoundRequestor getRoundRequestor()
  {
    this.assertInitialized();

    return this.roundRequestor;
  }

  public BlockCreditRequestor getCreditRequestor()
  {
    return this.creditRequestor;
  }

  public PayoutRequestor getPayoutRequestor()
  {
    return this.payoutRequestor;
  }

  public PayoutsSummaryRequestor getPayoutsSummaryRequestor()
  {
    return this.payoutsSummaryRequestor;
  }

  public BalancesSummaryRequestor getBalancesRequestor()
  {
    return this.balancesRequestor;
  }

  public WorkersSummaryRequestor getWorkersRequestor()
  {
    return this.workersRequestor;
  }

  public User getPoolDaemonUser()
  {
    this.assertInitialized();

    return this.poolDaemonUser;
  }

  public boolean wasInitialized()
  {
    return (this.sessionManager != null);
  }

  protected void assertInitialized()
  {
    if (!this.wasInitialized())
      throw new IllegalStateException("Session must first be initialized by calling initializeSession().");
  }

  protected void initializeSession(Config config)
  {
    String  drupalSiteUri,
            daemonUserName,
            daemonPassword;
    URI     siteUri;

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

    this.initializeSession(siteUri, daemonUserName, daemonPassword);
  }

  protected void initializeSession(URI drupalSiteUri, String userName, String password)
  {
    if (this.wasInitialized())
      throw new IllegalStateException("Session has already been initialized.");

    this.sessionManager = new SessionManager(drupalSiteUri, userName, password);

    this.userRequestor            = new UserRequestor(this.sessionManager);
    this.shareRequestor           = new WorkShareRequestor(this.sessionManager);
    this.blockRequestor           = new SolvedBlockRequestor(this.sessionManager);
    this.roundRequestor           = new RoundRequestor(this.sessionManager);
    this.creditRequestor          = new BlockCreditRequestor(this.sessionManager);
    this.payoutRequestor          = new PayoutRequestor(this.sessionManager);
    this.payoutsSummaryRequestor  = new PayoutsSummaryRequestor(this.sessionManager);
    this.balancesRequestor        = new BalancesSummaryRequestor(this.sessionManager);
    this.workersRequestor         = new WorkersSummaryRequestor(this.sessionManager);

    try
    {
      this.poolDaemonUser = this.getUserRequestor().requestUserByUsername(userName);
    }

    catch (IOException | DrupalHttpException ex)
    {
      throw new RuntimeException("Failed to look-up pool daemon user account: " + ex.getMessage(), ex);
    }
  }
}