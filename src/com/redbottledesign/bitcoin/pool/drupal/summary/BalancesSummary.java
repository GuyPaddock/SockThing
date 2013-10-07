package com.redbottledesign.bitcoin.pool.drupal.summary;

import java.math.BigDecimal;

import com.google.gson.annotations.SerializedName;
import com.redbottledesign.drupal.User;

public class BalancesSummary
{
  public static final String DRUPAL_FIELD_BALANCES = "balances";
  public static final String JAVA_FIELD_BALANCES = "balances";

  @SerializedName(DRUPAL_FIELD_BALANCES)
  private UserBalanceSummary[] balances;

  public UserBalanceSummary[] getBalances()
  {
    return this.balances;
  }

  protected void setBalances(UserBalanceSummary[] payouts)
  {
    this.balances = payouts;
  }

  public static class UserBalanceSummary
  {
    public static final String DRUPAL_FIELD_USER_ID = "uid";
    public static final String JAVA_FIELD_USER_ID = "userId";

    public static final String DRUPAL_FIELD_USER_BALANCE_CURRENT = "user_balance_current";
    public static final String JAVA_FIELD_USER_BALANCE_CURRENT = "userBalanceCurrent";

    public static final String DRUPAL_FIELD_USER_PAYOUT_MINIMUM = "user_payout_minimum_amount";
    public static final String JAVA_FIELD_USER_PAYOUT_MINIMUM = "userPayoutMinimum";

    public static final String DRUPAL_FIELD_USER_PAYOUT_ADDRESS = "user_payout_address";
    public static final String JAVA_FIELD_USER_PAYOUT_ADDRESS = "userPayoutAddress";

    @SerializedName(DRUPAL_FIELD_USER_ID)
    private int userId;

    @SerializedName(DRUPAL_FIELD_USER_BALANCE_CURRENT)
    private BigDecimal userBalanceCurrent;

    @SerializedName(DRUPAL_FIELD_USER_PAYOUT_MINIMUM)
    private BigDecimal userPayoutMinimum;

    @SerializedName(DRUPAL_FIELD_USER_PAYOUT_ADDRESS)
    private String userPayoutAddress;

    public int getUserId()
    {
      return this.userId;
    }

    public BigDecimal getUserBalanceCurrent()
    {
      return this.userBalanceCurrent;
    }

    public BigDecimal getUserPayoutMinimum()
    {
      return this.userPayoutMinimum;
    }

    public String getUserPayoutAddress()
    {
      return this.userPayoutAddress;
    }

    public User.Reference getUserReference()
    {
      return new User.Reference(this.userId);
    }

    protected void setUserId(int userId)
    {
      this.userId = userId;
    }

    protected void setUserBalanceCurrent(BigDecimal userBalanceCurrent)
    {
      this.userBalanceCurrent = userBalanceCurrent;
    }

    protected void setUserPayoutMinimum(BigDecimal userPayoutMinimum)
    {
      this.userPayoutMinimum = userPayoutMinimum;
    }

    protected void setUserPayoutAddress(String userPayoutAddress)
    {
      this.userPayoutAddress = userPayoutAddress;
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName()                    + " [" +
      		   "userId="              + this.userId               + ", " +
    		   	 "userBalanceCurrent="  + this.userBalanceCurrent   + ", " +
  		   	 	 "userPayoutMinimum="   + this.userPayoutMinimum    + ", " +
  		   	 	 "userPayoutAddress="   + this.userPayoutAddress    + "]";
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName()  + " [" +
    		   "balances=" + this.balances      + "]";
  }
}