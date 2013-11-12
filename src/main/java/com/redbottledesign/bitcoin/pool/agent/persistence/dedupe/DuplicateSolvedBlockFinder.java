package com.redbottledesign.bitcoin.pool.agent.persistence.dedupe;

import java.io.IOException;

import com.redbottledesign.bitcoin.pool.drupal.DrupalSession;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.SolvedBlockRequestor;
import com.redbottledesign.bitcoin.pool.drupal.node.SolvedBlock;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;

class DuplicateSolvedBlockFinder
extends DuplicateFinder<SolvedBlock>
{
    @Override
    public boolean wasAlreadySaved(DrupalSession session, final SolvedBlock entity)
    throws IOException, DrupalHttpException
    {
        SolvedBlockRequestor    requestor       = session.getBlockRequestor();
        SolvedBlock             existingEntity  = requestor.getBlock(entity.getHash(), entity.getHeight());

        return this.wasAlreadySaved(existingEntity, entity);
    }
}