package com.redbottledesign.bitcoin.pool.drupal;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.Config;
import com.github.fireduck64.sockthing.PplnsAgent;
import com.github.fireduck64.sockthing.StratumServer;
import com.redbottledesign.bitcoin.pool.agent.CheckpointableAgent;
import com.redbottledesign.bitcoin.pool.agent.EvictableQueue;
import com.redbottledesign.bitcoin.pool.agent.PersistenceAgent;
import com.redbottledesign.bitcoin.pool.checkpoint.CheckpointItem;
import com.redbottledesign.bitcoin.pool.checkpoint.SimpleCheckpointItem;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.PayoutsSummaryRequestor;
import com.redbottledesign.bitcoin.pool.drupal.node.BlockCredit;
import com.redbottledesign.bitcoin.pool.drupal.node.SolvedBlock;
import com.redbottledesign.bitcoin.pool.drupal.summary.PayoutsSummary;
import com.redbottledesign.drupal.User;
import com.redbottledesign.util.QueueUtils;

public class DrupalPplnsAgent
extends CheckpointableAgent
implements PplnsAgent, EvictableQueue<String>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DrupalPplnsAgent.class);

    private static final String CONFIG_VALUE_PAYOUT_BLOCK_PERCENTAGE_SOLVER = "payout_block_percentage_solver";
    private static final String CONFIG_VALUE_PAYOUT_BLOCK_PERCENTAGE_NORMAL = "payout_block_percentage_normal";
    private static final String CONFIG_VALUE_PAYOUT_BLOCK_PERCENTAGE_POOL_FEE = "payout_block_percentage_pool_fee";

    private final StratumServer server;
    private final BlockingQueue<BlockQueueItem> pendingBlockQueue;
    private final Config config;
    private double solverPercentage;
    private double normalPercentage;
    private double poolFee;

    public DrupalPplnsAgent(StratumServer server)
    {
        super();

        this.server             = server;
        this.config             = server.getConfig();
        this.pendingBlockQueue  = new LinkedBlockingQueue<>();
    }

    @Override
    public void queueBlockForPayout(SolvedBlock block)
    {
        BlockQueueItem queueItem = new BlockQueueItem(block);

        if (LOGGER.isInfoEnabled())
            LOGGER.info(String.format("Queuing payouts for block %d.", block.getHeight()));

        QueueUtils.ensureQueued(this.pendingBlockQueue, queueItem);

        this.notifyCheckpointListenersOnItemCreated(queueItem);
    }

    @Override
    public synchronized boolean evictQueueItem(String blockHash)
    {
        return this.evictQueueItems(Collections.singleton(blockHash));
    }

    @Override
    public synchronized boolean evictQueueItems(Set<String> blockHashes)
    {
        boolean                     atLeastOneItemEvicted = false;
        Iterator<BlockQueueItem>    queueIterator;

        this.interruptQueueProcessing();

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Evicting items from block payout queue: " + blockHashes);

        queueIterator = this.pendingBlockQueue.iterator();

        while (queueIterator.hasNext())
        {
            BlockQueueItem  queueItem   = queueIterator.next();
            String          blockHash   = queueItem.getValue().getHash();

            if (blockHashes.contains(blockHash))
            {
                if (LOGGER.isInfoEnabled())
                {
                    LOGGER.info(
                        String.format(
                            "Evicting queued block payout upon request (block hash: %s): %s",
                            blockHash,
                            queueItem));
                }

                queueIterator.remove();

                atLeastOneItemEvicted = true;
                break;
            }
        }

        if (LOGGER.isInfoEnabled())
        {
            if (atLeastOneItemEvicted)
                LOGGER.info("evictQueueItems() was called and at least one item was evicted.");

            else
                LOGGER.info("evictQueueItems() was called, but no items were evicted.");
        }

        return atLeastOneItemEvicted;
    }

    @Override
    public synchronized boolean evictAllQueueItems()
    {
        boolean queueHasItems;

        this.interruptQueueProcessing();

        queueHasItems = !this.pendingBlockQueue.isEmpty();

        if (queueHasItems)
        {
            if (LOGGER.isInfoEnabled())
                LOGGER.info("Evicting all queued block payouts upon request.");

            this.pendingBlockQueue.clear();
        }

        return queueHasItems;
    }

    @Override
    public Type getCheckpointItemType()
    {
        return SolvedBlock.class;
    }

    @Override
    public synchronized Collection<? extends CheckpointItem> captureCheckpoint()
    {
        this.interruptQueueProcessing();

        if (LOGGER.isInfoEnabled())
        {
            LOGGER.info(
                String.format("Capturing checkpoint of %d queued block payouts.", this.pendingBlockQueue.size()));
        }

        return new LinkedList<BlockQueueItem>(this.pendingBlockQueue);
    }

    @Override
    public void restoreFromCheckpoint(Collection<? extends CheckpointItem> checkpoint)
    {
        Set<String>          newBlockHashes = new HashSet<>();
        List<BlockQueueItem> newQueueItems  = new ArrayList<>(checkpoint.size());

        if (LOGGER.isInfoEnabled())
            LOGGER.info(String.format("Restoring %d items from a checkpoint.", checkpoint.size()));

        this.interruptQueueProcessing();

        for (CheckpointItem checkpointItem : checkpoint)
        {
            BlockQueueItem blockItem;

            if (!(checkpointItem instanceof BlockQueueItem))
            {
                if (LOGGER.isErrorEnabled())
                {
                    LOGGER.error(
                        "A checkpoint item was provided that is not a BlockQueueItem (ignoring): " +
                        checkpointItem);
                }
            }

            else
            {
                blockItem = (BlockQueueItem)checkpointItem;

                newBlockHashes.add(blockItem.getValue().getHash());
                newQueueItems.add(blockItem);
            }
        }

        // Only evict items we're replacing from the checkpoint.
        this.evictQueueItems(newBlockHashes);

        this.pendingBlockQueue.addAll(newQueueItems);
    }

    @Override
    protected void loadConfig()
    {
        double totalPercentage;

        this.config.require(CONFIG_VALUE_PAYOUT_BLOCK_PERCENTAGE_SOLVER);
        this.config.require(CONFIG_VALUE_PAYOUT_BLOCK_PERCENTAGE_NORMAL);
        this.config.require(CONFIG_VALUE_PAYOUT_BLOCK_PERCENTAGE_POOL_FEE);

        this.solverPercentage = this.config.getDouble(CONFIG_VALUE_PAYOUT_BLOCK_PERCENTAGE_SOLVER);
        this.normalPercentage = this.config.getDouble(CONFIG_VALUE_PAYOUT_BLOCK_PERCENTAGE_NORMAL);
        this.poolFee          = this.config.getDouble(CONFIG_VALUE_PAYOUT_BLOCK_PERCENTAGE_POOL_FEE);

        totalPercentage = solverPercentage + normalPercentage;

        if ((solverPercentage < 0) || (solverPercentage > 100))
        {
            throw new IllegalArgumentException(
                String.format(
                    "Solver percentage must be between 0.00 and 1.00, but currently equals %f.",
                    solverPercentage));
        }

        if ((normalPercentage < 0) || (normalPercentage > 100))
        {
            throw new IllegalArgumentException(
                String.format(
                    "Normal percentage must be between 0.00 and 1.00, but currently equals %f.",
                    normalPercentage));
        }

        if ((poolFee < 0) || (poolFee > 100))
        {
            throw new IllegalArgumentException(
                String.format(
                    "Pool fee must be between 0.00 and 1.00, but currently equals %f.",
                    poolFee));
        }

        if (totalPercentage != 1.0d)
        {
            throw new IllegalArgumentException(
                String.format(
                    "Solver percentage plus normal percentage must equal 1.0, but currently equals %f.",
                    totalPercentage));
        }
    }

    protected synchronized void interruptQueueProcessing()
    {
        if (LOGGER.isInfoEnabled())
            LOGGER.info("Queue processing has been temporarily interrupted.");

        /*
         * Interrupt to break out of pendingBlockQueue.take(); the block
         * thread should block in the run() method of Agent since it doesn't
         * hold the lock on this object.
         */
        this.interrupt();
    }

    @Override
    protected void runPeriodicTask()
    throws InterruptedException
    {
        PersistenceAgent        persistenceAgent    = this.server.getAgent(PersistenceAgent.class);
        DrupalSession           session             = this.server.getSession();
        PayoutsSummaryRequestor payoutsRequestor    = session.getPayoutsSummaryRequestor();
        User.Reference          poolDaemonUser      = session.getPoolDaemonUser().asReference();
        BlockQueueItem          currentQueueItem;

        // NOTE: This will block indefinitely.
        while ((currentQueueItem = this.pendingBlockQueue.take()) != null)
        {
            SolvedBlock         currentBlock    = currentQueueItem.getValue();
            double              blockReward     = currentBlock.getReward().floatValue() * (1d - this.poolFee);
            PayoutsSummary      payoutsSummary;
            List<BlockCredit>   blockCredits    = new LinkedList<>();

            if (LOGGER.isInfoEnabled())
                LOGGER.info(String.format("Running payouts for block %d...", currentBlock.getHeight()));

            try
            {
                // FIXME: Request the 12 open rounds at the time that the block was solved rather than right now
                payoutsSummary = payoutsRequestor.requestPayoutsSummary();
            }

            catch (Throwable ex)
            {
                String error;

                // Re-queue block.
                QueueUtils.ensureQueued(this.pendingBlockQueue, currentQueueItem);

                error =
                    String.format(
                        "Failed to request payouts summary while handling block '%s' (queued to retry): %s",
                        currentBlock,
                        ex.getMessage());

                if (LOGGER.isErrorEnabled())
                    LOGGER.error(error + "\n" + ExceptionUtils.getStackTrace(ex));

                throw new RuntimeException(error, ex);
            }

            try
            {
                for (PayoutsSummary.UserPayoutSummary userPayout : payoutsSummary.getPayouts())
                {
                    User.Reference recipient = userPayout.getUserReference();

                    blockCredits.add(
                        this.createUserCreditForBlock(
                            recipient,
                            this.calculateBlockCredit(blockReward, userPayout),
                            currentBlock,
                            BlockCredit.Type.REGULAR_SHARE,
                            persistenceAgent,
                            poolDaemonUser));

                    if (currentBlock.getSolvingMember().equals(recipient))
                    {
                        blockCredits.add(
                            this.createUserCreditForBlock(
                                recipient,
                                this.calculateBlockBonus(blockReward, userPayout),
                                currentBlock,
                                BlockCredit.Type.BLOCK_SOLUTION_BONUS,
                                persistenceAgent,
                                poolDaemonUser));
                    }
                }
            }

            catch (Throwable ex)
            {
                String error;

                // Re-queue block.
                QueueUtils.ensureQueued(this.pendingBlockQueue, currentQueueItem);

                error =
                    String.format(
                        "Failed to issue payouts for block '%s' (no payouts issued; block queued to retry): %s",
                        currentBlock,
                        ex.getMessage());

                if (LOGGER.isErrorEnabled())
                    LOGGER.error(error + "\n" + ExceptionUtils.getStackTrace(ex));

                throw new RuntimeException(error, ex);
            }

            /* We queue up the credits for persistence only at the end, to ensure all-or-nothing behavior for credits.
             *
             * We don't want only some of the users to get credit for a block and not others.
             */
            for (BlockCredit blockCredit : blockCredits)
            {
                persistenceAgent.queueForSave(blockCredit);
            }

            this.notifyCheckpointListenersOnItemExpired(currentQueueItem);
        }
    }

    protected BlockCredit createUserCreditForBlock(User.Reference user, BigDecimal creditAmount, SolvedBlock block,
                                                   BlockCredit.Type creditType, PersistenceAgent persistenceAgent,
                                                   User.Reference poolDaemonUser)
    {
        BlockCredit result = new BlockCredit();

        if (LOGGER.isInfoEnabled())
        {
            LOGGER.info(
                String.format(
                    "Crediting user ID #%d with %s in the amount of %s for block %d.",
                    user.getId(),
                    creditType,
                    creditAmount.toPlainString(),
                    block.getHeight()));
        }

        result.setAuthor(poolDaemonUser);
        result.setRecipient(user);
        result.setBlock(block.asReference());
        result.setAmount(creditAmount);
        result.setCreditType(creditType);

        return result;
    }

    protected BigDecimal calculateBlockCredit(double blockReward, PayoutsSummary.UserPayoutSummary userPayout)
    {
        float difficultyUser    = userPayout.getOpenDifficultyUser(),
              difficultyTotal   = userPayout.getOpenDifficultyTotal();

        return BigDecimal.valueOf(blockReward * (difficultyUser / difficultyTotal) * this.normalPercentage);
    }

    protected BigDecimal calculateBlockBonus(double blockReward, PayoutsSummary.UserPayoutSummary userPayout)
    {
        return BigDecimal.valueOf(blockReward * this.solverPercentage);
    }

    protected class BlockQueueItem
    extends SimpleCheckpointItem<SolvedBlock>
    {
        public BlockQueueItem(SolvedBlock block)
        {
            super(block.getHash(), block);
        }
    }
}