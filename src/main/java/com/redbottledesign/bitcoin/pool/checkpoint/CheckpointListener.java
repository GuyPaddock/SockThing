package com.redbottledesign.bitcoin.pool.checkpoint;

public interface CheckpointListener
{
    public void onCheckpointItemCreated(Checkpointable checkpointable, CheckpointItem checkpoint);
    public void onCheckpointItemUpdated(Checkpointable checkpointable, CheckpointItem checkpoint);
    public void onCheckpointItemExpired(Checkpointable checkpointable, CheckpointItem checkpoint);
}
