
package com.redbottledesign.bitcoin.pool.drupal;

import java.io.IOException;
import java.util.Date;

import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.RoundRequestor;
import com.redbottledesign.bitcoin.pool.drupal.node.Round;
import com.redbottledesign.drupal.DateRange;
import com.redbottledesign.drupal.User;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;

public class RoundAgent extends Thread
{
    public static final long DB_CHECK_MS = 2L * 60L * 1000L;            // 2 minutes
    private static final long MAX_ROUND_LENGTH_MS = 60L * 60L * 1000L;  // 1 hour

//    public static final long DB_CHECK_MS = 10L * 1000L;           // 10 seconds
//    private static final long MAX_ROUND_LENGTH_MS = 15L * 1000L;  // 15 seconds

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
          Thread.sleep(500);
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
            Date latestExpiredStartTime = new Date(new Date().getTime() - MAX_ROUND_LENGTH_MS);

            this.currentRound = this.roundRequestor.getCurrentRound();

            if ((this.currentRound == null) ||
                (this.currentRound.getRoundDates().getStartDate().compareTo(latestExpiredStartTime) <= 0))
            {
              this.startNewRound();
            }
          }

          catch (IOException | DrupalHttpException e)
          {
            e.printStackTrace();
          }
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

        System.out.println("Closing round started at " + roundDates.getStartDate());

        roundDates.setEndDate(now);

        this.currentRound.setRoundStatus(Round.Status.CLOSED);

        this.roundRequestor.updateNode(this.currentRound);
      }

      System.out.println("Starting new round at " + now);

      newRound = new Round();

      newRound.setAuthor(this.poolDaemonUser);
      newRound.setRoundStatus(Round.Status.OPEN);
      newRound.getRoundDates().setStartDate(now);

      this.roundRequestor.createNode(newRound);

      this.currentRound = newRound;
    }
}
