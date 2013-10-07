package com.github.fireduck64.sockthing;

import java.util.concurrent.BlockingQueue;

import com.redbottledesign.bitcoin.pool.drupal.node.SolvedBlock;

public interface PplnsAgent
extends Runnable
{
  public abstract BlockingQueue<SolvedBlock> getPendingBlockQueue();
  public abstract void start();
}