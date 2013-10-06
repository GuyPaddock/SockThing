
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

public class DrupalPplnsAgent
extends Thread
implements PplnsAgent
{
  private static final long RETRY_MS = TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS);

  // TODO: Move to config.
  private static final BigDecimal PAYOUT_BLOCK_PERCENTAGE_SOLVER  = BigDecimal.valueOf(0.2d);
  private static final BigDecimal PAYOUT_BLOCK_PERCENTAGE_NORMAL  = BigDecimal.ONE.subtract(PAYOUT_BLOCK_PERCENTAGE_SOLVER);

  private final StratumServer server;
  private final BlockingQueue<SolvedBlock> pendingBlocks;
  private final BlockingQueue<BlockCredit> pendingBlockCredits;
  private final Thread creditsThread;

  public DrupalPplnsAgent(StratumServer server)
  {
    this.setDaemon(true);
    this.setName(this.getClass().getSimpleName());

    this.server = server;

    this.pendingBlocks        = new LinkedBlockingQueue<>();
    this.pendingBlockCredits  = new LinkedBlockingQueue<>();
    this.creditsThread        = new PayoutsRunner(this.pendingBlockCredits);
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
      while ((currentBlock = this.pendingBlocks.take()) != null)
      {
        PayoutsSummary payoutsSummary;

        try
        {
          payoutsSummary = payoutsRequestor.requestPayoutsSummary();
        }

        catch (Throwable ex)
        {
          // Re-queue block.
          this.pendingBlocks.put(currentBlock);

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
          DrupalPplnsAgent.ensureQueued(this.pendingBlockCredits, regularCredit);

          if (currentBlock.getSolvingMember().equals(recipient))
          {
            BlockCredit bonusCredit  = new BlockCredit();

            bonusCredit.setAuthor(poolDaemonReference);
            bonusCredit.setRecipient(recipient);
            bonusCredit.setBlock(currentBlock.asReference());
            bonusCredit.setAmount(this.calculateBlockBonus(currentBlock, userPayout));
            bonusCredit.setCreditType(BlockCredit.Type.BLOCK_SOLUTION_BONUS);

            // Save this in another thread for both performance and reliability
            DrupalPplnsAgent.ensureQueued(this.pendingBlockCredits, bonusCredit);
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
    // reward = blockReward * (difficultyUser / difficultyTotal) * (1.0 - PAYOUT_NORMAL)
    BigDecimal result =
      currentBlock.getReward()
        .multiply(
          BigDecimal.valueOf(userPayout.getOpenDifficultyUser()))
        .divide(
          BigDecimal.valueOf(userPayout.getOpenDifficultyTotal()))
        .multiply(
          PAYOUT_BLOCK_PERCENTAGE_NORMAL);

    return result;
  }

  protected BigDecimal calculateBlockBonus(SolvedBlock currentBlock, PayoutsSummary.UserPayoutSummary userPayout)
  {
    return currentBlock.getReward().multiply(PAYOUT_BLOCK_PERCENTAGE_SOLVER);
  }

  protected static <T> void ensureQueued(BlockingQueue<T> queue, T object)
  {
    boolean saved = false;

    do
    {
      try
      {
        queue.put(object);

        saved = true;
      }

      catch (InterruptedException innerEx)
      {
        // Suppressed; expected
      }
    }
    while (!saved);
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
            "Saving %0.2d %s credit for user ID %s on block ID %s.\n",
            currentCredit.getAmount().doubleValue(),
            currentCredit.getCreditType(),
            currentCredit.getRecipient().getId(),
            currentCredit.getBlock().getId());

          creditRequestor.createNode(currentCredit);
        }

        catch (Throwable ex)
        {
          // Re-queue credit.
          DrupalPplnsAgent.ensureQueued(this.pendingBlockCredits, currentCredit);

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