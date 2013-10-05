
package com.redbottledesign.bitcoin.pool.drupal;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.RoundRequestor;
import com.redbottledesign.bitcoin.pool.drupal.node.Round;
import com.redbottledesign.drupal.DateRange;
import com.redbottledesign.drupal.User;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;

public class RoundAgent extends Thread
{
    private static final long ROUND_POLL_ACQUIRE_MS = 500;
    private static final long DB_CHECK_MS = TimeUnit.MILLISECONDS.convert(2, TimeUnit.MINUTES);

    private final RoundRequestor roundRequestor;
    private long lastCheck;
    private Round currentRound;
    private final User.Reference poolDaemonUser;

    public RoundAgent()
    {
      SingletonDrupalSessionFactory sessionFactory = SingletonDrupalSessionFactory.getInstance();

      this.setDaemon(true);
      this.setName(this.getClass().getSimpleName());

      this.roundRequestor = sessionFactory.getRoundRequestor();
      this.poolDaemonUser = sessionFactory.getPoolDaemonUser().asReference();
    }

    public Round getCurrentRoundSynchronized()
    {
      Round currentRound = null;

      // Wait for the round to be initialized if it hasn't been yet.
      do
      {
        synchronized (this)
        {
          currentRound = this.currentRound;
        }

        try
        {
          Thread.sleep(ROUND_POLL_ACQUIRE_MS);
        }

        catch (InterruptedException e)
        {
          // Expected
        }
      }
      while (currentRound == null);

      return currentRound;
    }

    /**
     * Do the actual update in this thread to avoid ever blocking work generation
     */
    @Override
    public void run()
    {
      while (true)
      {
        try
        {
          this.updateRounds();

          // FIXME: Switch to scheduled threads.
          synchronized (this)
          {
            this.wait(DB_CHECK_MS / 4);
          }
        }

        catch (Throwable t)
        {
          // Top-level handler
          t.printStackTrace();
        }
      }
    }

    private void updateRounds()
    {
      if (System.currentTimeMillis() > (lastCheck + DB_CHECK_MS))
      {
        synchronized (this)
        {
          try
          {
            this.currentRound = this.roundRequestor.requestCurrentRound();

            if ((this.currentRound == null) || (this.currentRound.hasExpired()))
              this.startNewRound();
          }

          catch (IOException | DrupalHttpException e)
          {
            e.printStackTrace();
          }
        }

        try
        {
          this.updateStatusOfPastRounds();
        }

        catch (IOException | DrupalHttpException e)
        {
          e.printStackTrace();
        }

        this.lastCheck = System.currentTimeMillis();
      }
    }

    protected void startNewRound()
    throws IOException, DrupalHttpException
    {
      Date  now       = new Date();
      Round newRound;

      if (this.currentRound != null)
      {
        DateRange roundDates = this.currentRound.getRoundDates();

        System.out.println("Ending round started at " + roundDates.getStartDate());

        roundDates.setEndDate(now);

        this.roundRequestor.updateNode(this.currentRound);
      }

      System.out.println("Starting new round at " + now);

      newRound = new Round();

      newRound.setAuthor(this.poolDaemonUser);
      newRound.getRoundDates().setStartDate(now);

      this.roundRequestor.createNode(newRound);

      this.currentRound = newRound;
    }

    protected void updateStatusOfPastRounds()
    throws IOException, DrupalHttpException
    {
      List<Round> openRounds      = this.roundRequestor.requestAllOpenRounds();
      int         openRoundCount  = openRounds.size();

      System.out.println(openRounds);
      System.out.printf("There are %d open rounds.\n", openRoundCount);

      if (openRoundCount > Round.MAX_OPEN_ROUNDS)
      {
        // Close everything over the round cap.
        List<Round> roundsToClose = openRounds.subList(Round.MAX_OPEN_ROUNDS, openRoundCount);

        for (Round roundToClose : roundsToClose)
        {
          System.out.println("Closing round started at " + roundToClose.getRoundDates().getStartDate());

          roundToClose.setRoundStatus(Round.Status.CLOSED);

          this.roundRequestor.updateNode(roundToClose);
        }
      }
    }
}
