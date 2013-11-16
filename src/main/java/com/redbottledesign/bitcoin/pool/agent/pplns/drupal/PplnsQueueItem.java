package com.redbottledesign.bitcoin.pool.agent.pplns.drupal;

import com.redbottledesign.bitcoin.pool.checkpoint.SimpleCheckpointItem;
import com.redbottledesign.bitcoin.pool.drupal.node.SolvedBlock;

class PplnsQueueItem
extends SimpleCheckpointItem<SolvedBlock>
{
    public PplnsQueueItem(SolvedBlock block)
    {
        super(block.getHash(), block);
    }
}