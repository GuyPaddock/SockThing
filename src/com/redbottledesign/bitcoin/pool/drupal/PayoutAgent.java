package com.redbottledesign.bitcoin.pool.drupal;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.github.fireduck64.sockthing.EventLog;
import com.github.fireduck64.sockthing.StratumServer;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.BalancesSummaryRequestor;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.PayoutRequestor;
import com.redbottledesign.bitcoin.pool.drupal.node.Payout;
import com.redbottledesign.bitcoin.pool.drupal.summary.BalancesSummary;
import com.redbottledesign.drupal.User;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;
import com.redbottledesign.util.QueueUtils;

public class PayoutAgent
extends Thread
{
  private static final long DB_CHECK_MS = TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES);
  private static final long RETRY_MS = TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS);

  private long lastCheck;
  private final StratumServer server;
  private final BlockingQueue<Payout> payoutPersistenceQueue;
  private final Thread payoutPersistenceThread;

  public PayoutAgent(StratumServer server)
  {
    this.setDaemon(true);
    this.setName(this.getClass().getSimpleName());

    this.server                   = server;
    this.payoutPersistenceQueue   = new LinkedBlockingQueue<>();
    this.payoutPersistenceThread  = new PayoutPersistenceRunner(server, this.payoutPersistenceQueue);
  }

  /**
   * Do the actual update in this thread to avoid ever blocking work generation
   */
  @Override
  public void run()
  {
    this.payoutPersistenceThread.start();

    while (true)
    {
      try
      {
        if (System.currentTimeMillis() > (lastCheck + DB_CHECK_MS))
        {
          this.runPayouts();

          this.lastCheck = System.currentTimeMillis();
        }
      }

      catch (Throwable t)
      {
        // Top-level handler
        t.printStackTrace();
      }

      synchronized(this)
      {
        try
        {
          // FIXME: Switch to scheduled threads.
          this.wait(DB_CHECK_MS / 4);
        }

        catch (InterruptedException e)
        {
          // Suppressed; expected
        }
      }
    }
  }

  protected void runPayouts()
  throws IOException, DrupalHttpException
  {
    EventLog                      eventLog          = this.server.getEventLog();
    SingletonDrupalSessionFactory sessionFactory    = SingletonDrupalSessionFactory.getInstance();
    User.Reference                poolDaemonUser    = sessionFactory.getPoolDaemonUser().asReference();
    BalancesSummaryRequestor      balancesRequestor = sessionFactory.getBalancesRequestor();
    BalancesSummary               allUserBalances   = balancesRequestor.requestBalancesSummary();

    eventLog.log("Checking for pending payouts...");

    for (BalancesSummary.UserBalanceSummary userBalance : allUserBalances.getBalances())
    {
      BigDecimal currentUserBalance = userBalance.getUserBalanceCurrent();
      BigDecimal userPayoutMinimum = userBalance.getUserPayoutMinimum();

      if ((currentUserBalance != null) && (userPayoutMinimum != null) &&
          (currentUserBalance.compareTo(userPayoutMinimum) >= 0))
      {
        int     userId            = userBalance.getUserId();
        String  userPayoutAddress = userBalance.getUserPayoutAddress();

        String message =
          String.format("Sending %.2f BTC payout to user #%d at address '%s'.",
            currentUserBalance.doubleValue(),
            userId,
            userPayoutAddress);

        eventLog.log(message);

        try
        {
          this.sendPayout(currentUserBalance, userId, userPayoutAddress, poolDaemonUser);
        }

        catch (AddressFormatException e)
        {
          String error = String.format(
            "Failed to send %.2f BTC payout to user #%d at address '%s' (user skipped): %s",
            currentUserBalance.doubleValue(),
            userId,
            userPayoutAddress,
            e.getMessage());

          eventLog.log(error);
          System.err.println(error);
          e.printStackTrace();
        }
      }
    }
  }

  protected void sendPayout(BigDecimal currentUserBalance, int userId, String payoutAddress,
                            User.Reference poolDaemonUser)
  throws AddressFormatException
  {
    Payout  payoutRecord  = new Payout();
    String  paymentHash   = this.server.sendPayment(currentUserBalance, new Address(null, payoutAddress));

    payoutRecord.setAuthor(poolDaemonUser);
    payoutRecord.setRecipient(new User.Reference(userId));
    payoutRecord.setAmount(currentUserBalance);
    payoutRecord.setPaymentAddress(payoutAddress);
    payoutRecord.setPaymentHash(paymentHash);

    this.queuePayoutPersistence(payoutRecord);
  }

  protected void queuePayoutPersistence(Payout payout)
  {
    QueueUtils.ensureQueued(this.payoutPersistenceQueue, payout);
  }

  protected static class PayoutPersistenceRunner
  extends Thread
  {
    protected final StratumServer server;
    protected final BlockingQueue<Payout> payoutPersistenceQueue;

    public PayoutPersistenceRunner(StratumServer server, BlockingQueue<Payout> payoutPersistenceQueue)
    {
      this.server                 = server;
      this.payoutPersistenceQueue = payoutPersistenceQueue;
    }

    @Override
    public void run()
    {
      while (true)
      {
        try
        {
          /* NOTE: This will block indefinitely for items.
           *
           * We still call this method in a loop in case we get interrupted.
           */
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
      EventLog                      eventLog        = this.server.getEventLog();
      SingletonDrupalSessionFactory sessionFactory  = SingletonDrupalSessionFactory.getInstance();
      PayoutRequestor               payoutRequestor = sessionFactory.getPayoutRequestor();
      Payout                        payoutRecord;

      while ((payoutRecord = this.payoutPersistenceQueue.take()) != null)
      {
        double  paymentAmount   = payoutRecord.getAmount().doubleValue();
        Integer recipientId     = payoutRecord.getRecipient().getId();
        String  paymentAddress  = payoutRecord.getPaymentAddress(),
                message;

        message =
          String.format("Saving record of a %.2f BTC payout to user #%d at address '%s'.",
            paymentAmount,
            recipientId,
            paymentAddress);

        eventLog.log(message);

        try
        {
          payoutRequestor.createNode(payoutRecord);
        }

        catch (Throwable ex)
        {
          // Re-queue payout record.
          QueueUtils.ensureQueued(this.payoutPersistenceQueue, payoutRecord);

          String error = String.format(
            "A %.2f BTC payout to user #%d succeeded but failed to be written: %s",
            paymentAmount,
            recipientId,
            paymentAddress,
            ex.getMessage());

          eventLog.log(error);
          System.err.println(error);
          ex.printStackTrace();
        }
      }
    }
  }
}