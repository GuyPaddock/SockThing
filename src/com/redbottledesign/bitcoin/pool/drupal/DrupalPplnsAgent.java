package com.redbottledesign.bitcoin.pool.drupal;

import java.math.BigDecimal;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.Config;
import com.github.fireduck64.sockthing.PplnsAgent;
import com.github.fireduck64.sockthing.StratumServer;
import com.redbottledesign.bitcoin.pool.Agent;
import com.redbottledesign.bitcoin.pool.agent.PersistenceAgent;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.PayoutsSummaryRequestor;
import com.redbottledesign.bitcoin.pool.drupal.node.BlockCredit;
import com.redbottledesign.bitcoin.pool.drupal.node.SolvedBlock;
import com.redbottledesign.bitcoin.pool.drupal.summary.PayoutsSummary;
import com.redbottledesign.drupal.User;
import com.redbottledesign.util.QueueUtils;

public class DrupalPplnsAgent
extends Agent
implements PplnsAgent
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DrupalPplnsAgent.class);

    private static final String CONFIG_VALUE_PAYOUT_BLOCK_PERCENTAGE_SOLVER = "payout_block_percentage_solver";
    private static final String CONFIG_VALUE_PAYOUT_BLOCK_PERCENTAGE_NORMAL = "payout_block_percentage_normal";
    private static final String CONFIG_VALUE_PAYOUT_BLOCK_PERCENTAGE_POOL_FEE = "payout_block_percentage_pool_fee";

    private final StratumServer server;
    private final BlockingQueue<SolvedBlock> pendingBlockQueue;
    private final Config config;

    public DrupalPplnsAgent(StratumServer server)
    {
        super();

        this.server             = server;
        this.config             = server.getConfig();
        this.pendingBlockQueue  = new LinkedBlockingQueue<>();
    }

    @Override
    public void payoutBlock(SolvedBlock block)
    {
        if (LOGGER.isInfoEnabled())
            LOGGER.info(String.format("Queuing payouts for block %d.", block.getHeight()));

        QueueUtils.ensureQueued(this.pendingBlockQueue, block);
    }

    @Override
    protected void checkConfig()
    {
        this.config.require(CONFIG_VALUE_PAYOUT_BLOCK_PERCENTAGE_SOLVER);
        this.config.require(CONFIG_VALUE_PAYOUT_BLOCK_PERCENTAGE_NORMAL);
        this.config.require(CONFIG_VALUE_PAYOUT_BLOCK_PERCENTAGE_POOL_FEE);
    }

    @Override
    protected void runPeriodicTask()
    throws InterruptedException
    {
        PersistenceAgent        persistenceAgent    = this.server.getPersistenceAgent();
        DrupalSession           session             = this.server.getSession();
        PayoutsSummaryRequestor payoutsRequestor    = session.getPayoutsSummaryRequestor();
        User.Reference          poolDaemonUser      = session.getPoolDaemonUser().asReference();
        SolvedBlock             currentBlock;

        while ((currentBlock = this.pendingBlockQueue.take()) != null)
        {
            double          blockReward = currentBlock.getReward().floatValue() * (1d - this.getPoolFeePercentage());
            PayoutsSummary  payoutsSummary;

            if (LOGGER.isInfoEnabled())
            {
                LOGGER.info("Running payouts for block %d...", currentBlock.getHeight());
            }

            try
            {
                payoutsSummary = payoutsRequestor.requestPayoutsSummary();
            }

            catch (Throwable ex)
            {
                String error;

                // Re-queue block.
                QueueUtils.ensureQueued(this.pendingBlockQueue, currentBlock);

                error =
                    String.format(
                        "Failed to request payouts summary while handling block '%s' (queued to retry): %s",
                        currentBlock,
                        ex.getMessage());

                if (LOGGER.isErrorEnabled())
                    LOGGER.error(error + "\n" + ExceptionUtils.getStackTrace(ex));

                throw new RuntimeException(error, ex);
            }

            for (PayoutsSummary.UserPayoutSummary userPayout : payoutsSummary.getPayouts())
            {
                User.Reference recipient = userPayout.getUserReference();

                this.creditUserForBlock(
                    recipient,
                    this.calculateBlockCredit(blockReward, userPayout),
                    currentBlock,
                    BlockCredit.Type.REGULAR_SHARE,
                    persistenceAgent,
                    poolDaemonUser);

                if (currentBlock.getSolvingMember().equals(recipient))
                {
                    this.creditUserForBlock(
                        recipient,
                        this.calculateBlockBonus(blockReward, userPayout),
                        currentBlock,
                        BlockCredit.Type.BLOCK_SOLUTION_BONUS,
                        persistenceAgent,
                        poolDaemonUser);
                }
            }
        }
    }

    protected void creditUserForBlock(User.Reference user, BigDecimal creditAmount, SolvedBlock block,
                                      BlockCredit.Type creditType, PersistenceAgent persistenceAgent,
                                      User.Reference poolDaemonUser)
    {
        BlockCredit newCredit = new BlockCredit();

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

        newCredit.setAuthor(poolDaemonUser);
        newCredit.setRecipient(user);
        newCredit.setBlock(block.asReference());
        newCredit.setAmount(creditAmount);
        newCredit.setCreditType(creditType);

        persistenceAgent.queueForSave(newCredit);
    }

    protected BigDecimal calculateBlockCredit(double blockReward, PayoutsSummary.UserPayoutSummary userPayout)
    {
        float difficultyUser    = userPayout.getOpenDifficultyUser(),
              difficultyTotal   = userPayout.getOpenDifficultyTotal();

        return BigDecimal.valueOf(blockReward * (difficultyUser / difficultyTotal) * this.getPayoutPercentageNormal());
    }

    protected BigDecimal calculateBlockBonus(double blockReward, PayoutsSummary.UserPayoutSummary userPayout)
    {
        return BigDecimal.valueOf(blockReward * this.getPayoutPercentageSolver());
    }

    protected double getPayoutPercentageSolver()
    {
        return this.config.getDouble(CONFIG_VALUE_PAYOUT_BLOCK_PERCENTAGE_SOLVER);
    }

    protected double getPayoutPercentageNormal()
    {
        return this.config.getDouble(CONFIG_VALUE_PAYOUT_BLOCK_PERCENTAGE_NORMAL);
    }

    protected double getPoolFeePercentage()
    {
        return this.config.getDouble(CONFIG_VALUE_PAYOUT_BLOCK_PERCENTAGE_POOL_FEE);
    }
}