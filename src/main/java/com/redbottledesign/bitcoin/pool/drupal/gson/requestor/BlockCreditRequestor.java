package com.redbottledesign.bitcoin.pool.drupal.gson.requestor;

import java.lang.reflect.Type;

import com.google.gson.reflect.TypeToken;
import com.redbottledesign.bitcoin.pool.drupal.node.BlockCredit;
import com.redbottledesign.drupal.gson.JsonEntityResultList;
import com.redbottledesign.drupal.gson.SessionManager;
import com.redbottledesign.drupal.gson.requestor.NodeRequestor;

public class BlockCreditRequestor
extends NodeRequestor<BlockCredit>
{
  public BlockCreditRequestor(SessionManager sessionManager)
  {
    super(sessionManager);
  }

  @Override
  protected Type getListResultType()
  {
    return new TypeToken<JsonEntityResultList<BlockCredit>>(){}.getType();
  }

  @Override
  protected Class<BlockCredit> getNodeType()
  {
    return BlockCredit.class;
  }
}
