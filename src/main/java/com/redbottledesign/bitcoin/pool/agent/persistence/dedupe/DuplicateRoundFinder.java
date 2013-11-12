package com.redbottledesign.bitcoin.pool.agent.persistence.dedupe;

import java.io.IOException;

import com.redbottledesign.bitcoin.pool.drupal.DrupalSession;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.RoundRequestor;
import com.redbottledesign.bitcoin.pool.drupal.node.Round;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;

public class DuplicateRoundFinder
extends DuplicateFinder<Round>
{
    @Override
    public boolean wasAlreadySaved(DrupalSession session, Round entity)
    throws IOException, DrupalHttpException
    {
        RoundRequestor  requestor       = session.getRoundRequestor();
        Round           existingEntity;

        existingEntity = requestor.getRound(entity.getRoundDates().getStartDate());

        return this.wasAlreadySaved(existingEntity, entity);
    }
}
