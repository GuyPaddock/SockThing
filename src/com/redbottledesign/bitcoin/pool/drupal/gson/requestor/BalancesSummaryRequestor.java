package com.redbottledesign.bitcoin.pool.drupal.gson.requestor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;

import org.apache.http.client.methods.HttpGet;

import com.google.gson.Gson;
import com.redbottledesign.bitcoin.pool.drupal.summary.BalancesSummary;
import com.redbottledesign.drupal.gson.DrupalGsonFactory;
import com.redbottledesign.drupal.gson.SessionManager;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;
import com.redbottledesign.drupal.gson.requestor.SessionBasedHttpRequestor;

public class BalancesSummaryRequestor
extends SessionBasedHttpRequestor
{
  protected static final String ENDPOINT = "/balance_summary.json";

  public BalancesSummaryRequestor(SessionManager sessionManager)
  {
    super(sessionManager);
  }

  public BalancesSummary requestBalancesSummary()
  throws IOException, DrupalHttpException
  {
    BalancesSummary result      = null;
    URI             requestUri  = this.createEndpointUri(ENDPOINT);

    try (InputStream  responseStream = this.executeRequest(new HttpGet(requestUri));
         Reader       responseReader = new InputStreamReader(responseStream))
    {
      Gson drupalGson = DrupalGsonFactory.getInstance().createGson();

      result = drupalGson.fromJson(responseReader, BalancesSummary.class);
    }

    return result;
  }
}