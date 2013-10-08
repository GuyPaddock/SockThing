package com.redbottledesign.bitcoin.pool;

public interface Checkpointable<C>
extends Stoppable
{
  public C captureCheckpoint();
  public C captureCheckpointAndStopGracefully();
  public void restoreFromCheckpoint(C checkpoint);
}
