package com.redbottledesign.bitcoin.pool.agent.persistence.dedupe;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redbottledesign.bitcoin.pool.drupal.DrupalSession;
import com.redbottledesign.bitcoin.pool.drupal.node.BlockCredit;
import com.redbottledesign.bitcoin.pool.drupal.node.Payout;
import com.redbottledesign.bitcoin.pool.drupal.node.Round;
import com.redbottledesign.bitcoin.pool.drupal.node.SolvedBlock;
import com.redbottledesign.bitcoin.pool.drupal.node.WorkShare;
import com.redbottledesign.drupal.Entity;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;

@SuppressWarnings("serial")
public class DuplicateDetector
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DuplicateDetector.class);

    protected static Map<Class<? extends Entity<?>>, DuplicateFinder<?>> DISPATCHER_REGISTRY =
        new HashMap<Class<? extends Entity<?>>, DuplicateFinder<?>>()
        {{
            put(BlockCredit.class,  new DuplicateBlockCreditFinder());
            put(Payout.class,       new DuplicatePayoutFinder());
            put(Round.class,        new DuplicateRoundFinder());
            put(SolvedBlock.class,  new DuplicateSolvedBlockFinder());
            put(WorkShare.class,    new DuplicateWorkShareFinder());
        }};

    @SuppressWarnings("unchecked")
    public static <T extends Entity<?>>boolean wasEntityAlreadySaved(DrupalSession session, T entity)
    throws IOException, DrupalHttpException
    {
        boolean             result          = false;
        DuplicateFinder<T>  duplicateFinder = (DuplicateFinder<T>)DISPATCHER_REGISTRY.get(entity.getClass());

        if (LOGGER.isTraceEnabled())
            LOGGER.trace("wasEntityAlreadySaved(): Looking for existing, already-saved duplicate of entity: " + entity);

        if (duplicateFinder == null)
        {
            if (LOGGER.isTraceEnabled())
            {
                LOGGER.trace(
                    "wasEntityAlreadySaved(): No duplicate finder for entity type: " +
                    entity.getClass().getSimpleName());
            }
        }

        else
        {
            if (LOGGER.isTraceEnabled())
            {
                LOGGER.trace(
                    "wasEntityAlreadySaved(): Using duplicate finder - " + duplicateFinder.getClass().getName());
            }

            result = duplicateFinder.wasAlreadySaved(session, entity);
        }


        if (LOGGER.isTraceEnabled())
            LOGGER.trace("wasEntityAlreadySaved(): Result - " + result);

        return result;
    }
}