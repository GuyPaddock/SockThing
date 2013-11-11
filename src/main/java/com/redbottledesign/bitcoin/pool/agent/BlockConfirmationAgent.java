package com.redbottledesign.bitcoin.pool.agent;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.PplnsAgent;
import com.github.fireduck64.sockthing.StratumServer;
import com.redbottledesign.bitcoin.pool.agent.BlockConfirmationAgent.BlockConfirmationCallback;
import com.redbottledesign.bitcoin.pool.agent.persistence.PersistenceAgent;
import com.redbottledesign.bitcoin.pool.agent.persistence.PersistenceCallback;
import com.redbottledesign.bitcoin.pool.agent.persistence.PersistenceCallbackFactory;
import com.redbottledesign.bitcoin.pool.checkpoint.CheckpointGsonBuilder;
import com.redbottledesign.bitcoin.pool.drupal.DrupalSession;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.SolvedBlockRequestor;
import com.redbottledesign.bitcoin.pool.drupal.node.SolvedBlock;
import com.redbottledesign.bitcoin.pool.util.queue.QueueItem;
import com.redbottledesign.bitcoin.pool.util.queue.QueueItemSieve;
import com.redbottledesign.drupal.Entity;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;

public class BlockConfirmationAgent
extends Agent
implements PersistenceCallbackFactory<BlockConfirmationCallback>
{
    private static final int MAX_BLOCK_ORPHAN_DISTANCE = 10;
    private static final int MIN_REQUIRED_BLOCK_CONFIRMATIONS = 120;
    private static final long CONFIRMATION_FREQUENCY_MS = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockConfirmationAgent.class);

    private final StratumServer server;
    private final PersistenceAgent persistenceAgent;
    private final PplnsAgent pplnsAgent;

    public BlockConfirmationAgent(StratumServer server)
    {
        super(CONFIRMATION_FREQUENCY_MS);

        this.server             = server;
        this.persistenceAgent   = server.getAgent(PersistenceAgent.class);
        this.pplnsAgent         = server.getAgent(PplnsAgent.class);

        CheckpointGsonBuilder.getInstance().registerPersistenceCallbackFactory(this);
    }

    @Override
    public BlockConfirmationCallback createCallback(Type desiredType)
    {
        BlockConfirmationCallback result = null;

        if (BlockConfirmationCallback.class.equals(desiredType))
            result = new BlockConfirmationCallback();

        return result;
    }

    @Override
    protected void runPeriodicTask()
    throws IOException, JSONException, DrupalHttpException
    {
        DrupalSession           session             = this.server.getSession();
        SolvedBlockRequestor    blockRequestor      = session.getBlockRequestor();
        List<SolvedBlock>       unconfirmedBlocks;
        Set<String>             blocksPendingSave;
        long                    currentBlockHeight;

        if (LOGGER.isInfoEnabled())
            LOGGER.info("Checking for recently-confirmed blocks...");

        /* Check persistence queue status FIRST, then request the list of
         * unconfirmed blocks.
         *
         * This ensures that we err on the side of skipping blocks we may have
         * just modified instead of updating the blocks twice.
         */
        blocksPendingSave   = this.getBlockHashesWithPendingModifications();
        unconfirmedBlocks   = blockRequestor.getUnconfirmedBlocks();
        currentBlockHeight  = this.getCurrentBlockHeight();

        for (SolvedBlock block : unconfirmedBlocks)
        {
            final String blockHash = block.getHash();

            if (blocksPendingSave.contains(blockHash))
            {
                if (LOGGER.isInfoEnabled())
                {
                    LOGGER.info(
                        String.format(
                            "  Not checking the status of block '%s', since changes are already pending for this " +
                            "block.",
                            blockHash));
                }
            }

            else
            {
                try
                {
                    long confirmationCount   = this.server.getBlockConfirmationCount(blockHash);
                    long blockHeightDistance = (currentBlockHeight - block.getHeight());

                    if ((confirmationCount == 0) && (blockHeightDistance > MAX_BLOCK_ORPHAN_DISTANCE))
                    {
                        if (LOGGER.isInfoEnabled())
                        {
                            LOGGER.info(
                                String.format(
                                    "  Block '%s' has been orphaned (was %d blocks behind current block without any " +
                                    "confirmations).",
                                    blockHash,
                                    blockHeightDistance));
                        }

                        block.setStatus(SolvedBlock.Status.ORPHANED);

                        this.persistenceAgent.queueForSave(block);
                    }

                    else if (confirmationCount < MIN_REQUIRED_BLOCK_CONFIRMATIONS)
                    {
                        if (LOGGER.isDebugEnabled())
                        {
                            LOGGER.debug(
                                String.format(
                                    "  Block '%s' is not yet mature (only %d confirmations but need %d).",
                                    blockHash,
                                    confirmationCount,
                                    MIN_REQUIRED_BLOCK_CONFIRMATIONS));
                        }
                    }

                    else
                    {
                        if (LOGGER.isInfoEnabled())
                            LOGGER.info(String.format("  Block '%s' is now confirmed; updating status.", blockHash));

                        block.setStatus(SolvedBlock.Status.CONFIRMED);

                        this.persistenceAgent.queueForSave(block, new BlockConfirmationCallback());
                    }
                }

                catch (JSONException ex)
                {
                    if (LOGGER.isErrorEnabled())
                    {
                        LOGGER.error(
                            String.format(
                                "Unable to retrieve information on block '%s': %s\n%s",
                                blockHash,
                                ex.getMessage(),
                                ExceptionUtils.getStackTrace(ex)));
                    }
                }
            }
        }

        if (LOGGER.isInfoEnabled())
            LOGGER.info("Block confirmation check complete.");
    }

    protected Set<String> getBlockHashesWithPendingModifications()
    {
        Set<String>             pendingBlockHashes = new HashSet<>();
        Collection<SolvedBlock> pendingBlocks;

        pendingBlocks =
            this.persistenceAgent.getQueryableQueue().getItemsMatchingSieve(
                SolvedBlock.class,
                new QueueItemSieve()
                {
                    @Override
                    public boolean matches(QueueItem<? extends Entity<?>> queueItem)
                    {
                        Entity<?> entity = queueItem.getEntity();

                        return (SolvedBlock.class.isAssignableFrom(entity.getClass()));
                    }
                });

        // Grab block hashes for faster look-ups.
        for (SolvedBlock block : pendingBlocks)
        {
            pendingBlockHashes.add(block.getHash());
        }

        return pendingBlockHashes;
    }

    protected long getCurrentBlockHeight()
    throws IOException, JSONException
    {
        long        result;
        JSONObject  currentBlockTemplate = this.server.getCurrentBlockTemplate();

        result = currentBlockTemplate.getLong("height");

        if (result <= 0)
            throw new IllegalStateException("Unexpected block height: " + result);

        return result;
    }

    protected class BlockConfirmationCallback
    implements PersistenceCallback<SolvedBlock>
    {
        @Override
        public void onEntitySaved(SolvedBlock savedBlock)
        {
            final PplnsAgent pplnsAgent = BlockConfirmationAgent.this.pplnsAgent;

            if (pplnsAgent != null)
            {
                if (LOGGER.isInfoEnabled())
                {
                    LOGGER.info(
                        String.format(
                            "  Block '%s' status updated. Queuing-up PPLNS block credits.",
                            savedBlock.getHash()));
                }

                pplnsAgent.payoutForBlock(savedBlock);
            }

            else
            {
                if (LOGGER.isInfoEnabled())
                {
                    LOGGER.info(
                        String.format(
                            "  Block '%s' status updated, but there is no PPLNS agent configured.",
                            savedBlock.getHash()));
                }
            }
        }

        @Override
        public void onEntityEvicted(SolvedBlock evictedBlock)
        {
            if (LOGGER.isInfoEnabled())
            {
                LOGGER.info(
                    String.format(
                        "WARNING: Payouts for solved block '%s' were canceled because block update was EVICTED: %s",
                        evictedBlock.getHash(),
                        evictedBlock));
            }
        }
    }
}