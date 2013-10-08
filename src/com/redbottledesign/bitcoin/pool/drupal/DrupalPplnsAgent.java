
package com.redbottledesign.bitcoin.pool.drupal;

import java.math.BigDecimal;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.github.fireduck64.sockthing.EventLog;
import com.github.fireduck64.sockthing.PplnsAgent;
import com.github.fireduck64.sockthing.StratumServer;
import com.redbottledesign.bitcoin.pool.Agent;
import com.redbottledesign.bitcoin.pool.agent.PersistenceAgent;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.PayoutsSummaryRequestor;
import com.redbottledesign.bitcoin.pool.drupal.node.BlockCredit;
import com.redbottledesign.bitcoin.pool.drupal.node.SolvedBlock;
import com.redbottledesign.bitcoin.pool.drupal.summary.PayoutsSummary;
import com.redbottledesign.drupal.Node;
import com.redbottledesign.drupal.User;
import com.redbottledesign.util.QueueUtils;

public class DrupalPplnsAgent
extends Agent
implements PplnsAgent
{
  // TODO: Move to config.
  private static final float PAYOUT_BLOCK_PERCENTAGE_SOLVER = 0.2f;
  private static final float PAYOUT_BLOCK_PERCENTAGE_NORMAL = 1.0f - PAYOUT_BLOCK_PERCENTAGE_SOLVER;
  private static final float POOL_FEE = 0.015f;

  private final StratumServer server;
  private final BlockingQueue<SolvedBlock> pendingBlockQueue;

  public DrupalPplnsAgent(StratumServer server)
  {
    super();

    this.server             = server;
    this.pendingBlockQueue  = new LinkedBlockingQueue<>();
  }

  @Override
  public void payoutBlock(SolvedBlock block)
  {
    QueueUtils.ensureQueued(this.pendingBlockQueue, block);
  }

  @Override
  protected void runPeriodicTask()
  throws InterruptedException
  {
    EventLog                eventLog          = this.server.getEventLog();
    PersistenceAgent        persistenceAgent  = this.server.getPersistenceAgent();
    DrupalSession           session           = this.server.getSession();
    PayoutsSummaryRequestor payoutsRequestor  = session.getPayoutsSummaryRequestor();
    User.Reference          poolDaemonUser    = session.getPoolDaemonUser().asReference();
    SolvedBlock             currentBlock;

    while ((currentBlock = this.pendingBlockQueue.take()) != null)
    {
      float           blockReward           = currentBlock.getReward().floatValue() * (1f - POOL_FEE);
      Node.Reference  currentBlockReference = currentBlock.asReference();
      PayoutsSummary  payoutsSummary;

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

        eventLog.log(error);

        throw new RuntimeException(error, ex);
      }

      for (PayoutsSummary.UserPayoutSummary userPayout : payoutsSummary.getPayouts())
      {
        User.Reference  recipient = userPayout.getUserReference();

        this.creditUserForBlock(
          recipient,
          this.calculateBlockCredit(blockReward, userPayout),
          currentBlockReference,
          BlockCredit.Type.REGULAR_SHARE,
          persistenceAgent,
          poolDaemonUser);

        if (currentBlock.getSolvingMember().equals(recipient))
        {
          this.creditUserForBlock(
            recipient,
            this.calculateBlockBonus(blockReward, userPayout),
            currentBlockReference,
            BlockCredit.Type.BLOCK_SOLUTION_BONUS,
            persistenceAgent,
            poolDaemonUser);
        }
      }
    }
  }

  protected void creditUserForBlock(User.Reference user, BigDecimal creditAmount, Node.Reference block,
                                    BlockCredit.Type creditType, PersistenceAgent persistenceAgent,
                                    User.Reference poolDaemonUser)
  {
    BlockCredit newCredit = new BlockCredit();

    newCredit.setAuthor(poolDaemonUser);
    newCredit.setRecipient(user);
    newCredit.setBlock(block);
    newCredit.setAmount(creditAmount);
    newCredit.setCreditType(creditType);

    persistenceAgent.queueForSave(newCredit);
  }

  protected BigDecimal calculateBlockCredit(float blockReward, PayoutsSummary.UserPayoutSummary userPayout)
  {
    float difficultyUser  = userPayout.getOpenDifficultyUser(),
          difficultyTotal = userPayout.getOpenDifficultyTotal();

    return BigDecimal.valueOf(blockReward * (difficultyUser / difficultyTotal) * PAYOUT_BLOCK_PERCENTAGE_NORMAL);
  }

  protected BigDecimal calculateBlockBonus(float blockReward, PayoutsSummary.UserPayoutSummary userPayout)
  {
    return BigDecimal.valueOf(blockReward * PAYOUT_BLOCK_PERCENTAGE_SOLVER);
  }
}