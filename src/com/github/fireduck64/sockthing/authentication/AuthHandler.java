package com.github.fireduck64.sockthing.authentication;

import com.github.fireduck64.sockthing.PoolUser;

/**
 * Interface for authentication handlers.
 *
 * @author Fireduck (fireduck@gmail.com)
 */
public interface AuthHandler
{
    /**
     * Attempts to authenticates the user with the provided username and
     * password, and, upon success, returns an appropriate {@link PoolUser}
     * object for the user.
     *
     * @param   username
     *          The user name.
     *
     * @param   password
     *          The password.
     *
     * @return  Either the {@link PoolUser} corresponding to the user's
     *          account, or {@code null} if the user is unknown / not
     *          allowed / username or password are incorrect.
     */
    public PoolUser authenticate(String username, String password);
}
