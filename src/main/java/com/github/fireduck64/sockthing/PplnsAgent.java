package com.github.fireduck64.sockthing;

import com.redbottledesign.bitcoin.pool.drupal.node.SolvedBlock;

public interface PplnsAgent
extends Runnable
{
  public abstract void start();
  public abstract void payoutBlock(SolvedBlock payout);
}