package com.redbottledesign.bitcoin.pool.drupal.authentication;

import com.github.fireduck64.sockthing.PoolUser;
import com.redbottledesign.drupal.User;

public class DrupalPoolUser
extends PoolUser
{
  private final User drupalUser;

  public DrupalPoolUser(User drupalUser, String workerName, int difficulty)
  {
    super(workerName);

    this.setDifficulty(difficulty);

    this.drupalUser = drupalUser;
  }

  public User getDrupalUser()
  {
    return this.drupalUser;
  }
}
