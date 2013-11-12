package com.redbottledesign.bitcoin.pool.agent.persistence.dedupe;

import java.io.IOException;

import com.redbottledesign.bitcoin.pool.drupal.DrupalSession;
import com.redbottledesign.drupal.Entity;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;

interface DuplicateFinder<T extends Entity<?>>
{
    public boolean wasAlreadySaved(DrupalSession session, T entity)
    throws IOException, DrupalHttpException;
}