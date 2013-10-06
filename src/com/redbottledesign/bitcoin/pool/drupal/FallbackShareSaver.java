package com.redbottledesign.bitcoin.pool.drupal;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;

import au.com.bytecode.opencsv.CSVWriter;

import com.github.fireduck64.sockthing.Config;
import com.github.fireduck64.sockthing.PoolUser;
import com.github.fireduck64.sockthing.StratumServer;
import com.github.fireduck64.sockthing.SubmitResult;
import com.github.fireduck64.sockthing.sharesaver.ShareSaveException;
import com.github.fireduck64.sockthing.sharesaver.ShareSaver;
import com.redbottledesign.bitcoin.pool.drupal.node.Round;

public class FallbackShareSaver
implements ShareSaver
{
  private static final String CONFIRM_YES = "Y";
  private static final String CONFIG_VALUE_SHARE_FALLBACK_OUTPUT = "fallback_share_output";

  private final StratumServer server;
  private ShareSaver innerSaver;
  private CSVWriter csvWriter;

  public FallbackShareSaver(Config config, StratumServer server)
  throws IOException
  {
    this(config, server, null);
  }

  public FallbackShareSaver(Config config, StratumServer server, ShareSaver innerSaver)
  throws IOException
  {
    this(server, getConfiguredOutput(config), innerSaver);
  }

  public FallbackShareSaver(StratumServer server, String outputFilename)
  throws IOException
  {
    this(server, outputFilename, null);
  }

  public FallbackShareSaver(StratumServer server, File outputFile)
  throws IOException
  {
    this(server, outputFile, null);
  }

  public FallbackShareSaver(StratumServer server, String outputFilename, ShareSaver innerSaver)
  throws IOException
  {
    this(server, new File(outputFilename), innerSaver);
  }

  public FallbackShareSaver(StratumServer server, File outputFile, ShareSaver innerSaver)
  throws IOException
  {
    this.server = server;

    this.setInnerSaver(innerSaver);
    this.initializeOutput(outputFile);
  }

  public ShareSaver getInnerSaver()
  {
    return this.innerSaver;
  }

  protected void setInnerSaver(ShareSaver innerSaver)
  {
    this.innerSaver = innerSaver;
  }

  @Override
  public void saveShare(PoolUser pu, SubmitResult submitResult, String source, String uniqueJobString, Long blockReward)
  throws ShareSaveException
  {
    boolean   wasSaved          = false;
    Throwable pendingException  = null;

    if (this.innerSaver != null)
    {
      try
      {
        this.innerSaver.saveShare(pu, submitResult, source, uniqueJobString, blockReward);

        wasSaved = true;
      }

      catch (Throwable t)
      {
        pendingException = t;
      }
    }

    try
    {
      this.writeFallbackShare(pu, submitResult, source, uniqueJobString, blockReward, wasSaved, pendingException);
    }

    catch (IOException ex)
    {
      throw new ShareSaveException("Failed while writing fallback share: " + ex.getMessage(), ex);
    }

    if (pendingException != null)
      throw new ShareSaveException(pendingException);
  }

  protected void initializeOutput(File outputFile)
  throws IOException
  {
    boolean isFileNew = !outputFile.exists();

    this.csvWriter = new CSVWriter(new FileWriter(outputFile, true));

    if (isFileNew)
    {
      String[] shareHeaders =
        new String[]
        {
          "Job hash",
          "Round node ID",
          "Share difficulty",
          "Network difficulty",
          "Share submitter",
          "Submission time",
          "Client software",
          "Pool host",
          "Verified by pool",
          "Verified by network",
          "Status",
          "Hash",
          "Block",
          "Block reward (satoshis)",
          "Was saved successfully to primary database",
          "Last exception",
        };

      synchronized (this.csvWriter)
      {
        this.csvWriter.writeNext(shareHeaders);
        this.csvWriter.flush();
      }
    }
  }

  protected void writeFallbackShare(PoolUser pu, SubmitResult submitResult, String source, String uniqueJobString,
                                    Long blockReward, boolean wasSaved, Throwable pendingException)
  throws IOException
  {
    String[] shareInfo = null;

    try
    {
      RoundAgent  roundAgent      = this.server.getRoundAgent();
      Round       currentRound    = roundAgent.getCurrentRoundSynchronized();
      double      blockDifficulty = submitResult.getNetworkDifficulty(),
                  workDifficulty  = submitResult.getOurDifficulty();
      String      statusString    = "accepted";

      if (submitResult.getReason() != null)
      {
        statusString = submitResult.getReason();

        if (statusString.length() > 50)
          statusString = statusString.substring(0, 50);
      }

      shareInfo =
        new String[]
        {
          uniqueJobString,
          currentRound.getId().toString(),
          Double.toString(workDifficulty),
          Double.toString(blockDifficulty),
          pu.getName(),
          new Date().toString(),
          submitResult.getClientVersion(),
          source,
          Boolean.toString(CONFIRM_YES.equals(submitResult.getOurResult())),
          Boolean.toString(CONFIRM_YES.equals(submitResult.getUpstreamResult())),
          statusString,
          submitResult.getHash().toString(),
          Integer.toString(submitResult.getHeight()),
          BigDecimal.valueOf(blockReward).toString(),
          Boolean.toString(wasSaved),
          ((pendingException == null) ?
           "" :
           (pendingException.getClass().getName() + ": " + pendingException.getMessage())),
        };

      synchronized (this.csvWriter)
      {
        this.csvWriter.writeNext(shareInfo);
        this.csvWriter.flush();
      }
    }

    catch (Throwable ex)
    {
      ex.printStackTrace();

      // It's pretty serious if we can't write to the audit log.
      System.err.println("Exiting with fatal status; shares could not be captured in fall-back audit log.");
      System.err.println("Share being written was: " + Arrays.toString(shareInfo));
      System.exit(-1);
    }
  }

  protected static String getConfiguredOutput(Config config)
  {
    config.require(CONFIG_VALUE_SHARE_FALLBACK_OUTPUT);

    return config.get(CONFIG_VALUE_SHARE_FALLBACK_OUTPUT);
  }
}
