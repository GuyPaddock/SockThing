package com.redbottledesign.bitcoin.pool.agent;

import java.util.LinkedList;
import java.util.List;

import com.redbottledesign.bitcoin.pool.Agent;
import com.redbottledesign.bitcoin.pool.checkpoint.Checkpoint;
import com.redbottledesign.bitcoin.pool.checkpoint.CheckpointListener;
import com.redbottledesign.bitcoin.pool.checkpoint.Checkpointable;

public abstract class CheckpointableAgent
extends Agent
implements Checkpointable
{
    private List<CheckpointListener> checkpointListeners;

    public CheckpointableAgent()
    {
        super();

        this.checkpointListeners = new LinkedList<CheckpointListener>();
    }

    public CheckpointableAgent(long frequencyInMilliseconds)
    {
        super(frequencyInMilliseconds);

        this.checkpointListeners = new LinkedList<CheckpointListener>();
    }

    @Override
    public String getCheckpointableName()
    {
        return this.getClass().getName();
    }

    @Override
    public void registerCheckpointListener(CheckpointListener listener)
    {
        this.checkpointListeners.add(listener);
    }

    @Override
    public void unregisterCheckpointListener(CheckpointListener listener)
    {
        this.checkpointListeners.remove(listener);
    }

    protected List<CheckpointListener> getCheckpointListeners()
    {
        return this.checkpointListeners;
    }

    protected void setCheckpointListeners(List<CheckpointListener> checkpointListeners)
    {
        this.checkpointListeners = checkpointListeners;
    }

    protected void notifyCheckpointListenersOnItemCreated(Checkpoint checkpoint)
    {
        for (CheckpointListener listener : this.checkpointListeners)
        {
            listener.onCheckpointItemCreated(this, checkpoint);
        }
    }

    protected void notifyCheckpointListenersOnItemExpired(Checkpoint checkpoint)
    {
        for (CheckpointListener listener : this.checkpointListeners)
        {
            listener.onCheckpointItemExpired(this, checkpoint);
        }
    }
}