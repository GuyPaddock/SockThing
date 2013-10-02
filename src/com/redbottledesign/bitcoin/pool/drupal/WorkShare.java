package com.redbottledesign.bitcoin.pool.drupal;

import java.util.Date;

import com.google.gson.annotations.SerializedName;
import com.redbottledesign.drupal.Node;
import com.redbottledesign.drupal.User;

public class WorkShare
extends Node
{
  private static final String CONTENT_TYPE = "share";

  public static final String DRUPAL_FIELD_BLOCK = "field_share_block";
  public static final String JAVA_FIELD_BLOCK = "block";

  public static final String DRUPAL_FIELD_ROUND = "field_share_round";
  public static final String JAVA_FIELD_ROUND = "round";

  public static final String DRUPAL_FIELD_SUBMITTER = "field_share_submitter";
  public static final String JAVA_FIELD_SUBMITTER = "submitter";

  public static final String DRUPAL_FIELD_DATE_SUBMITTED = "field_share_time_submitted";
  public static final String JAVA_FIELD_SUBMISSION_TIME = "dateSubmitted";

  public static final String DRUPAL_FIELD_CLIENT_SOFTWARE_VERSION = "field_share_client_version";
  public static final String JAVA_FIELD_CLIENT_SOFTWARE_VERSION = "clientSoftwareVersion";

  public static final String DRUPAL_FIELD_POOL_HOST = "field_share_pool_host";
  public static final String JAVA_FIELD_POOL_HOST = "poolHost";

  public static final String DRUPAL_FIELD_VERIFIED_BY_POOL = "field_share_verified_by_pool";
  public static final String JAVA_FIELD_VERIFIED_BY_POOL = "verifiedByPool";

  public static final String DRUPAL_FIELD_VERIFIED_BY_NETWORK = "field_share_verified_by_network";
  public static final String JAVA_FIELD_VERIFIED_BY_NETWORK = "verifiedByNetwork";

  public static final String DRUPAL_FIELD_STATUS = "field_share_status";
  public static final String JAVA_FIELD_STATUS = "status";

  @SerializedName(DRUPAL_FIELD_BLOCK)
  private Node.Reference block;

  @SerializedName(DRUPAL_FIELD_ROUND)
  private Node.Reference round;

  @SerializedName(DRUPAL_FIELD_SUBMITTER)
  private User.Reference submitter;

  @SerializedName(DRUPAL_FIELD_DATE_SUBMITTED)
  private Date dateSubmitted;

  @SerializedName(DRUPAL_FIELD_CLIENT_SOFTWARE_VERSION)
  private String clientSoftwareVersion;

  @SerializedName(DRUPAL_FIELD_POOL_HOST)
  private String poolHost;

  @SerializedName(DRUPAL_FIELD_VERIFIED_BY_POOL)
  private boolean verifiedByPool;

  @SerializedName(DRUPAL_FIELD_VERIFIED_BY_NETWORK)
  private boolean verifiedByNetwork;

  @SerializedName(DRUPAL_FIELD_STATUS)
  private String status;

  public WorkShare()
  {
    super(CONTENT_TYPE);
  }

  public String getJobHash()
  {
    return this.getTitle();
  }

  public void setJobHash(String jobHash)
  {
    this.setTitle(jobHash);
  }

  public Node.Reference getBlock()
  {
    return this.block;
  }

  public void setBlock(Node.Reference block)
  {
    this.block = block;
  }

  public Node.Reference getRound()
  {
    return this.round;
  }

  public void setRound(Node.Reference round)
  {
    this.round = round;
  }

  public User.Reference getSubmitter()
  {
    return this.submitter;
  }

  public void setSubmitter(User.Reference submitter)
  {
    this.submitter = submitter;
  }

  public Date getDateSubmitted()
  {
    return this.dateSubmitted;
  }

  public void setDateSubmitted(Date dateSubmitted)
  {
    this.dateSubmitted = dateSubmitted;
  }

  public String getClientSoftwareVersion()
  {
    return this.clientSoftwareVersion;
  }

  public void setClientSoftwareVersion(String clientSoftwareVersion)
  {
    this.clientSoftwareVersion = clientSoftwareVersion;
  }

  public String getPoolHost()
  {
    return this.poolHost;
  }

  public void setPoolHost(String poolHost)
  {
    this.poolHost = poolHost;
  }

  public boolean isVerifiedByPool()
  {
    return this.verifiedByPool;
  }

  public void setVerifiedByPool(boolean verifiedByPool)
  {
    this.verifiedByPool = verifiedByPool;
  }

  public boolean isVerifiedByNetwork()
  {
    return this.verifiedByNetwork;
  }

  public void setVerifiedByNetwork(boolean verifiedByNetwork)
  {
    this.verifiedByNetwork = verifiedByNetwork;
  }

  public String getStatus()
  {
    return this.status;
  }

  public void setStatus(String status)
  {
    this.status = status;
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName()                        + " [" +
           "id="                    + this.getId()                + ", " +
           "url="                   + this.getUrl()               + ", " +
           "revisionId="            + this.getRevisionId()        + ", " +
           "jobHash="               + this.getJobHash()           + ", " +
           "block="                 + this.block                  + ", " +
           "round="                 + this.round                  + ", " +
           "submitter="             + this.submitter              + ", " +
           "clientSoftwareVersion=" + this.clientSoftwareVersion  + ", " +
           "poolHost="              + this.poolHost               + ", " +
           "verifiedByPool="        + this.verifiedByPool         + ", " +
           "verifiedByNetwork="     + this.verifiedByNetwork      + ", " +
           "status="                + this.status                 + ", " +
           "published="             + this.isPublished()          + ", " +
           "dateSubmitted="         + this.dateSubmitted          + ", " +
           "dateCreated="           + this.getDateCreated()       + ", " +
           "dateChanged="           + this.getDateChanged()       + ", " +
           "]";
  }
}