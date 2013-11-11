package com.redbottledesign.bitcoin.pool.agent.persistence;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.redbottledesign.bitcoin.pool.drupal.DrupalSession;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.BlockCreditRequestor;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.PayoutRequestor;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.SolvedBlockRequestor;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.WorkShareRequestor;
import com.redbottledesign.bitcoin.pool.drupal.node.BlockCredit;
import com.redbottledesign.bitcoin.pool.drupal.node.Payout;
import com.redbottledesign.bitcoin.pool.drupal.node.SolvedBlock;
import com.redbottledesign.bitcoin.pool.drupal.node.WorkShare;
import com.redbottledesign.drupal.Entity;
import com.redbottledesign.drupal.Node;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;

@SuppressWarnings("serial")
public class DuplicateChecker
{
    protected static Map<Class<? extends Entity<?>>, DuplicateFinder<?>> DISPATCHER_REGISTRY =
        new HashMap<Class<? extends Entity<?>>, DuplicateFinder<?>>()
        {{
            put(BlockCredit.class,  new DuplicateBlockCreditFinder());
            put(Payout.class,       new DuplicatePayoutFinder());
//            put(Round.class,        new DuplicateRoundFinder());
            put(SolvedBlock.class,  new DuplicateSolvedBlockFinder());
            put(WorkShare.class,    new DuplicateWorkShareFinder());
        }};

    @SuppressWarnings("unchecked")
    public static <T extends Entity<?>>boolean wasEntityAlreadySaved(DrupalSession session, T entity)
    throws IOException, DrupalHttpException
    {
        boolean             result          = false;
        DuplicateFinder<T>  duplicateFinder = (DuplicateFinder<T>)DISPATCHER_REGISTRY.get(entity.getClass());

        if (duplicateFinder != null)
            result = duplicateFinder.wasAlreadySaved(session, entity);

        return result;
    }

    protected static interface DuplicateFinder<T extends Entity<?>>
    {
        public boolean wasAlreadySaved(DrupalSession session, T entity)
        throws IOException, DrupalHttpException;
    }

    protected static class DuplicateBlockCreditFinder
    implements DuplicateFinder<BlockCredit>
    {
        @Override
        public boolean wasAlreadySaved(DrupalSession session, final BlockCredit entity)
        throws IOException, DrupalHttpException
        {
            BlockCreditRequestor    requestor       = session.getCreditRequestor();
            BlockCredit             existingEntity;

            existingEntity =
                requestor.requestEntityByCriteria(
                    BlockCredit.ENTITY_TYPE,
                    new HashMap<String, Object>()
                    {{
                        put(Node.DRUPAL_BUNDLE_TYPE_FIELD_NAME, BlockCredit.CONTENT_TYPE);
                        put(BlockCredit.DRUPAL_FIELD_BLOCK,     entity.getBlock().getId());
                        put(BlockCredit.DRUPAL_FIELD_RECIPIENT, entity.getRecipient().getId());
                    }});

            return (existingEntity == null);
        }
    }

    protected static class DuplicatePayoutFinder
    implements DuplicateFinder<Payout>
    {
        @Override
        public boolean wasAlreadySaved(DrupalSession session, final Payout entity)
        throws IOException, DrupalHttpException
        {
            PayoutRequestor requestor       = session.getPayoutRequestor();
            Payout          existingEntity;

            existingEntity =
                requestor.requestEntityByCriteria(
                    Payout.ENTITY_TYPE,
                    new HashMap<String, Object>()
                    {{
                        put(Node.DRUPAL_BUNDLE_TYPE_FIELD_NAME, Payout.CONTENT_TYPE);
                        put(Payout.DRUPAL_FIELD_PAYMENT_HASH,   entity.getPaymentHash());
                    }});

            return (existingEntity == null);
        }
    }

    protected static class DuplicateSolvedBlockFinder
    implements DuplicateFinder<SolvedBlock>
    {
        @Override
        public boolean wasAlreadySaved(DrupalSession session, final SolvedBlock entity)
        throws IOException, DrupalHttpException
        {
            SolvedBlockRequestor    requestor       = session.getBlockRequestor();
            SolvedBlock             existingEntity;

            existingEntity =
                requestor.requestEntityByCriteria(
                    SolvedBlock.ENTITY_TYPE,
                    new HashMap<String, Object>()
                    {{
                        put(Node.DRUPAL_BUNDLE_TYPE_FIELD_NAME, SolvedBlock.CONTENT_TYPE);
                        put(SolvedBlock.DRUPAL_FIELD_HASH,      entity.getHash());
                    }});

            return (existingEntity == null);
        }
    }

    protected static class DuplicateWorkShareFinder
    implements DuplicateFinder<WorkShare>
    {
        @Override
        public boolean wasAlreadySaved(DrupalSession session, final WorkShare entity)
        throws IOException, DrupalHttpException
        {
            WorkShareRequestor  requestor       = session.getShareRequestor();
            WorkShare           existingEntity;

            existingEntity =
                requestor.requestEntityByCriteria(
                    WorkShare.ENTITY_TYPE,
                    new HashMap<String, Object>()
                    {{
                        put(Node.DRUPAL_BUNDLE_TYPE_FIELD_NAME, WorkShare.CONTENT_TYPE);
                        put(WorkShare.DRUPAL_FIELD_JOB_HASH,    entity.getJobHash());
                    }});

            return (existingEntity == null);
        }
    }
}
