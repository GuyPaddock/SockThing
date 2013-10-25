package com.redbottledesign.bitcoin.pool.checkpoint;

import java.lang.reflect.Type;
import java.util.Collection;

public interface Checkpointable
{
    public Type getCheckpointItemType();
    public String getCheckpointableName();

    public Collection<? extends CheckpointItem> captureCheckpoint();
    public void restoreFromCheckpoint(Collection<? extends CheckpointItem> checkpoint);

    public void registerCheckpointListener(CheckpointListener listener);
    public void unregisterCheckpointListener(CheckpointListener listener);
}
