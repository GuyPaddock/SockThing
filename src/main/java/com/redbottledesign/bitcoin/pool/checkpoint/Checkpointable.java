package com.redbottledesign.bitcoin.pool.checkpoint;

import java.lang.reflect.Type;
import java.util.Collection;

public interface Checkpointable
{
    public Type getCheckpointType();
    public String getCheckpointableName();

    public Collection<? extends Checkpoint> captureCheckpoints();
    public void restoreFromCheckpoints(Collection<? extends Checkpoint> checkpoint);

    public void registerCheckpointListener(CheckpointListener listener);
    public void unregisterCheckpointListener(CheckpointListener listener);
}
