
package com.redbottledesign.bitcoin.pool.drupal;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.github.fireduck64.sockthing.EventLog;
import com.github.fireduck64.sockthing.StratumServer;
import com.redbottledesign.bitcoin.pool.Agent;
import com.redbottledesign.bitcoin.pool.PersistenceAgent;
import com.redbottledesign.bitcoin.pool.PersistenceCallback;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.RoundRequestor;
import com.redbottledesign.bitcoin.pool.drupal.node.Round;
import com.redbottledesign.drupal.DateRange;
import com.redbottledesign.drupal.User;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;

public class RoundAgent
extends Agent
{
  private static final long ROUND_UPDATE_FREQUENCY_MS = TimeUnit.MILLISECONDS.convert(2, TimeUnit.MINUTES);

  private final StratumServer server;
  private final EventLog logger;
  private final RoundRequestor roundRequestor;
  private final User.Reference poolDaemonUser;

  private Round currentRound;

  public RoundAgent(StratumServer server)
  {
    super(ROUND_UPDATE_FREQUENCY_MS);

    DrupalSession session = server.getSession();

    this.server           = server;
    this.logger           = server.getEventLog();
    this.roundRequestor   = session.getRoundRequestor();
    this.poolDaemonUser   = session.getPoolDaemonUser().asReference();
  }

  public Round getCurrentRoundSynchronized()
  {
    Round currentRound = null;

    do
    {
      // Block in case we're changing between rounds...
      synchronized (this)
      {
        currentRound = this.currentRound;

        // Wait for the round to be initialized if it hasn't been yet.
        if (currentRound == null)
        {
          try
          {
            this.wait();
          }

          catch (InterruptedException e)
          {
            // Suppressed; expected.
          }
        }
      }
    }
    while (currentRound == null);

    return currentRound;
  }

  @Override
  protected void runPeriodicTask()
  {
    try
    {
      PersistenceAgent persistenceAgent = this.server.getPersistenceAgent();

      synchronized (this)
      {
        this.currentRound = this.roundRequestor.requestCurrentRound();

        if ((this.currentRound == null) || (this.currentRound.hasExpired()))
          this.startNewRound(persistenceAgent);

        this.notifyAll();
      }

      this.updateStatusOfPastRounds(persistenceAgent);
    }

    catch (IOException | DrupalHttpException e)
    {
      e.printStackTrace();
    }
  }

  protected synchronized void startNewRound(PersistenceAgent persistenceAgent)
  throws IOException, DrupalHttpException
  {
    Date      now           = new Date();
    Round     newRound;
    DateRange newRoundDates;

    if (this.currentRound != null)
    {
      DateRange roundDates = this.currentRound.getRoundDates();

      System.out.println("Ending round started at " + roundDates.getStartDate());

      roundDates.setEndDate(now);

      persistenceAgent.queueForSave(this.currentRound);
    }

    System.out.println("Starting new round at " + now);

    newRound      = new Round();
    newRoundDates = newRound.getRoundDates();

    newRound.setAuthor(this.poolDaemonUser);
    newRoundDates.setStartDate(now);
    newRoundDates.setEndDate(now);

    persistenceAgent.queueForSave(newRound, new PersistenceCallback<Round>()
    {
      @Override
      public void onEntitySaved(Round newRound)
      {
        RoundAgent.this.currentRound = newRound;

        synchronized (RoundAgent.this)
        {
          RoundAgent.this.notifyAll();
        }
      }
    });

    do
    {
      try
      {
        this.wait();
      }

      catch (InterruptedException ex)
      {
        // Suppressed; expected
      }
    }
    while (this.currentRound != newRound);
  }

  protected void updateStatusOfPastRounds(PersistenceAgent persistenceAgent)
  throws IOException, DrupalHttpException
  {
    List<Round> openRounds      = this.roundRequestor.requestAllOpenRounds();
    int         openRoundCount  = openRounds.size();
    String      message         = String.format("There are %d open rounds.", openRoundCount);

    System.out.println(message);
    logger.log(message);

    if (openRoundCount > Round.MAX_OPEN_ROUNDS)
    {
      // Close everything over the round cap.
      List<Round> roundsToClose = openRounds.subList(Round.MAX_OPEN_ROUNDS, openRoundCount);

      for (Round roundToClose : roundsToClose)
      {
        message = "Closing round started at " + roundToClose.getRoundDates().getStartDate();

        System.out.println(message);
        logger.log(message);

        roundToClose.setRoundStatus(Round.Status.CLOSED);

        persistenceAgent.queueForSave(roundToClose);
      }
    }
  }
}