package com.redbottledesign.bitcoin.pool.agent;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redbottledesign.bitcoin.pool.checkpoint.CheckpointItem;
import com.redbottledesign.bitcoin.pool.checkpoint.CheckpointListener;
import com.redbottledesign.bitcoin.pool.checkpoint.Checkpointable;

public abstract class CheckpointableAgent
extends Agent
implements Checkpointable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckpointableAgent.class);

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

    protected void notifyCheckpointListenersOnItemCreated(CheckpointItem checkpoint)
    {
        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace(
                String.format(
                    "%s.notifyCheckpointListenersOnItemCreated(%s)",
                    this.getClass().getSimpleName(),
                    checkpoint));
        }

        for (CheckpointListener listener : this.checkpointListeners)
        {
            listener.onCheckpointItemCreated(this, checkpoint);
        }
    }

    protected void notifyCheckpointListenersOnItemExpired(CheckpointItem checkpoint)
    {
        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace(
                String.format(
                    "%s.notifyCheckpointListenersOnItemExpired(%s)",
                    this.getClass().getSimpleName(),
                    checkpoint));
        }

        for (CheckpointListener listener : this.checkpointListeners)
        {
            listener.onCheckpointItemExpired(this, checkpoint);
        }
    }
}