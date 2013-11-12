package com.redbottledesign.bitcoin.pool.agent.persistence.dedupe;

import java.io.IOException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redbottledesign.bitcoin.pool.drupal.DrupalSession;
import com.redbottledesign.drupal.Entity;
import com.redbottledesign.drupal.Node;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;

abstract class DuplicateFinder<T extends Entity<?>>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DuplicateDetector.class);

    public abstract boolean wasAlreadySaved(DrupalSession session, T entity)
    throws IOException, DrupalHttpException;

    protected boolean wasAlreadySaved(Node existingEntity, Node updatedEntity)
    {
        boolean result;

        if (updatedEntity.isNew())
        {
            result = (existingEntity == null);

            if (LOGGER.isTraceEnabled())
                LOGGER.trace("wasAlreadySaved(): entity is new. Have existing entity: " + result);
        }

        else
        {
            Date      existingDate      = existingEntity.getDateChanged();
            Date      updatedDate       = updatedEntity.getDateChanged();
            Integer   existingRevision  = existingEntity.getRevisionId();
            Integer   updatedRevision   = updatedEntity.getRevisionId();

            if (updatedRevision == null)
                updatedRevision = 0;

            if (updatedDate == null)
                updatedDate = new Date();

            result = ((existingDate.after(updatedDate)) || (existingRevision > updatedRevision));

            if (LOGGER.isTraceEnabled())
            {
                LOGGER.trace(
                    String.format(
                        "wasAlreadySaved(): entity is being updated.\n" +
                        "Revision: existing (remote) - %d, updated (local) - %d.\n" +
                        "Date: existing (remote) - %s, updated (local) - %s.\n" +
                        "Result: %b",
                        existingRevision,
                        updatedRevision,
                        existingDate,
                        updatedDate,
                        result));
            }
        }

        return result;
    }
}