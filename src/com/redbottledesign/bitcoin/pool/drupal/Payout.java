package com.redbottledesign.bitcoin.pool.drupal;

import java.math.BigDecimal;

import com.google.gson.annotations.SerializedName;
import com.redbottledesign.drupal.Node;
import com.redbottledesign.drupal.User;

public class Payout
extends Node
{
  private static final String CONTENT_TYPE = "payout";

  public static final String DRUPAL_FIELD_RECIPIENT = "field_payout_recipient";
  public static final String JAVA_FIELD_RECIPIENT = "recipient";

  public static final String DRUPAL_FIELD_AMOUNT = "field_payout_amount";
  public static final String JAVA_FIELD_AMOUNT = "amount";

  public static final String DRUPAL_FIELD_BLOCK = "field_payout_block";
  public static final String JAVA_FIELD_BLOCK = "block";

  public static final String DRUPAL_FIELD_TYPE = "field_payout_type";
  public static final String JAVA_FIELD_TYPE = "type";

  @SerializedName(DRUPAL_FIELD_RECIPIENT)
  private User.Reference recipient;

  @SerializedName(DRUPAL_FIELD_AMOUNT)
  private BigDecimal amount;

  @SerializedName(DRUPAL_FIELD_BLOCK)
  private Node.Reference block;

  @SerializedName(DRUPAL_FIELD_TYPE)
  private Payout.Type type;

  public Payout()
  {
    super(CONTENT_TYPE);
  }

  public String getPaymentHash()
  {
    return this.getTitle();
  }

  public void setPaymentHash(String paymentHash)
  {
    this.setTitle(paymentHash);
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

  public Node.Reference getBlock()
  {
    return this.block;
  }

  public void setBlock(Node.Reference block)
  {
    this.block = block;
  }

  public Payout.Type getType()
  {
    return this.type;
  }

  public void setType(Payout.Type type)
  {
    this.type = type;
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName()          + " [" +
           "id="            + this.getId()          + ", " +
           "url="           + this.getUrl()         + ", " +
           "revisionId="    + this.getRevisionId()  + ", " +
           "recipient="     + this.recipient        + ", " +
           "amount="        + this.amount           + ", " +
           "block="         + this.block            + ", " +
           "type="          + this.type             + ", " +
           "published="     + this.isPublished()    + ", " +
           "dateCreated="   + this.getDateCreated() + ", " +
           "dateChanged="   + this.getDateChanged() + ", " +
           "]";
  }

  public static enum Type
  {
    @SerializedName("regular")
    REGULAR_SHARE,

    @SerializedName("bonus")
    BLOCK_SOLUTION_BONUS
  }
}