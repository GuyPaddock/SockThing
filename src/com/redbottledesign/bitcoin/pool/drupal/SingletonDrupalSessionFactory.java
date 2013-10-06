package com.redbottledesign.bitcoin.pool.drupal;

import java.io.IOException;
import java.net.URI;

import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.BlockCreditRequestor;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.PayoutsSummaryRequestor;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.RoundRequestor;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.SolvedBlockRequestor;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.WorkShareRequestor;
import com.redbottledesign.drupal.User;
import com.redbottledesign.drupal.gson.SessionManager;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;
import com.redbottledesign.drupal.gson.requestor.UserRequestor;

public class SingletonDrupalSessionFactory
{
  private static final SingletonDrupalSessionFactory INSTANCE = new SingletonDrupalSessionFactory();

  private SessionManager sessionManager;

  private UserRequestor userRequestor;
  private WorkShareRequestor shareRequestor;
  private SolvedBlockRequestor blockRequestor;
  private RoundRequestor roundRequestor;
  private PayoutsSummaryRequestor payoutsRequestor;
  private BlockCreditRequestor creditRequestor;

  private User poolDaemonUser;

  public static SingletonDrupalSessionFactory getInstance()
  {
    return INSTANCE;
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

  public PayoutsSummaryRequestor getPayoutsRequestor()
  {
    return this.payoutsRequestor;
  }

  public BlockCreditRequestor getCreditRequestor()
  {
    return this.creditRequestor;
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

  public void initializeSession(URI drupalSiteUri, String userName, String password)
  {
    if (this.wasInitialized())
      throw new IllegalStateException("Session has already been initialized.");

    this.sessionManager = new SessionManager(drupalSiteUri, userName, password);

    this.userRequestor    = new UserRequestor(this.sessionManager);
    this.shareRequestor   = new WorkShareRequestor(this.sessionManager);
    this.blockRequestor   = new SolvedBlockRequestor(this.sessionManager);
    this.roundRequestor   = new RoundRequestor(this.sessionManager);
    this.payoutsRequestor = new PayoutsSummaryRequestor(this.sessionManager);
    this.creditRequestor  = new BlockCreditRequestor(this.sessionManager);

    try
    {
      this.poolDaemonUser = this.getUserRequestor().requestUserByUsername(userName);
    }

    catch (IOException | DrupalHttpException ex)
    {
      throw new RuntimeException("Failed to look-up pool daemon user account: " + ex.getMessage(), ex);
    }
  }

  protected void assertInitialized()
  {
    if (!this.wasInitialized())
      throw new IllegalStateException("Session must first be initialized by calling initializeSession().");
  }
}