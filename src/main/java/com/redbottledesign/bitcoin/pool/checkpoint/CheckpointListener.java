package com.redbottledesign.bitcoin.pool.checkpoint;

public interface CheckpointListener
{
    public void onCheckpointItemCreated(Checkpointable checkpointable, Checkpoint checkpoint);
    public void onCheckpointItemExpired(Checkpointable checkpointable, Checkpoint checkpoint);
}
