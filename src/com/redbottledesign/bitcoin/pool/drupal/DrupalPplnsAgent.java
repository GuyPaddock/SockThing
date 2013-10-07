
package com.redbottledesign.bitcoin.pool.drupal;

import java.math.BigDecimal;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.github.fireduck64.sockthing.PplnsAgent;
import com.github.fireduck64.sockthing.StratumServer;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.BlockCreditRequestor;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.PayoutsSummaryRequestor;
import com.redbottledesign.bitcoin.pool.drupal.node.BlockCredit;
import com.redbottledesign.bitcoin.pool.drupal.node.SolvedBlock;
import com.redbottledesign.bitcoin.pool.drupal.summary.PayoutsSummary;
import com.redbottledesign.drupal.User;
import com.redbottledesign.util.QueueUtils;

public class DrupalPplnsAgent
extends Thread
implements PplnsAgent
{
  private static final long RETRY_MS = TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS);

  // TODO: Move to config.
  private static final float PAYOUT_BLOCK_PERCENTAGE_SOLVER = 0.2f;
  private static final float PAYOUT_BLOCK_PERCENTAGE_NORMAL = 1.0f - PAYOUT_BLOCK_PERCENTAGE_SOLVER;
  private static final float POOL_FEE = 0.015f;

  private final StratumServer server;
  private final BlockingQueue<SolvedBlock> pendingBlockQueue;
  private final BlockingQueue<BlockCredit> pendingBlockCreditQueue;
  private final Thread creditsThread;

  public DrupalPplnsAgent(StratumServer server)
  {
    this.setDaemon(true);
    this.setName(this.getClass().getSimpleName());

    this.server = server;

    this.pendingBlockQueue        = new LinkedBlockingQueue<>();
    this.pendingBlockCreditQueue  = new LinkedBlockingQueue<>();
    this.creditsThread            = new PayoutsRunner(this.pendingBlockCreditQueue);
  }

  @Override
  public BlockingQueue<SolvedBlock> getPendingBlockQueue()
  {
    return this.pendingBlockQueue;
  }

  @Override
  public void run()
  {
    this.creditsThread.start();

    while (true)
    {
      try
      {
        this.runPplnsCredits();
      }

      catch (Throwable ex)
      {
        ex.printStackTrace();
      }

      synchronized(this)
      {
        try
        {
          this.wait(RETRY_MS);
        }

        catch (InterruptedException e)
        {
          // Suppressed; expected
        }
      }
    }
  }

  protected void runPplnsCredits()
  {
    SolvedBlock                   currentBlock;
    SingletonDrupalSessionFactory sessionFactory      = SingletonDrupalSessionFactory.getInstance();
    PayoutsSummaryRequestor       payoutsRequestor    = sessionFactory.getPayoutsRequestor();
    User.Reference                poolDaemonReference = sessionFactory.getPoolDaemonUser().asReference();

    try
    {
      /* NOTE: This will block indefinitely for items.
       *
       * We still call this method in a loop in case we get interrupted.
       */
      while ((currentBlock = this.pendingBlockQueue.take()) != null)
      {
        PayoutsSummary payoutsSummary;

        try
        {
          payoutsSummary = payoutsRequestor.requestPayoutsSummary();
        }

        catch (Throwable ex)
        {
          // Re-queue block.
          this.pendingBlockQueue.put(currentBlock);

          throw new RuntimeException(
            String.format(
              "Failed to request payouts summary while handling block '%s' (queued to retry): %s",
               currentBlock,
               ex.getMessage()),
            ex);
        }

        for (PayoutsSummary.UserPayoutSummary userPayout : payoutsSummary.getPayouts())
        {
          BlockCredit     regularCredit  = new BlockCredit();
          User.Reference  recipient   = userPayout.getUserReference();

          regularCredit.setAuthor(poolDaemonReference);
          regularCredit.setRecipient(recipient);
          regularCredit.setBlock(currentBlock.asReference());
          regularCredit.setAmount(this.calculateBlockCredit(currentBlock, userPayout));
          regularCredit.setCreditType(BlockCredit.Type.REGULAR_SHARE);

          // Save this in another thread for both performance and reliability
          QueueUtils.ensureQueued(this.pendingBlockCreditQueue, regularCredit);

          if (currentBlock.getSolvingMember().equals(recipient))
          {
            BlockCredit bonusCredit  = new BlockCredit();

            bonusCredit.setAuthor(poolDaemonReference);
            bonusCredit.setRecipient(recipient);
            bonusCredit.setBlock(currentBlock.asReference());
            bonusCredit.setAmount(this.calculateBlockBonus(currentBlock, userPayout));
            bonusCredit.setCreditType(BlockCredit.Type.BLOCK_SOLUTION_BONUS);

            // Save this in another thread for both performance and reliability
            QueueUtils.ensureQueued(this.pendingBlockCreditQueue, bonusCredit);
          }
        }
      }
    }

    catch (InterruptedException e)
    {
      // Suppress -- expected.
    }
  }

  protected BigDecimal calculateBlockCredit(SolvedBlock currentBlock, PayoutsSummary.UserPayoutSummary userPayout)
  {
    float blockReward     = currentBlock.getReward().floatValue(),
          difficultyUser  = userPayout.getOpenDifficultyUser(),
          difficultyTotal = userPayout.getOpenDifficultyTotal(),
          credit          = blockReward * (difficultyUser / difficultyTotal) * (PAYOUT_BLOCK_PERCENTAGE_NORMAL - POOL_FEE);

    return BigDecimal.valueOf(credit);
  }

  protected BigDecimal calculateBlockBonus(SolvedBlock currentBlock, PayoutsSummary.UserPayoutSummary userPayout)
  {
    float blockReward     = currentBlock.getReward().floatValue(),
          credit          = blockReward * (PAYOUT_BLOCK_PERCENTAGE_SOLVER - POOL_FEE);

    return BigDecimal.valueOf(credit);
  }

  protected static class PayoutsRunner
  extends Thread
  {
    private final BlockingQueue<BlockCredit> pendingBlockCredits;

    public PayoutsRunner(BlockingQueue<BlockCredit> pendingBlockCredits)
    {
      this.setDaemon(true);
      this.setName(this.getClass().getSimpleName());

      this.pendingBlockCredits = pendingBlockCredits;
    }

    @Override
    public void run()
    {
      while (true)
      {
        try
        {
          this.runPayouts();
        }

        catch (Throwable ex)
        {
          ex.printStackTrace();
        }

        synchronized(this)
        {
          try
          {
            this.wait(RETRY_MS);
          }

          catch (InterruptedException e)
          {
            // Suppressed; expected
          }
        }
      }
    }

    protected void runPayouts()
    throws InterruptedException
    {
      SingletonDrupalSessionFactory sessionFactory  = SingletonDrupalSessionFactory.getInstance();
      BlockCreditRequestor          creditRequestor = sessionFactory.getCreditRequestor();
      BlockCredit currentCredit;

      while ((currentCredit = pendingBlockCredits.take()) != null)
      {
        try
        {
          System.out.printf(
            "Saving %.2f %s credit for user ID %s on block ID %s.\n",
            currentCredit.getAmount().doubleValue(),
            currentCredit.getCreditType(),
            currentCredit.getRecipient().getId(),
            currentCredit.getBlock().getId());

          creditRequestor.createNode(currentCredit);
        }

        catch (Throwable ex)
        {
          // Re-queue credit.
          QueueUtils.ensureQueued(this.pendingBlockCredits, currentCredit);

          throw new RuntimeException(
            String.format(
              "Failed to save credit '%s' (queued to retry): %s",
               currentCredit,
               ex.getMessage()),
            ex);
        }
      }
    }
  }
}