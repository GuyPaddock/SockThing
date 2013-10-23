package com.redbottledesign.bitcoin.pool.drupal.gson.requestor;

import java.lang.reflect.Type;

import com.google.gson.reflect.TypeToken;
import com.redbottledesign.bitcoin.pool.drupal.node.WorkShare;
import com.redbottledesign.drupal.gson.JsonEntityResultList;
import com.redbottledesign.drupal.gson.SessionManager;
import com.redbottledesign.drupal.gson.requestor.NodeRequestor;

public class WorkShareRequestor
extends NodeRequestor<WorkShare>
{
  public WorkShareRequestor(SessionManager sessionManager)
  {
    super(sessionManager);
  }

  @Override
  protected Type getListResultType()
  {
    return new TypeToken<JsonEntityResultList<WorkShare>>(){}.getType();
  }

  @Override
  protected Class<WorkShare> getNodeType()
  {
    return WorkShare.class;
  }
}
