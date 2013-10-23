package com.redbottledesign.bitcoin.pool.drupal.node;

import java.math.BigDecimal;

import com.google.gson.annotations.SerializedName;
import com.redbottledesign.drupal.Node;
import com.redbottledesign.drupal.User;

public class BlockCredit
extends Node
{
  public static final String CONTENT_TYPE = "credit";

  public static final String DRUPAL_FIELD_RECIPIENT = "field_credit_recipient";
  public static final String JAVA_FIELD_RECIPIENT = "recipient";

  public static final String DRUPAL_FIELD_BLOCK = "field_credit_block";
  public static final String JAVA_FIELD_BLOCK = "block";

  public static final String DRUPAL_FIELD_AMOUNT = "field_credit_amount";
  public static final String JAVA_FIELD_AMOUNT = "amount";

  public static final String DRUPAL_FIELD_TYPE = "field_credit_type";
  public static final String JAVA_FIELD_TYPE = "creditType";

  @SerializedName(DRUPAL_FIELD_RECIPIENT)
  private User.Reference recipient;

  @SerializedName(DRUPAL_FIELD_BLOCK)
  private Node.Reference block;

  @SerializedName(DRUPAL_FIELD_AMOUNT)
  private BigDecimal amount;

  @SerializedName(DRUPAL_FIELD_TYPE)
  private BlockCredit.Type creditType;

  public BlockCredit()
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

  public Node.Reference getBlock()
  {
    return this.block;
  }

  public void setBlock(Node.Reference block)
  {
    this.block = block;
  }

  public BigDecimal getAmount()
  {
    return this.amount;
  }

  public void setAmount(BigDecimal amount)
  {
    this.amount = amount;
  }

  public BlockCredit.Type getCreditType()
  {
    return this.creditType;
  }

  public void setCreditType(BlockCredit.Type creditType)
  {
    this.creditType = creditType;
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
           "block="         + this.block            + ", " +
           "amount="        + this.amount           + ", " +
           "creditType="    + this.creditType       + ", " +
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