package com.redbottledesign.bitcoin.pool.drupal.node;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.google.gson.annotations.SerializedName;
import com.redbottledesign.drupal.DateRange;
import com.redbottledesign.drupal.Node;

public class Round
extends Node
{
  public static final String CONTENT_TYPE = "round";

  public static final String DRUPAL_FIELD_ROUND_STATUS = "field_round_status";
  public static final String JAVA_FIELD_ROUND_STATUS = "roundStatus";

  public static final String DRUPAL_FIELD_ROUND_DATES = "field_round_start_end";
  public static final String JAVA_FIELD_ROUND_DATES = "roundDates";

  // FIXME: Move these to config
  public static final int MAX_OPEN_ROUNDS = 12;
  private static final long MAX_ROUND_LENGTH_MS = TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES);

  @SerializedName(DRUPAL_FIELD_ROUND_STATUS)
  private Round.Status roundStatus;

  @SerializedName(DRUPAL_FIELD_ROUND_DATES)
  private DateRange roundDates;

  public Round()
  {
    super(CONTENT_TYPE);

    this.setRoundDates(new DateRange());
  }

  public Round.Status getRoundStatus()
  {
    return this.roundStatus;
  }

  public void setRoundStatus(Round.Status roundStatus)
  {
    this.roundStatus = roundStatus;
  }

  public DateRange getRoundDates()
  {
    return this.roundDates;
  }

  public boolean hasExpired()
  {
    Date latestExpiredStartTime = new Date(new Date().getTime() - MAX_ROUND_LENGTH_MS);

    return (this.getRoundDates().getStartDate().compareTo(latestExpiredStartTime) <= 0);
  }

  protected void setRoundDates(DateRange roundDates)
  {
    this.roundDates = roundDates;
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName()          + " [" +
           "id="            + this.getId()          + ", " +
           "url="           + this.getUrl()         + ", " +
           "roundStatus="   + this.roundStatus      + ", " +
           "roundDates="    + this.roundDates       + ", " +
           "published="     + this.isPublished()    + ", " +
           "dateCreated="   + this.getDateCreated() + ", " +
           "dateChanged="   + this.getDateChanged() +
           "]";
  }

  public static enum Status
  {
    @SerializedName("0")
    OPEN,

    @SerializedName("1")
    CLOSED
  }
}
