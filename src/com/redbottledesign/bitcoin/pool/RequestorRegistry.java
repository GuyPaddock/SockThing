package com.redbottledesign.bitcoin.pool;

import java.util.HashMap;
import java.util.Map;

import com.github.fireduck64.sockthing.StratumServer;
import com.redbottledesign.bitcoin.pool.drupal.DrupalSession;
import com.redbottledesign.bitcoin.pool.drupal.node.BlockCredit;
import com.redbottledesign.bitcoin.pool.drupal.node.Round;
import com.redbottledesign.bitcoin.pool.drupal.node.SolvedBlock;
import com.redbottledesign.bitcoin.pool.drupal.node.WorkShare;
import com.redbottledesign.drupal.Entity;
import com.redbottledesign.drupal.User;
import com.redbottledesign.drupal.gson.requestor.EntityRequestor;

public class RequestorRegistry
{
  private final Map<Class<?>, EntityRequestor<?>> requestorMap;

  public RequestorRegistry(StratumServer server)
  {
    DrupalSession session = server.getSession();

    this.requestorMap = new HashMap<>();

    this.requestorMap.put(User.class,             session.getUserRequestor());
    this.requestorMap.put(WorkShare.class,        session.getShareRequestor());
    this.requestorMap.put(SolvedBlock.class,      session.getBlockRequestor());
    this.requestorMap.put(Round.class,            session.getRoundRequestor());
    this.requestorMap.put(BlockCredit.class,      session.getCreditRequestor());
//    this.requestorMap.put(key, value)
  }

  @SuppressWarnings("unchecked")
  public <T extends Entity<?>> EntityRequestor<T> getRequestorForEntity(T entity)
  {
    // Type-cast should be safe -- the map is based on types.
    Class<?>            entityClass = entity.getClass();
    EntityRequestor<T>  requestor   = (EntityRequestor<T>)this.requestorMap.get(entityClass);

    if (requestor == null)
      throw new IllegalStateException("No requestor registered for entity: " + entityClass.getName());

    return requestor;
  }
}
