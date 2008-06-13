/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository.security.user;

import javax.transaction.NotSupportedException;

import org.hippoecm.repository.security.AAContext;

/**
 * Interface for interacting with a specific user from a backend
 */
public interface User {
    @SuppressWarnings("unused")
    final static String SVN_ID = "$Id$";

    /**
     * Initialize the user with the given context
     * @param context the context with params for the backend
     * @param UserId a String representing a unique user id
     * @throws UserException
     */
    public void init(AAContext context, String UserId) throws UserException;;

    /**
     * Get the unique user id
     * @return the user id string
     * @throws UserException
     */
    public String getUserID() throws UserException;

    /**
     * Check with the backend if the password matches
     * @param password the char array containing the password
     * @return true when password matches
     * @throws UserException
     */
    public boolean checkPassword(char[] password) throws UserException;

    /**
     * Try to set the password of the user
     * @param password the password
     * @throws NotSupportedException thrown when backend does not support setting the password
     * @throws UserException
     */
    public void setPassword(char[] password) throws NotSupportedException, UserException;

    /**
     * Check if the current user is active. If the backen doesn't support an active setting
     * it MUST return true
     * @return true if user is active
     * @throws UserException
     */
    public boolean isActive() throws UserException;

    /**
     * Try to set the user to active or inactive
     * @param active true if the user must be set to active
     * @throws NotSupportedException thrown when backend does not support setting the active flag
     * @throws UserException
     */
    public void setActive(boolean active) throws NotSupportedException, UserException;

    /**
     * Try to set a key value pair property on the user
     * @param key the key string
     * @param value the value string
     * @throws NotSupportedException thrown when the backend does not support setting the key
     * @throws UserException
     */
    public void setProperty(String key, String value) throws NotSupportedException, UserException;
}
