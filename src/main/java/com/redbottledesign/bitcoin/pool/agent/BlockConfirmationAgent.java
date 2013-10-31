package com.redbottledesign.bitcoin.pool.agent;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.PplnsAgent;
import com.github.fireduck64.sockthing.StratumServer;
import com.redbottledesign.bitcoin.pool.Agent;
import com.redbottledesign.bitcoin.pool.PersistenceCallback;
import com.redbottledesign.bitcoin.pool.drupal.DrupalSession;
import com.redbottledesign.bitcoin.pool.drupal.gson.requestor.SolvedBlockRequestor;
import com.redbottledesign.bitcoin.pool.drupal.node.SolvedBlock;
import com.redbottledesign.drupal.gson.exception.DrupalHttpException;

public class BlockConfirmationAgent
extends Agent
{
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
    }

    @Override
    protected void runPeriodicTask()
    throws IOException, DrupalHttpException
    {
        DrupalSession           session             = this.server.getSession();
        SolvedBlockRequestor    blockRequestor      = session.getBlockRequestor();
        List<SolvedBlock>       unconfirmedBlocks   = blockRequestor.getUnconfirmedBlocks();

        if (LOGGER.isInfoEnabled())
            LOGGER.info("Checking for recently-confirmed blocks...");

        for (SolvedBlock block : unconfirmedBlocks)
        {
            final String blockHash = block.getHash();

            try
            {
                int confirmationCount = this.server.getBlockConfirmationCount(blockHash);

                if (confirmationCount >= MIN_REQUIRED_BLOCK_CONFIRMATIONS)
                {
                    if (LOGGER.isInfoEnabled())
                        LOGGER.info(String.format("  Block '%s' is now confirmed; updating status."));

                    block.setStatus(SolvedBlock.Status.CONFIRMED);
                    blockRequestor.update(block);

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

        if (LOGGER.isInfoEnabled())
            LOGGER.info("Block confirmation check complete.");
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
                    LOGGER.info(String.format("  Block '%s' status updated. Queuing-up PPLNS block credits."));

                pplnsAgent.queueBlockForPayout(savedBlock);
            }

            else
            {
                if (LOGGER.isInfoEnabled())
                    LOGGER.info(String.format("  Block '%s' status updated, but there is no PPLNS agent configured."));
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