package com.redbottledesign.bitcoin.pool.drupal.authentication;

import com.github.fireduck64.sockthing.PoolUser;
import com.redbottledesign.drupal.User;

public class DrupalPoolUser
extends PoolUser
{
    public DrupalPoolUser(User drupalUser, String workerName, int difficulty)
    {
        super(workerName);

        this.setName(drupalUser.getName());
        this.setDifficulty(difficulty);

        this.putExtension(new DrupalUserExtension(drupalUser));
    }

    public User getDrupalUser()
    {
        return this.getExtension(DrupalUserExtension.class).getDrupalUser();
    }
}
