package com.github.fireduck64.sockthing;

import java.util.HashMap;

public interface PplnsAgent
extends Runnable
{
  public abstract HashMap<String, Double> getUserMap();
  public abstract void start();
}