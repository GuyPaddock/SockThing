package com.redbottledesign.bitcoin.pool.drupal;

import java.net.URI;

import com.redbottledesign.drupal.gson.SessionManager;

public class SingletonSessionFactory
{
  private static final SingletonSessionFactory INSTANCE = new SingletonSessionFactory();

  private SessionManager sessionManager;

  public static SingletonSessionFactory getInstance()
  {
    return INSTANCE;
  }

  public boolean wasInitialized()
  {
    return (this.sessionManager != null);
  }

  public SessionManager getSessionManager()
  {
    if (!this.wasInitialized())
      throw new IllegalStateException("Session must first be initialized by calling initializeSession().");

    return this.sessionManager;
  }

  public void initializeSession(URI drupalSiteUri, String userName, String password)
  {
    if (this.wasInitialized())
      throw new IllegalStateException("Session has already been initialized.");

    this.sessionManager = new SessionManager(drupalSiteUri, userName, password);
  }
}
