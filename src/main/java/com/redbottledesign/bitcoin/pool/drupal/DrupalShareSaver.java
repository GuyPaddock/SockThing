package com.redbottledesign.bitcoin.pool.drupal;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.Config;
import com.github.fireduck64.sockthing.PoolUser;
import com.github.fireduck64.sockthing.StratumServer;
import com.github.fireduck64.sockthing.SubmitResult;
import com.github.fireduck64.sockthing.sharesaver.ShareSaver;
import com.redbottledesign.bitcoin.pool.agent.RoundAgent;
import com.redbottledesign.bitcoin.pool.agent.persistence.PersistenceAgent;
import com.redbottledesign.bitcoin.pool.checkpoint.CheckpointGsonBuilder;
import com.redbottledesign.bitcoin.pool.drupal.DrupalShareSaver.BlockPersistenceCallback;
import com.redbottledesign.bitcoin.pool.drupal.authentication.DrupalPoolUser;
import com.redbottledesign.bitcoin.pool.drupal.node.SolvedBlock;
import com.redbottledesign.bitcoin.pool.drupal.node.WorkShare;
import com.redbottledesign.bitcoin.pool.util.queue.QueueItemCallback;
import com.redbottledesign.bitcoin.pool.util.queue.QueueItemCallbackFactory;
import com.redbottledesign.drupal.Node;
import com.redbottledesign.drupal.User;

