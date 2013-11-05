package com.redbottledesign.bitcoin.pool.drupal.gson.requestor;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.reflect.TypeToken;
import com.redbottledesign.bitcoin.pool.drupal.node.SolvedBlock;
import com.redbottledesign.drupal.Node;
import com.redbottledesign.drupal.gson.JsonEntityResultList;
import com.redbottledesign.drupal.gson.SessionManager;
import com.redbottledesign.drupal.gson.exception.DrupalEndpointMissingException;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;
import com.redbottledesign.drupal.gson.requestor.NodeRequestor;

public class SolvedBlockRequestor
extends NodeRequestor<SolvedBlock>
{
  public SolvedBlockRequestor(SessionManager sessionManager)
  {
    super(sessionManager);
  }

  public List<SolvedBlock> getUnconfirmedBlocks()
  throws IOException, DrupalHttpException
  {
      List<SolvedBlock>   unconfirmedBlocks = Collections.emptyList();
      Map<String, Object> criteriaMap       = new HashMap<>();

      criteriaMap.put(Node.DRUPAL_BUNDLE_TYPE_FIELD_NAME,   SolvedBlock.CONTENT_TYPE);
      criteriaMap.put(Node.DRUPAL_PUBLISHED_FIELD_NAME,     1);
      criteriaMap.put(SolvedBlock.DRUPAL_FIELD_STATUS,      SolvedBlock.Status.UNCONFIRMED.ordinal());

      try
      {
          unconfirmedBlocks = this.requestEntitiesByCriteria(SolvedBlock.ENTITY_TYPE, criteriaMap);
      }

      catch (DrupalEndpointMissingException ex)
      {
          // Suppress -- this is expected if there is no current round.
      }

      return unconfirmedBlocks;
  }

  @Override
  protected Type getListResultType()
  {
    return new TypeToken<JsonEntityResultList<SolvedBlock>>(){}.getType();
  }

  @Override
  protected Class<SolvedBlock> getNodeType()
  {
    return SolvedBlock.class;
  }
}