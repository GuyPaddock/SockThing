package com.redbottledesign.bitcoin.pool.drupal.gson.requestor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.Arrays;

import org.apache.http.client.methods.HttpGet;

import com.google.gson.Gson;
import com.redbottledesign.bitcoin.pool.drupal.summary.WorkersSummary;
import com.redbottledesign.bitcoin.pool.drupal.summary.WorkersSummary.UserWorkerSummary;
import com.redbottledesign.drupal.gson.DrupalGsonFactory;
import com.redbottledesign.drupal.gson.SessionManager;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;
import com.redbottledesign.drupal.gson.requestor.SessionBasedHttpRequestor;

public class WorkersSummaryRequestor
extends SessionBasedHttpRequestor
{
  protected static final String ENDPOINT = "/worker_summary.json";

  public WorkersSummaryRequestor(SessionManager sessionManager)
  {
    super(sessionManager);
  }

  public WorkersSummary.UserWorkerSummary getUserWorkerSummary(String workerName)
  throws IOException, DrupalHttpException
  {
    UserWorkerSummary   result;
    WorkersSummary      workersSummary;
    UserWorkerSummary[] workers;

    if (workerName == null)
      throw new IllegalArgumentException("workerName cannot be null.");

    if (workerName.isEmpty())
      throw new IllegalArgumentException("workerName cannot be empty.");

    workersSummary =
      this.requestWorkersSummary(this.createUriForCriterion(ENDPOINT, WorkersSummary.DRUPAL_FIELD_WORKERS, workerName));

    workers = workersSummary.getWorkers();

    if (workers.length > 1)
    {
      throw new IllegalStateException(
        String.format(
          "Got multiple workers when only one worker was expected for worker name (%s): %s",
          workerName,
          Arrays.toString(workers)));
    }

    else if (workers.length == 0)
    {
      result = null;
    }

    else
    {
      result = workers[0];
    }

    return result;
  }

  public WorkersSummary requestWorkersSummary()
  throws IOException, DrupalHttpException
  {
    return this.requestWorkersSummary(this.createEndpointUri(ENDPOINT));
  }

  public WorkersSummary requestWorkersSummary(URI requestUri)
  throws IOException, DrupalHttpException
  {
    WorkersSummary  result      = null;

    try (InputStream  responseStream = this.executeRequest(new HttpGet(requestUri));
         Reader       responseReader = new InputStreamReader(responseStream))
    {
      Gson drupalGson = DrupalGsonFactory.getInstance().createGson();

      result = drupalGson.fromJson(responseReader, WorkersSummary.class);
    }

    return result;
  }
}