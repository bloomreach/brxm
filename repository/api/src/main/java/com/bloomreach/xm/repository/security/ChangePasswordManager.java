/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.bloomreach.xm.repository.security;

import java.util.Calendar;

import javax.jcr.RepositoryException;

/**
 * The ChangePasswordManager allows a {link HippoSession} user to change its password
 */
public interface ChangePasswordManager {

    /**
     * Number of milliseconds in one day
     */
    long ONEDAYMS = 1000 * 3600 * 24L;

    /**
     * Provides the max age for passwords in days. When not configured (default) this returns -1L (no max age).
     * @return the max age for passwords in days
     * @throws RepositoryException if the underlying HippoSession is no longer live, or something else went wrong
     */
    long getPasswordMaxAgeDays() throws RepositoryException;

    /**
     * Provides the max age for passwords in milliseconds as convenience for calculating an expiration timestamp.
     * When not configured (default) this returns -1L (no max age).
     * @return the max age for passwords in milliseconds
     * @throws RepositoryException if the underlying HippoSession is no longer live, or something else went wrong
     */
    long getPasswordMaxAgeMs() throws RepositoryException;

    /**
     * Returns the current password last modified date, or null if never changed
     * @return the current password last modified date, or null if never changed
     * @throws RepositoryException if the underlying HippoSession is no longer live, or something else went wrong
     */
    Calendar getPasswordLastModified() throws RepositoryException;

    /**
     * Check the current user password
     * @return true if the password is equal, false otherwise
     * @throws RepositoryException if the underlying HippoSession is no longer live, or something else went wrong
     */
    boolean checkPassword(final char[] password) throws RepositoryException;

    /**
     * Check if a new user password has been used before
     * @param newPassword the new candidate password, must be non-empty and at least 4 characters long
     * @param numberOfPreviousPasswordsToCheck the number of last used passwords which are not allowed to be reused
     *                                         (use value 0 to disable/ignore checking previous passwords)
     * @return true when the password is equal to the current password or has been used at least numberOfPreviousPasswordsToCheck before
     * @throws IllegalArgumentException For an empty/null newPassword
     * @throws RepositoryException if the underlying HippoSession is no longer live, or something else went wrong
     */
    boolean checkNewPasswordUsedBefore(final char[] newPassword, final int numberOfPreviousPasswordsToCheck)
            throws IllegalArgumentException, RepositoryException;

    /**
     * Update/set the current user password
     * @param currentPassword the current password, must match otherwise IllegalStateException will be thrown.
     *                        Note: for a first time password this parameter will be ignored!
     * @param newPassword the new password, must be non-empty and at least 4 characters long
     * @throws IllegalArgumentException When the currentPassword doesn't match, for an empty/null newPassword,
     * newPassword length < 4, or when newPassword is equal to the currentPassword
     * @throws RepositoryException if the underlying HippoSession is no longer live, or something else went wrong
     */
    void setPassword(final char[] currentPassword, final char[] newPassword)
            throws IllegalArgumentException, RepositoryException;
}
