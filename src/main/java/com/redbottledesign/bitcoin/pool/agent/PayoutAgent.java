package com.redbottledesign.bitcoin.pool.agent;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.StratumServer;
import com.google.bitcoin.core.Address;
import com.redbottledesign.bitcoin.pool.Agent;
import com.redbottledesign.bitcoin.pool.agent.PersistenceAgent.QueueItem;
import com.redbottledesign.bitcoin.pool.agent.PersistenceAgent.QueueItemSieve;
import com.redbottledesign.bitcoin.pool.drupal.DrupalSession;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.BalancesSummaryRequestor;
import com.redbottledesign.bitcoin.pool.drupal.node.Payout;
import com.redbottledesign.bitcoin.pool.drupal.summary.BalancesSummary;
import com.redbottledesign.drupal.Entity;
import com.redbottledesign.drupal.User;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;

public class PayoutAgent
extends Agent
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PayoutAgent.class);
    private static final long PAYOUT_FREQUENCY_MS = TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES);

    private final StratumServer server;
    private final PersistenceAgent persistenceAgent;

    public PayoutAgent(StratumServer server)
    {
        super(PAYOUT_FREQUENCY_MS);

        this.server             = server;
        this.persistenceAgent   = server.getAgent(PersistenceAgent.class);
    }

    @Override
    protected void runPeriodicTask()
    throws IOException, DrupalHttpException
    {
        DrupalSession               session             = this.server.getSession();
        User.Reference              poolDaemonUser      = session.getPoolDaemonUser().asReference();
        BalancesSummaryRequestor    balancesRequestor   = session.getBalancesRequestor();
        BalancesSummary             allUserBalances     = balancesRequestor.requestBalancesSummary();

        if (LOGGER.isInfoEnabled())
            LOGGER.info("Checking for pending payouts...");

        for (BalancesSummary.UserBalanceSummary userBalance : allUserBalances.getBalances())
        {
            BigDecimal  currentUserBalance  = userBalance.getUserBalanceCurrent(),
                        userPayoutMinimum   = userBalance.getUserPayoutMinimum();

            if ((currentUserBalance != null) && (userPayoutMinimum != null) &&
                (currentUserBalance.compareTo(userPayoutMinimum) >= 0))
            {
                int     userId              = userBalance.getUserId();
                String  userPayoutAddress   = userBalance.getUserPayoutAddress();

                if (this.isPayoutPendingForUser(userId))
                {
                    if (LOGGER.isInfoEnabled())
                    {
                        LOGGER.info(
                            String.format(
                                "Not sending a payout to user #%d, since a payout is already pending for this user.",
                                userId));
                    }
                }

                else
                {
                    this.sendPayout(currentUserBalance, userId, userPayoutAddress, poolDaemonUser);
                }
            }
        }

        if (LOGGER.isInfoEnabled())
            LOGGER.info("Payout run complete.");
    }

    protected void sendPayout(BigDecimal currentUserBalance, int userId, String payoutAddress,
                              User.Reference poolDaemonUser)
    {
        if (LOGGER.isInfoEnabled())
        {
            LOGGER.info(
                String.format(
                    "Sending %.2f BTC payout to user #%d at address '%s'.",
                    currentUserBalance.doubleValue(),
                    userId,
                    payoutAddress));
        }

        try
        {
            Payout  payoutRecord    = new Payout();
            Address payeeAddress    = new Address(this.server.getNetworkParameters(), payoutAddress);
            String  paymentHash     = this.server.sendPayment(currentUserBalance, payeeAddress);

            payoutRecord.setAuthor(poolDaemonUser);
            payoutRecord.setRecipient(new User.Reference(userId));
            payoutRecord.setAmount(currentUserBalance);
            payoutRecord.setPaymentAddress(payoutAddress);
            payoutRecord.setPaymentHash(paymentHash);

            this.persistenceAgent.queueForSave(payoutRecord);
        }

        catch (Throwable ex)
        {
            if (LOGGER.isErrorEnabled())
            {
                LOGGER.error(
                    String.format(
                        "Failed to send %.2f BTC payout to user #%d at address '%s' (user skipped): %s\n",
                        currentUserBalance.doubleValue(),
                        userId,
                        payoutAddress,
                        ex.getMessage(),
                        ExceptionUtils.getStackTrace(ex)));
            }
        }
    }

    protected boolean isPayoutPendingForUser(int userId)
    {
        final Integer userIdInteger = userId;

        /* Look through the persistence queue to see if we have any payouts
         * pending for this user.
         */
        return this.persistenceAgent.hasQueuedItemMatchingSieve(
            new QueueItemSieve()
            {

                @Override
                public boolean matches(QueueItem<? extends Entity<?>> queueItem)
                {
                    Entity<?> entity = queueItem.getEntity();

                    return (Payout.class.isAssignableFrom(entity.getClass()) &&
                            userIdInteger.equals(((Payout)entity).getRecipient().getId()));
                }
            });
    }
}