package com.redbottledesign.bitcoin.pool.drupal.gson.requestor;

import java.lang.reflect.Type;

import com.google.gson.reflect.TypeToken;
import com.redbottledesign.bitcoin.pool.drupal.node.Payout;
import com.redbottledesign.drupal.gson.JsonEntityResultList;
import com.redbottledesign.drupal.gson.SessionManager;
import com.redbottledesign.drupal.gson.requestor.NodeRequestor;

public class PayoutRequestor
extends NodeRequestor<Payout>
{
  public PayoutRequestor(SessionManager sessionManager)
  {
    super(sessionManager);
  }

  @Override
  protected Type getListResultType()
  {
    return new TypeToken<JsonEntityResultList<Payout>>(){}.getType();
  }

  @Override
  protected Class<Payout> getNodeType()
  {
    return Payout.class;
  }
}
