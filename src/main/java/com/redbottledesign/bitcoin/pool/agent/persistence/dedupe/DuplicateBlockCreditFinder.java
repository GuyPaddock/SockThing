package com.redbottledesign.bitcoin.pool.agent.persistence.dedupe;

import java.io.IOException;

import com.redbottledesign.bitcoin.pool.drupal.DrupalSession;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.BlockCreditRequestor;
import com.redbottledesign.bitcoin.pool.drupal.node.BlockCredit;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;

class DuplicateBlockCreditFinder
extends DuplicateFinder<BlockCredit>
{
    @Override
    public boolean wasAlreadySaved(DrupalSession session, final BlockCredit entity)
    throws IOException, DrupalHttpException
    {
        BlockCreditRequestor    requestor       = session.getCreditRequestor();
        BlockCredit             existingEntity;

        existingEntity =
            requestor.getCredit(
                entity.getBlock().getId(),
                entity.getRecipient().getId(),
                entity.getCreditType());

        return this.wasAlreadySaved(existingEntity, entity);
    }
}