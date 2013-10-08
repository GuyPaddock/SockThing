package com.redbottledesign.bitcoin.pool.drupal;

import java.math.BigDecimal;
import java.util.Date;

import com.github.fireduck64.sockthing.Config;
import com.github.fireduck64.sockthing.PoolUser;
import com.github.fireduck64.sockthing.PplnsAgent;
import com.github.fireduck64.sockthing.StratumServer;
import com.github.fireduck64.sockthing.SubmitResult;
import com.github.fireduck64.sockthing.sharesaver.ShareSaver;
import com.redbottledesign.bitcoin.pool.PersistenceAgent;
import com.redbottledesign.bitcoin.pool.PersistenceCallback;
import com.redbottledesign.bitcoin.pool.drupal.node.SolvedBlock;
import com.redbottledesign.bitcoin.pool.drupal.node.WorkShare;
import com.redbottledesign.drupal.Node;
import com.redbottledesign.drupal.User;

public class DrupalShareSaver
implements ShareSaver
{
  private static final String SHARE_STATUS_ACCEPTED = "accepted";

  private static final String CONFIRM_YES = "Y";

  private static final User.Reference TEST_USER = new User.Reference(16);
  private static final Node.Reference TEST_REMARK = new Node.Reference(11);

  private static final int SATOSHIS_PER_BITCOIN = 100000000;

  private final StratumServer server;
  private final DrupalSession session;
  private final PersistenceAgent persistenceAgent;
  private final User poolDaemonUser;

  public DrupalShareSaver(Config config, StratumServer server)
  {
    this.server           = server;
    this.session          = server.getSession();
    this.persistenceAgent = server.getPersistenceAgent();
    this.poolDaemonUser   = this.session.getPoolDaemonUser();
  }

  @Override
  public void saveShare(PoolUser pu, SubmitResult submitResult, String source, String uniqueJobString, long blockReward,
                        long feeTotal)
  {
    RoundAgent      roundAgent            = this.server.getRoundAgent();
    Node.Reference  currentRoundReference = roundAgent.getCurrentRoundSynchronized().asReference();
    User.Reference  daemonUserReference   = this.poolDaemonUser.asReference();
    final WorkShare newShare;

    newShare = createNewShare(submitResult, source, uniqueJobString, currentRoundReference, daemonUserReference);

//    if (!CONFIRM_YES.equals(submitResult.getUpstreamResult()) || (submitResult.getHash() == null))
    if (false)
    {
      newShare.setBlock(null);

      // Save the new share immediately
      this.persistenceAgent.queueForSave(newShare);
    }

    else
    {
      SolvedBlock newBlock =
        this.createNewBlock(submitResult, blockReward, feeTotal, currentRoundReference, daemonUserReference);

      // Save the block first...
      this.persistenceAgent.queueForSave(newBlock, new PersistenceCallback<SolvedBlock>()
      {
        @Override
        public void onEntitySaved(SolvedBlock newBlock)
        {
          PplnsAgent pplnsAgent = DrupalShareSaver.this.server.getPplnsAgent();

          if (pplnsAgent != null)
            pplnsAgent.payoutBlock(newBlock);

          newShare.setBlock(newBlock.asReference());

          // ...then save the new share.
          DrupalShareSaver.this.persistenceAgent.queueForSave(newShare);
        }
      });
    }
  }

  protected WorkShare createNewShare(SubmitResult submitResult, String source, String uniqueJobString,
                                     Node.Reference currentRoundReference, User.Reference daemonUserReference)
  {
    WorkShare newShare        = new WorkShare();
    double    workDifficulty  = submitResult.getWorkDifficulty();
    String    statusString    = SHARE_STATUS_ACCEPTED;

    if (submitResult.getReason() != null)
    {
      statusString = submitResult.getReason();

      if (statusString.length() > 50)
        statusString = statusString.substring(0, 50);

      System.out.println("Reason: " + statusString);
    }

    newShare.setAuthor(daemonUserReference);
    newShare.setJobHash(uniqueJobString);
    newShare.setRound(currentRoundReference);
    newShare.setShareDifficulty(workDifficulty);
    newShare.setSubmitter(TEST_USER);
    newShare.setDateSubmitted(new Date());
    newShare.setClientSoftwareVersion(submitResult.getClientVersion());
    newShare.setPoolHost(source);
    newShare.setVerifiedByPool(CONFIRM_YES.equals(submitResult.getOurResult()));
    newShare.setVerifiedByNetwork(CONFIRM_YES.equals(submitResult.getUpstreamResult()));
    newShare.setStatus(statusString);

    return newShare;
  }

  protected SolvedBlock createNewBlock(SubmitResult submitResult, long blockReward, long feeTotal,
                                       Node.Reference currentRoundReference, User.Reference daemonUserReference)
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
    newBlock.setReward(totalReward.divide(BigDecimal.valueOf(SATOSHIS_PER_BITCOIN))); // TODO: Separate out fees later?
    newBlock.setSolvingMember(TEST_USER);
    newBlock.setWittyRemark(TEST_REMARK);
    return newBlock;
  }
}
