package com.redbottledesign.bitcoin.pool.agent;

import com.redbottledesign.bitcoin.pool.Agent;
import com.redbottledesign.bitcoin.pool.Checkpointable;

public abstract class CheckpointableAgent<C>
extends Agent
implements Checkpointable<C>
{
  public CheckpointableAgent()
  {
    super();
  }

  public CheckpointableAgent(long frequencyInMilliseconds)
  {
    super(frequencyInMilliseconds);
  }

  @Override
  public C captureCheckpointAndStopGracefully()
  {
    this.stopProcessing();

    return this.captureCheckpoint();
  }
}
