package com.redbottledesign.bitcoin.pool.drupal.gson.requestor;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  private static final String JSON_PARAM_LIMIT = "limit";

  public RoundRequestor(SessionManager sessionManager)
  {
    super(sessionManager);
  }

  public Round requestCurrentRound()
  throws IOException, DrupalHttpException
  {
    Round               currentRound  = null;
    Map<String, Object> criteriaMap   = new HashMap<>();

    criteriaMap.put(Node.DRUPAL_BUNDLE_TYPE_FIELD_NAME, Round.CONTENT_TYPE);
    criteriaMap.put(JSON_PARAM_LIMIT, 1);

    try
    {
      // Find the oldest round.
      currentRound =
        this.requestEntityByCriteria(
          Round.ENTITY_TYPE,
          criteriaMap,
          Round.DRUPAL_FIELD_ROUND_DATES, SortOrder.DESCENDING);
    }

    catch (DrupalEndpointMissingException ex)
    {
      // Suppress -- this is expected if there is no current round.
    }

    return currentRound;
  }

  public List<Round> requestAllOpenRounds()
  throws IOException, DrupalHttpException
  {
    List<Round>         openRounds  = Collections.emptyList();
    Map<String, Object> criteriaMap = new HashMap<>();

    criteriaMap.put(Node.DRUPAL_BUNDLE_TYPE_FIELD_NAME, Round.CONTENT_TYPE);
    criteriaMap.put(Round.DRUPAL_FIELD_ROUND_STATUS, Round.Status.OPEN.ordinal());

    try
    {
      // Find the oldest round.
      openRounds =
        this.requestEntitiesByCriteria(
          Round.ENTITY_TYPE,
          criteriaMap,
          Round.DRUPAL_FIELD_ROUND_DATES, SortOrder.DESCENDING);
    }

    catch (DrupalEndpointMissingException ex)
    {
      // Suppress -- this is expected if there is no current round.
    }

    return openRounds;
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
