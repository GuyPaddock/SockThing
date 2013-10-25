package com.redbottledesign.bitcoin.pool.checkpoint;

public interface Checkpointer
{
    public abstract void setupCheckpointing(Checkpointable... checkpointables);
    public abstract void restoreCheckpointsFromDisk();
}