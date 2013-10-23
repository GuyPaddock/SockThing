package com.redbottledesign.bitcoin.pool.drupal.node;

import java.math.BigDecimal;

import com.google.gson.annotations.SerializedName;
import com.redbottledesign.drupal.Node;
import com.redbottledesign.drupal.User;

public class Payout
extends Node
{
  public static final String CONTENT_TYPE = "payout";

  public static final String DRUPAL_FIELD_RECIPIENT = "field_payout_recipient";
  public static final String JAVA_FIELD_RECIPIENT = "recipient";

  public static final String DRUPAL_FIELD_AMOUNT = "field_payout_amount";
  public static final String JAVA_FIELD_AMOUNT = "amount";

  public static final String DRUPAL_FIELD_PAYMENT_ADDRESS = "field_payout_payment_address";
  public static final String JAVA_FIELD_PAYMENT_ADDRESS = "paymentAddress";

  public static final String DRUPAL_FIELD_PAYMENT_HASH = "field_payout_payment_hash";
  public static final String JAVA_FIELD_PAYMENT_HASH = "paymentHash";

  @SerializedName(DRUPAL_FIELD_RECIPIENT)
  private User.Reference recipient;

  @SerializedName(DRUPAL_FIELD_AMOUNT)
  private BigDecimal amount;

  @SerializedName(DRUPAL_FIELD_PAYMENT_ADDRESS)
  private String paymentAddress;

  @SerializedName(DRUPAL_FIELD_PAYMENT_HASH)
  private String paymentHash;

  public Payout()
  {
    super(CONTENT_TYPE);
  }

  public User.Reference getRecipient()
  {
    return this.recipient;
  }

  public void setRecipient(User.Reference recipient)
  {
    this.recipient = recipient;
  }

  public BigDecimal getAmount()
  {
    return this.amount;
  }

  public void setAmount(BigDecimal amount)
  {
    this.amount = amount;
  }

  public String getPaymentAddress()
  {
    return this.paymentAddress;
  }

  public void setPaymentAddress(String paymentAddress)
  {
    this.paymentAddress = paymentAddress;
  }

  public String getPaymentHash()
  {
    return this.paymentHash;
  }

  public void setPaymentHash(String paymentHash)
  {
    this.paymentHash = paymentHash;
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName()          + " [" +
           "id="            + this.getId()          + ", " +
           "url="           + this.getUrl()         + ", " +
           "revisionId="    + this.getRevisionId()  + ", " +
           "title="         + this.getTitle()       + ", " +
           "recipient="     + this.recipient        + ", " +
           "amount="        + this.amount           + ", " +
           "paymentAddress="+ this.paymentAddress   + ", " +
           "paymentHash="   + this.paymentHash      + ", " +
           "published="     + this.isPublished()    + ", " +
           "dateCreated="   + this.getDateCreated() + ", " +
           "dateChanged="   + this.getDateChanged() + ", " +
           "]";
  }
}