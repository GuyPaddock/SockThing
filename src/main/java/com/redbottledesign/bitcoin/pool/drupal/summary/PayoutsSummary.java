package com.redbottledesign.bitcoin.pool.drupal.summary;

import com.google.gson.annotations.SerializedName;
import com.redbottledesign.drupal.User;

public class PayoutsSummary
{
  public static final String DRUPAL_FIELD_PAYOUTS = "payouts";
  public static final String JAVA_FIELD_PAYOUTS = "payouts";

  @SerializedName(DRUPAL_FIELD_PAYOUTS)
  private UserPayoutSummary[] payouts;

  public UserPayoutSummary[] getPayouts()
  {
    return this.payouts;
  }

  protected void setPayouts(UserPayoutSummary[] payouts)
  {
    this.payouts = payouts;
  }

  public static class UserPayoutSummary
  {
    public static final String DRUPAL_FIELD_USER_ID = "uid";
    public static final String JAVA_FIELD_USER_ID = "userId";

    public static final String DRUPAL_FIELD_OPEN_SHARES_TOTAL = "open_shares_total";
    public static final String JAVA_FIELD_OPEN_SHARES_TOTAL = "openSharesTotal";

    public static final String DRUPAL_FIELD_OPEN_SHARES_USER = "open_shares_user";
    public static final String JAVA_FIELD_OPEN_SHARES_USER = "openSharesUser";

    public static final String DRUPAL_FIELD_OPEN_BLOCKS_TOTAL = "open_blocks_total";
    public static final String JAVA_FIELD_OPEN_BLOCKS_TOTAL = "openBlocksTotal";

    public static final String DRUPAL_FIELD_OPEN_BLOCKS_USER = "open_blocks_user";
    public static final String JAVA_FIELD_OPEN_BLOCKS_USER = "openBlocksUser";

    public static final String DRUPAL_FIELD_OPEN_DIFFICULTY_TOTAL = "open_difficulty_total";
    public static final String JAVA_FIELD_OPEN_DIFFICULTY_TOTAL = "openDifficultyTotal";

    public static final String DRUPAL_FIELD_OPEN_DIFFICULTY_USER = "open_difficulty_user";
    public static final String JAVA_FIELD_OPEN_DIFFICULTY_USER = "openDifficultyUser";

    @SerializedName(DRUPAL_FIELD_USER_ID)
    private int userId;

    @SerializedName(DRUPAL_FIELD_OPEN_SHARES_TOTAL)
    private long openSharesTotal;

    @SerializedName(DRUPAL_FIELD_OPEN_SHARES_USER)
    private long openSharesUser;

    @SerializedName(DRUPAL_FIELD_OPEN_BLOCKS_TOTAL)
    private long openBlocksTotal;

    @SerializedName(DRUPAL_FIELD_OPEN_BLOCKS_USER)
    private long openBlocksUser;

    @SerializedName(DRUPAL_FIELD_OPEN_DIFFICULTY_TOTAL)
    private long openDifficultyTotal;

    @SerializedName(DRUPAL_FIELD_OPEN_DIFFICULTY_USER)
    private long openDifficultyUser;

    public int getUserId()
    {
      return this.userId;
    }

    public long getOpenSharesTotal()
    {
      return this.openSharesTotal;
    }

    public long getOpenSharesUser()
    {
      return this.openSharesUser;
    }

    public long getOpenBlocksTotal()
    {
      return this.openBlocksTotal;
    }

    public long getOpenBlocksUser()
    {
      return this.openBlocksUser;
    }

    public long getOpenDifficultyTotal()
    {
      return this.openDifficultyTotal;
    }

    public long getOpenDifficultyUser()
    {
      return this.openDifficultyUser;
    }

    public User.Reference getUserReference()
    {
      return new User.Reference(this.userId);
    }

    protected void setUserId(int userId)
    {
      this.userId = userId;
    }

    protected void setOpenSharesTotal(long openSharesTotal)
    {
      this.openSharesTotal = openSharesTotal;
    }

    protected void setOpenSharesUser(long openSharesUser)
    {
      this.openSharesUser = openSharesUser;
    }

    protected void setOpenBlocksTotal(long openBlocksTotal)
    {
      this.openBlocksTotal = openBlocksTotal;
    }

    protected void setOpenBlocksUser(long openBlocksUser)
    {
      this.openBlocksUser = openBlocksUser;
    }

    protected void setOpenDifficultyTotal(long openDifficultyTotal)
    {
      this.openDifficultyTotal = openDifficultyTotal;
    }

    protected void setOpenDifficultyUser(long openDifficultyUser)
    {
      this.openDifficultyUser = openDifficultyUser;
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName()                    + " [" +
      		   "userId="              + this.userId               + ", " +
  		   	 	 "openSharesTotal="     + this.openSharesTotal      + ", " +
  		   	 	 "openSharesUser="      + this.openSharesUser       + ", " +
  		   	 	 "openBlocksTotal="     + this.openBlocksTotal      + ", " +
  		   	 	 "openBlocksUser="      + this.openBlocksUser       + ", " +
  		   	 	 "openDifficultyTotal=" + this.openDifficultyTotal  + ", " +
  		   	 	 "openDifficultyUser="  + this.openDifficultyUser   + "]";
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName()  + " [" +
    		   "payouts=" + this.payouts        + "]";
  }
}