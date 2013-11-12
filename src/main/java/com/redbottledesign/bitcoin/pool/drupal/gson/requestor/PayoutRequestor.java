package com.redbottledesign.bitcoin.pool.drupal.gson.requestor;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;

import com.google.gson.reflect.TypeToken;
import com.redbottledesign.bitcoin.pool.drupal.node.Payout;
import com.redbottledesign.drupal.Node;
import com.redbottledesign.drupal.gson.JsonEntityResultList;
import com.redbottledesign.drupal.gson.SessionManager;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;
import com.redbottledesign.drupal.gson.requestor.NodeRequestor;

public class PayoutRequestor
extends NodeRequestor<Payout>
{
  public PayoutRequestor(SessionManager sessionManager)
  {
    super(sessionManager);
  }

  @SuppressWarnings("serial")
  public Payout getPayout(final String paymentHash, final String paymentAddress)
  throws IOException, DrupalHttpException
  {
      Payout result;

      result =
          this.requestEntityByCriteria(
              Payout.ENTITY_TYPE,
              new HashMap<String, Object>()
              {{
                  put(Node.DRUPAL_BUNDLE_TYPE_FIELD_NAME,   Payout.CONTENT_TYPE);
                  put(Payout.DRUPAL_FIELD_PAYMENT_HASH,     paymentHash);
                  put(Payout.DRUPAL_FIELD_PAYMENT_ADDRESS,  paymentAddress);
              }});

      return result;
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
