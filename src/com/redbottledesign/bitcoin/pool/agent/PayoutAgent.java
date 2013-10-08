package com.redbottledesign.bitcoin.pool.agent;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.github.fireduck64.sockthing.EventLog;
import com.github.fireduck64.sockthing.StratumServer;
import com.google.bitcoin.core.Address;
import com.redbottledesign.bitcoin.pool.Agent;
import com.redbottledesign.bitcoin.pool.PersistenceCallback;
import com.redbottledesign.bitcoin.pool.drupal.DrupalSession;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.BalancesSummaryRequestor;
import com.redbottledesign.bitcoin.pool.drupal.node.Payout;
import com.redbottledesign.bitcoin.pool.drupal.summary.BalancesSummary;
import com.redbottledesign.drupal.User;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;

public class PayoutAgent
extends Agent
{
  private static final long PAYOUT_FREQUENCY_MS = TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS); // FIXME: Payout every 15 mins!

  private final StratumServer server;
  private final EventLog eventLog;
  private final Set<Integer> userIdsAlreadyBeingUpdated;

  public PayoutAgent(StratumServer server)
  {
    super(PAYOUT_FREQUENCY_MS);

    this.server                     = server;
    this.eventLog                   = server.getEventLog();
    this.userIdsAlreadyBeingUpdated = new HashSet<>();
  }

  @Override
  protected void runPeriodicTask()
  throws IOException, DrupalHttpException
  {
    DrupalSession             session           = this.server.getSession();
    User.Reference            poolDaemonUser    = session.getPoolDaemonUser().asReference();
    BalancesSummaryRequestor  balancesRequestor = session.getBalancesRequestor();
    BalancesSummary           allUserBalances   = balancesRequestor.requestBalancesSummary();

    eventLog.log("Checking for pending payouts...");

    for (BalancesSummary.UserBalanceSummary userBalance : allUserBalances.getBalances())
    {
      BigDecimal currentUserBalance = userBalance.getUserBalanceCurrent();
      BigDecimal userPayoutMinimum  = userBalance.getUserPayoutMinimum();

      if ((currentUserBalance != null) && (userPayoutMinimum != null) &&
          (currentUserBalance.compareTo(userPayoutMinimum) >= 0))
      {
        int userId = userBalance.getUserId();

        String  userPayoutAddress = userBalance.getUserPayoutAddress();

        if (this.isPayoutPendingForUser(userId))
        {
          String message =
              String.format("Not sending a payout to user #%d, since a payout is already pending for this user.",
                userId);

          System.out.println(message);
          this.eventLog.log(message);
        }

        else
        {
          this.sendPayout(currentUserBalance, userId, userPayoutAddress, poolDaemonUser);
        }
      }
    }
  }

  protected void sendPayout(BigDecimal currentUserBalance, int userId, String payoutAddress,
                            User.Reference poolDaemonUser)
  {
    String message =
        String.format("Sending %.2f BTC payout to user #%d at address '%s'.",
          currentUserBalance.doubleValue(),
          userId,
          payoutAddress);

    System.out.println(message);
    this.eventLog.log(message);

    try
    {
      Payout  payoutRecord  = new Payout();
      Address payeeAddress  = new Address(this.server.getNetworkParameters(), payoutAddress);
      String  paymentHash   = this.server.sendPayment(currentUserBalance, payeeAddress);

      payoutRecord.setAuthor(poolDaemonUser);
      payoutRecord.setRecipient(new User.Reference(userId));
      payoutRecord.setAmount(currentUserBalance);
      payoutRecord.setPaymentAddress(payoutAddress);
      payoutRecord.setPaymentHash(paymentHash);

      this.queuePayoutRecordForPersistence(payoutRecord);
    }

    catch (Throwable ex)
    {
      String error = String.format(
        "Failed to send %.2f BTC payout to user #%d at address '%s' (user skipped): %s",
        currentUserBalance.doubleValue(),
        userId,
        payoutAddress,
        ex.getMessage());

      this.eventLog.log(error);
      System.err.println(error);
      ex.printStackTrace();
    }
  }

  protected void queuePayoutRecordForPersistence(Payout payoutRecord)
  {
    // Prevent the user for this payout from getting any other pay-outs until their balance is up-to-date.
    this.userIdsAlreadyBeingUpdated.add(payoutRecord.getRecipient().getId());

    this.server.getPersistenceAgent().queueForSave(payoutRecord, new PersistenceCallback<Payout>()
    {
      @Override
      public void onEntitySaved(Payout savedPayout)
      {
        // Make the user eligible for payouts again.
        PayoutAgent.this.releasePayoutLockOnUser(savedPayout.getRecipient().getId());
      }

      @Override
      public void onEntityEvicted(Payout evictedPayout)
      {
        // FIXME: Is this really the best way to deal with this?
        PayoutAgent.this.releasePayoutLockOnUser(evictedPayout.getRecipient().getId());
      }
    });
  }

  protected boolean isPayoutPendingForUser(int userId)
  {
    synchronized (this.userIdsAlreadyBeingUpdated)
    {
      return this.userIdsAlreadyBeingUpdated.contains(userId);
    }
  }

  protected void releasePayoutLockOnUser(int userId)
  {
    synchronized (this.userIdsAlreadyBeingUpdated)
    {
      this.userIdsAlreadyBeingUpdated.remove(userId);
    }
  }
}