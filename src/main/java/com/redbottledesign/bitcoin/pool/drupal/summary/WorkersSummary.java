package com.redbottledesign.bitcoin.pool.drupal.summary;

import com.google.gson.annotations.SerializedName;

public class WorkersSummary
{
  public static final String DRUPAL_FIELD_WORKERS = "workers";
  public static final String JAVA_FIELD_WORKERS = "workers";

  @SerializedName(DRUPAL_FIELD_WORKERS)
  private UserWorkerSummary[] workers;

  public UserWorkerSummary[] getWorkers()
  {
    return this.workers;
  }

  protected void setWorkers(UserWorkerSummary[] workers)
  {
    this.workers = workers;
  }

  public static class UserWorkerSummary
  {
    public static final String DRUPAL_FIELD_USER_ID = "uid";
    public static final String JAVA_FIELD_USER_ID = "userId";

    public static final String DRUPAL_FIELD_USER_NAME = "user_name";
    public static final String JAVA_FIELD_USER_NAME = "userName";

    public static final String DRUPAL_FIELD_WORKER_NAME = "worker_name";
    public static final String JAVA_FIELD_WORKER_NAME = "workerName";

    public static final String DRUPAL_FIELD_WORKER_PASSWORD = "worker_password";
    public static final String JAVA_FIELD_WORKER_PASSWORD = "workerPassword";

    public static final String DRUPAL_FIELD_WORKER_MINIMUM_DIFFICULTY = "worker_minimum_difficulty";
    public static final String JAVA_FIELD_WORKER_MINIMUM_DIFFICULTY = "workerMinimimumDifficulty";

    @SerializedName(DRUPAL_FIELD_USER_ID)
    private int userId;

    @SerializedName(DRUPAL_FIELD_USER_NAME)
    private String userName;

    @SerializedName(DRUPAL_FIELD_WORKER_NAME)
    private String workerName;

    @SerializedName(DRUPAL_FIELD_WORKER_PASSWORD)
    private String workerPassword;

    @SerializedName(DRUPAL_FIELD_WORKER_MINIMUM_DIFFICULTY)
    private int workerMinimumDifficulty;

    public int getUserId()
    {
      return this.userId;
    }

    public String getUserName()
    {
      return this.userName;
    }

    public String getWorkerName()
    {
      return this.workerName;
    }

    public String getWorkerPassword()
    {
      return this.workerPassword;
    }

    public int getWorkerMinimumDifficulty()
    {
      return this.workerMinimumDifficulty;
    }

    protected void setUserId(int userId)
    {
      this.userId = userId;
    }

    protected void setUserName(String userName)
    {
      this.userName = userName;
    }

    protected void setWorkerName(String workerName)
    {
      this.workerName = workerName;
    }

    protected void setWorkerPassword(String workerPassword)
    {
      this.workerPassword = workerPassword;
    }

    protected void setWorkerMinimumDifficulty(int workerMinimumDifficulty)
    {
      this.workerMinimumDifficulty = workerMinimumDifficulty;
    }

    @Override
    public String toString()
    {
      return  this.getClass().getSimpleName()                       + " [" +
              "userId="                   + userId                  + ", " +
              "userName="                 + userName                + ", " +
              "workerName="               + workerName              + ", " +
          		"workerPassword="           + workerPassword          + ", " +
      				"workerMinimumDifficulty="  + workerMinimumDifficulty + "]";
    }


  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName()  + " [" +
           "workers=" + this.workers        + "]";
  }
}
