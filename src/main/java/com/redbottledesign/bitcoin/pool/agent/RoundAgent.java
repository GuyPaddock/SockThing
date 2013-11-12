package com.redbottledesign.bitcoin.pool.agent;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.StratumServer;
import com.redbottledesign.bitcoin.pool.agent.persistence.PersistenceAgent;
import com.redbottledesign.bitcoin.pool.checkpoint.CheckpointGsonBuilder;
import com.redbottledesign.bitcoin.pool.drupal.DrupalSession;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.RoundRequestor;
import com.redbottledesign.bitcoin.pool.drupal.node.Round;
import com.redbottledesign.bitcoin.pool.util.queue.QueueItem;
import com.redbottledesign.bitcoin.pool.util.queue.QueueItemCallback;
import com.redbottledesign.bitcoin.pool.util.queue.QueueItemCallbackFactory;
import com.redbottledesign.bitcoin.pool.util.queue.QueueItemSieve;
import com.redbottledesign.drupal.DateRange;
import com.redbottledesign.drupal.Entity;
import com.redbottledesign.drupal.User;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;

public class RoundAgent
extends Agent
implements QueueItemCallbackFactory<RoundAgent.RoundPersistenceCallback>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(RoundAgent.class);

    private static final long ROUND_UPDATE_FREQUENCY_MS = TimeUnit.MILLISECONDS.convert(2, TimeUnit.MINUTES);

    private final PersistenceAgent persistenceAgent;
    private final RoundRequestor roundRequestor;
    private final User.Reference poolDaemonUser;

    private Round currentRound;
    private Round nextRound;

    public RoundAgent(StratumServer server)
    {
        super(ROUND_UPDATE_FREQUENCY_MS);

        DrupalSession session = server.getSession();

        this.persistenceAgent   = server.getAgent(PersistenceAgent.class);
        this.roundRequestor     = session.getRoundRequestor();
        this.poolDaemonUser     = session.getPoolDaemonUser().asReference();

        CheckpointGsonBuilder.getInstance().registerQueueItemCallbackFactory(this);
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
                        if (LOGGER.isInfoEnabled())
                            LOGGER.info("Waiting for information on the current round...");

                        this.wait();
                    }

                    catch (InterruptedException e)
                    {
                        if (LOGGER.isTraceEnabled())
                            LOGGER.trace("getCurrentRoundSynchronized(): wait() interrupted.");
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
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("New round started: " + currentRound);

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

        if (this.roundChangesArePending())
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

                this.persistenceAgent.queueForSave(roundToClose);
            }
        }
    }

    protected void startNewRoundAndWait(Round newRound)
    {
        this.nextRound = newRound;

        this.persistenceAgent.queueForSave(newRound, new RoundPersistenceCallback(this));

        do
        {
            try
            {
                if (LOGGER.isInfoEnabled())
                    LOGGER.info(String.format("  Waiting for new round to start..."));

                // Wait until we find that the current round has become the next
                // round.
                this.wait();
            }

            catch (InterruptedException ex)
            {
                if (LOGGER.isTraceEnabled())
                    LOGGER.trace("startNewRoundAndWait(): wait() interrupted.");
            }
        }
        while ((this.nextRound != null) && (this.currentRound != this.nextRound));

        if (LOGGER.isInfoEnabled())
            LOGGER.info(String.format("  New round started."));
    }

    protected boolean roundChangesArePending()
    {
        return this.persistenceAgent.getQueryableQueue().hasItemMatchingSieve(
            new QueueItemSieve()
            {
                @Override
                public boolean matches(QueueItem<? extends Entity<?>> queueItem)
                {
                    return Round.class.isAssignableFrom(queueItem.getEntity().getClass());
                }
            });
    }

    @Override
    public RoundPersistenceCallback createCallback(Type desiredType)
    {
        RoundPersistenceCallback result = null;

        if (RoundPersistenceCallback.class.equals(desiredType))
            result = new RoundPersistenceCallback(this);

        return result;
    }

    protected static class RoundPersistenceCallback
    implements QueueItemCallback<Round>
    {
        private transient RoundAgent agent;

        public RoundPersistenceCallback(RoundAgent agent)
        {
            this.agent = agent;
        }

        public RoundAgent getAgent()
        {
            return this.agent;
        }

        @Override
        public void onEntityProcessed(Round newRound)
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