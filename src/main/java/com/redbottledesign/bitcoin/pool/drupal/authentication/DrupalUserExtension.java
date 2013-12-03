package com.redbottledesign.bitcoin.pool.drupal.authentication;

import com.github.fireduck64.sockthing.PoolUserExtension;
import com.redbottledesign.drupal.User;

public class DrupalUserExtension
implements PoolUserExtension
{
    private final User drupalUser;

    public DrupalUserExtension(User drupalUser)
    {
        this.drupalUser = drupalUser;
    }

    public User getDrupalUser()
    {
        return this.drupalUser;
    }
}