package com.redbottledesign.bitcoin.pool.agent.persistence.dedupe;

import java.io.IOException;

import com.redbottledesign.bitcoin.pool.drupal.DrupalSession;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.PayoutRequestor;
import com.redbottledesign.bitcoin.pool.drupal.node.Payout;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;

class DuplicatePayoutFinder
implements DuplicateFinder<Payout>
{
    @Override
    public boolean wasAlreadySaved(DrupalSession session, final Payout entity)
    throws IOException, DrupalHttpException
    {
        PayoutRequestor requestor       = session.getPayoutRequestor();
        Payout          existingEntity  = requestor.getPayout(entity.getPaymentHash(), entity.getPaymentAddress());

        return (existingEntity == null);
    }
}