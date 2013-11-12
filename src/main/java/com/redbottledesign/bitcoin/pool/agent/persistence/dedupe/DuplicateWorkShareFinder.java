package com.redbottledesign.bitcoin.pool.agent.persistence.dedupe;

import java.io.IOException;

import com.redbottledesign.bitcoin.pool.drupal.DrupalSession;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.WorkShareRequestor;
import com.redbottledesign.bitcoin.pool.drupal.node.WorkShare;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;

class DuplicateWorkShareFinder
extends DuplicateFinder<WorkShare>
{
    @Override
    public boolean wasAlreadySaved(DrupalSession session, final WorkShare entity)
    throws IOException, DrupalHttpException
    {
        WorkShareRequestor  requestor       = session.getShareRequestor();
        WorkShare           existingEntity;

        existingEntity =
            requestor.getShare(entity.getJobHash(), entity.getSubmitter().getId(), entity.getRound().getId());

        return this.wasAlreadySaved(existingEntity, entity);
    }
}