public class DrupalShareSaver
implements ShareSaver, QueueItemCallbackFactory<BlockPersistenceCallback>
{
    private static final boolean DEBUG_EVERY_SHARE_AS_BLOCK = false;

    private static final String SHARE_STATUS_ACCEPTED = "accepted";
    private static final String CONFIRM_YES = "Y";
    private static final Node.Reference TEST_REMARK = new Node.Reference(11);
    private static final int SATOSHIS_PER_BITCOIN = 100000000;

    final StratumServer server;
    private final DrupalSession session;
    private final PersistenceAgent persistenceAgent;
    private final User poolDaemonUser;

    public DrupalShareSaver(Config config, StratumServer server)
    {
        this.server             = server;
        this.session            = server.getSession();
        this.persistenceAgent   = server.getAgent(PersistenceAgent.class);
        this.poolDaemonUser     = this.session.getPoolDaemonUser();

        CheckpointGsonBuilder.getInstance().registerPersistenceCallbackFactory(this);
    }

    @Override
    public void saveShare(PoolUser submitter, SubmitResult submitResult, String source, String uniqueJobString,
                          long blockReward, long feeTotal)
    {
        RoundAgent      roundAgent              = this.server.getAgent(RoundAgent.class);
        Node.Reference  currentRoundReference   = roundAgent.getCurrentRoundSynchronized().asReference();
        User.Reference  daemonUserReference     = this.poolDaemonUser.asReference();
        User.Reference  drupalSubmitter         = ((DrupalPoolUser) submitter).getDrupalUser().asReference();
        WorkShare       newShare;

        newShare =
            createNewShare(
                drupalSubmitter,
                submitResult,
                source,
                uniqueJobString,
                currentRoundReference,
                daemonUserReference);

        if (!DEBUG_EVERY_SHARE_AS_BLOCK &&
            (!CONFIRM_YES.equals(submitResult.getUpstreamResult()) || (submitResult.getHash() == null)))
        {
            newShare.setBlock(null);

            // Save the new share immediately
            this.persistenceAgent.queueForSave(newShare);
        }

        else
        {
            SolvedBlock newBlock =
                this.createNewBlock(
                    drupalSubmitter,
                    submitResult,
                    blockReward,
                    feeTotal,
                    currentRoundReference,
                    daemonUserReference);

            // Save the block first, then save the share in the persistence
            // callback
            this.persistenceAgent.queueForSave(newBlock, new BlockPersistenceCallback(this, newShare));
        }
    }

    protected PersistenceAgent getPersistenceAgent()
    {
        return this.persistenceAgent;
    }

    protected WorkShare createNewShare(User.Reference drupalSubmitter, SubmitResult submitResult, String source,
                                       String uniqueJobString, Node.Reference currentRoundReference,
                                       User.Reference daemonUserReference)
    {
        WorkShare   newShare        = new WorkShare();
        double      workDifficulty  = submitResult.getWorkDifficulty();
        String      statusString    = SHARE_STATUS_ACCEPTED;

        if (submitResult.getReason() != null)
        {
            statusString = submitResult.getReason();

            if (statusString.length() > 50)
                statusString = statusString.substring(0, 50);
        }

        newShare.setAuthor(daemonUserReference);
        newShare.setJobHash(uniqueJobString);
        newShare.setRound(currentRoundReference);
        newShare.setShareDifficulty(workDifficulty);
        newShare.setSubmitter(drupalSubmitter);
        newShare.setDateSubmitted(new Date());
        newShare.setClientSoftwareVersion(submitResult.getClientVersion());
        newShare.setPoolHost(source);
        newShare.setVerifiedByPool(CONFIRM_YES.equals(submitResult.getOurResult()));
        newShare.setVerifiedByNetwork(CONFIRM_YES.equals(submitResult.getUpstreamResult()));
        newShare.setStatus(statusString);

        return newShare;
    }

    protected SolvedBlock createNewBlock(User.Reference drupalSubmitter, SubmitResult submitResult, long blockReward,
                                         long feeTotal, Node.Reference currentRoundReference,
                                         User.Reference daemonUserReference)
    {
        SolvedBlock newBlock        = new SolvedBlock();
        double      blockDifficulty = submitResult.getNetworkDifficulty();
        BigDecimal  totalReward     = BigDecimal.valueOf(blockReward).add(BigDecimal.valueOf(feeTotal));

        newBlock.setAuthor(daemonUserReference);
        newBlock.setHash(submitResult.getHash().toString());
        newBlock.setHeight(submitResult.getHeight());
        newBlock.setStatus(SolvedBlock.Status.UNCONFIRMED);
        newBlock.setCreationTime(new Date());
        newBlock.setDifficulty(blockDifficulty);
        newBlock.setRound(currentRoundReference);

        // TODO: Separate fees out from reward later?
        newBlock.setReward(totalReward.divide(BigDecimal.valueOf(SATOSHIS_PER_BITCOIN)));

        newBlock.setSolvingMember(drupalSubmitter);
        newBlock.setWittyRemark(TEST_REMARK);

        return newBlock;
    }

    @Override
    public BlockPersistenceCallback createCallback(Type desiredType)
    {
        BlockPersistenceCallback result = null;

        if (BlockPersistenceCallback.class.equals(desiredType))
            result = new BlockPersistenceCallback(this, null);

        return result;
    }

    protected static class BlockPersistenceCallback
    implements QueueItemCallback<SolvedBlock>
    {
        private static final Logger LOGGER = LoggerFactory.getLogger(BlockPersistenceCallback.class);

        private transient DrupalShareSaver shareSaver;
        private WorkShare relatedNewShare;

        public BlockPersistenceCallback(DrupalShareSaver shareSaver, WorkShare relatedNewShare)
        {
            this.shareSaver = shareSaver;
            this.relatedNewShare = relatedNewShare;
        }

        public DrupalShareSaver getShareSaver()
        {
            return this.shareSaver;
        }

        public WorkShare getRelatedNewShare()
        {
            return this.relatedNewShare;
        }

        @Override
        public void onEntityProcessed(SolvedBlock newBlock)
        {
            if (LOGGER.isInfoEnabled())
                LOGGER.info(String.format("New block %d saved successfully.", newBlock.getHeight()));

            /* NOTE: We do NOT issue pay-outs here. We used to, and it was bad;
             * we were paying out for blocks that weren't confirmed, and a lot
             * of them were dupes, either ones someone else got to first, or
             * dupes within the pool.
             *
             * The block payouts are now initiated by the BlockConfirmationAgent.
             */

            if (LOGGER.isInfoEnabled())
            {
                LOGGER.info(
                    String.format(
                        "Saving work share related to block %d for user #%d.",
                        newBlock.getHeight(),
                        this.relatedNewShare.getSubmitter().getId()));
            }

            this.relatedNewShare.setBlock(newBlock.asReference());
            this.shareSaver.getPersistenceAgent().queueForSave(this.relatedNewShare);
        }

        @Override
        public void onEntityEvicted(SolvedBlock evictedBlock)
        {
            // FIXME: What happens to the share?
            if (LOGGER.isInfoEnabled())
            {
                LOGGER.info(
                    String.format(
                        "WARNING: Work share related to block %d for user #%d was EVICTED: %s",
                        evictedBlock.getHeight(),
                        this.relatedNewShare.getSubmitter().getId()),
                        this.relatedNewShare);
            }
        }

        protected void setShareSaver(DrupalShareSaver shareSaver)
        {
            this.shareSaver = shareSaver;
        }

        protected void setRelatedNewShare(WorkShare relatedNewShare)
        {
            this.relatedNewShare = relatedNewShare;
        }
    }
}