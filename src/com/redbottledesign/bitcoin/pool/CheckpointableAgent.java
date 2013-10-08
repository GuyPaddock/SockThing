package com.redbottledesign.bitcoin.pool;

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
