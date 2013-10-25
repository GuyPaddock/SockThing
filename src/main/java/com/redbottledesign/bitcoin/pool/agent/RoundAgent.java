package com.redbottledesign.bitcoin.pool.agent;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.StratumServer;
import com.redbottledesign.bitcoin.pool.Agent;
import com.redbottledesign.bitcoin.pool.PersistenceCallback;
import com.redbottledesign.bitcoin.pool.drupal.DrupalSession;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.RoundRequestor;
import com.redbottledesign.bitcoin.pool.drupal.node.Round;
import com.redbottledesign.drupal.DateRange;
import com.redbottledesign.drupal.User;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;

public class RoundAgent
extends Agent
{
    private static final Logger LOGGER = LoggerFactory.getLogger(RoundAgent.class);

    private static final long ROUND_UPDATE_FREQUENCY_MS = TimeUnit.MILLISECONDS.convert(2, TimeUnit.MINUTES);

    private final StratumServer server;
    private final PersistenceAgent persistenceAgent;
    private final RoundRequestor roundRequestor;
    private final User.Reference poolDaemonUser;

    private Round currentRound;
    private Round nextRound;

    public RoundAgent(StratumServer server)
    {
        super(ROUND_UPDATE_FREQUENCY_MS);

        DrupalSession session = server.getSession();

        this.server             = server;
        this.persistenceAgent   = server.getPersistenceAgent();
        this.roundRequestor     = session.getRoundRequestor();
        this.poolDaemonUser     = session.getPoolDaemonUser().asReference();
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

    protected Round getCurrentRound()
    {
        return this.currentRound;
    }

    protected synchronized void setCurrentRound(Round currentRound)
    {
        this.currentRound = currentRound;

        this.notifyAll();
    }

    protected Round getNextRound()
    {
        return this.nextRound;
    }

    protected synchronized void setNextRound(Round nextRound)
    {
        this.nextRound = nextRound;

        this.notifyAll();
    }

    @Override
    protected void runPeriodicTask()
    {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Checking and updating the status of open and closed rounds...");

        if (this.persistenceAgent.queueHasItemOfType(Round.class))
        {
            if (LOGGER.isInfoEnabled())
                LOGGER.info("  Not updating round information -- a Round is still waiting to be persisted.");
        }

        else
        {
            try
            {
                // This will block if a round change is in progress.
                synchronized (this)
                {
                    this.currentRound = this.roundRequestor.requestCurrentRound();

                    if ((this.currentRound == null) || (this.currentRound.hasExpired()))
                    {
                        this.startNewRound();

                        // Wake-up any threads waiting to acquire the first round.
                        this.notifyAll();
                    }
                }

                this.updateStatusOfPastRounds();
            }

            catch (IOException | DrupalHttpException ex)
            {
                if (LOGGER.isErrorEnabled())
                {
                    LOGGER.error(
                        String.format(
                            "  Error updating round information: %s\n%s",
                            ex.getMessage(),
                            ExceptionUtils.getStackTrace(ex)));
                }
            }
        }

        if (LOGGER.isInfoEnabled())
            LOGGER.info("Round check complete.");
    }

    protected synchronized void startNewRound()
    throws IOException, DrupalHttpException
    {
        Date        now = new Date();
        Round       newRound;
        DateRange   newRoundDates;

        if (this.currentRound != null)
        {
            DateRange roundDates = this.currentRound.getRoundDates();

            if (LOGGER.isInfoEnabled())
                LOGGER.info("  Ending round started at " + roundDates.getStartDate());

            roundDates.setEndDate(now);

            this.persistenceAgent.queueForSave(this.currentRound);
        }

        if (LOGGER.isInfoEnabled())
            LOGGER.info("  Starting new round at " + now);

        newRound = new Round();
        newRoundDates = newRound.getRoundDates();

        newRound.setAuthor(this.poolDaemonUser);
        newRoundDates.setStartDate(now);
        newRoundDates.setEndDate(now);

        this.startNewRoundAndWait(newRound);
    }

    protected void updateStatusOfPastRounds()
    throws IOException, DrupalHttpException
    {
        PersistenceAgent    persistenceAgent    = this.server.getPersistenceAgent();
        List<Round>         openRounds          = this.roundRequestor.requestAllOpenRounds();
        int                 openRoundCount      = openRounds.size();

        if (LOGGER.isInfoEnabled())
            LOGGER.info(String.format("  There are %d open rounds.", openRoundCount));

        if (openRoundCount > Round.MAX_OPEN_ROUNDS)
        {
            // Close everything over the round cap.
            List<Round> roundsToClose = openRounds.subList(Round.MAX_OPEN_ROUNDS, openRoundCount);

            for (Round roundToClose : roundsToClose)
            {
                if (LOGGER.isInfoEnabled())
                    LOGGER.info("  Closing round started at " + roundToClose.getRoundDates().getStartDate());

                roundToClose.setRoundStatus(Round.Status.CLOSED);

                persistenceAgent.queueForSave(roundToClose);
            }
        }
    }

    private void startNewRoundAndWait(Round newRound)
    {
        PersistenceAgent persistenceAgent = this.server.getPersistenceAgent();

        this.nextRound = newRound;

        persistenceAgent.queueForSave(newRound, new RoundPersistenceCallback(this));

        do
        {
            try
            {
                // Wait until we find that the current round has become the next
                // round.
                this.wait();
            }

            catch (InterruptedException ex)
            {
                // Suppressed; expected
            }
        }
        while ((this.nextRound != null) && (this.currentRound != this.nextRound));
    }

    protected static class RoundPersistenceCallback
    implements PersistenceCallback<Round>
    {
        private RoundAgent agent;

        public RoundPersistenceCallback(RoundAgent agent)
        {
            this.agent = agent;
        }

        public RoundAgent getAgent()
        {
            return this.agent;
        }

        @Override
        public void onEntitySaved(Round newRound)
        {
            synchronized (this.agent)
            {
                // Move to next round.
                this.agent.setCurrentRound(newRound);
            }
        }

        @Override
        public void onEntityEvicted(Round evictedEntity)
        {
            synchronized (this.agent)
            {
                // Next round was canceled.
                this.agent.setNextRound(null);
            }
        }

        protected void setAgent(RoundAgent agent)
        {
            this.agent = agent;
        }
    }
}