package com.redbottledesign.bitcoin.pool.drupal.gson.requestor;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;

import com.google.gson.reflect.TypeToken;
import com.redbottledesign.bitcoin.pool.drupal.node.Round;
import com.redbottledesign.drupal.Node;
import com.redbottledesign.drupal.gson.JsonEntityResultList;
import com.redbottledesign.drupal.gson.SessionManager;
import com.redbottledesign.drupal.gson.exception.DrupalEndpointMissingException;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;
import com.redbottledesign.drupal.gson.requestor.NodeRequestor;
import com.redbottledesign.drupal.gson.requestor.SortOrder;

public class RoundRequestor
extends NodeRequestor<Round>
{
  public RoundRequestor(SessionManager sessionManager)
  {
    super(sessionManager);
  }

  public Round getCurrentRound()
  throws IOException, DrupalHttpException
  {
    Round currentRound = null;

    try
    {
      // Find the oldest round.
      currentRound =
        this.requestEntityByCriteria(
          Round.ENTITY_TYPE,
          Collections.singletonMap(Node.DRUPAL_BUNDLE_TYPE_FIELD_NAME, (Object)Round.CONTENT_TYPE),
          Round.DRUPAL_FIELD_ROUND_DATES, SortOrder.DESCENDING);
    }

    catch (DrupalEndpointMissingException ex)
    {
      // Suppress -- this is expected if there is no current round.
    }

    return currentRound;
  }

  @Override
  protected Type getListResultType()
  {
    return new TypeToken<JsonEntityResultList<Round>>(){}.getType();
  }

  @Override
  protected Class<Round> getNodeType()
  {
    return Round.class;
  }
}
