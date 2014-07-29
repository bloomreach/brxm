/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.api.security.user.UserManager;

/**
 * Interface for managing users in the backend
 */
public interface HippoUserManager extends UserManager {

    /**
     * Check if the user exists
     * @param userId
     * @return true if the group exists
     * @throws RepositoryException
     */
    boolean hasUser(String userId) throws RepositoryException;

    /**
     * Get the node for the user with the given userId
     * @param userId
     * @return the user node
     * @throws RepositoryException
     */
    Node getUser(String userId) throws RepositoryException;

    /**
     * Create a (skeleton) node for the user in the repository
     * @param userId
     * @return the newly created user node
     * @throws RepositoryException
     */
    Node createUser(String userId) throws RepositoryException;

    /**
     * Get all the users, regardless of their provider.
     *
     * @return  an iterator of user nodes
     * @throws RepositoryException
     */
    NodeIterator listUsers(long offset, long limit) throws RepositoryException;

    /**
     * Get all the users managed by a particular provider.
     *
     * @return an iterator of group nodes
     * @throws RepositoryException
     */
    NodeIterator listUsers(String providerId, long offset, long limit) throws RepositoryException;

    /**
     * Authenticate the user by the credentials
     * @param creds SimpleCredentials
     * @return true when successfully authenticate
     * @throws RepositoryException
     */
    boolean authenticate(SimpleCredentials creds) throws RepositoryException;

    /**
     * Check if the user is active
     * @param userId
     * @return true if the user is active
     * @throws RepositoryException
     */
    boolean isActive(final String userId) throws RepositoryException;

    /**
     * Check if the password of the user has been expired
     * @param userId
     * @return true if the password of the user has been expired
     * @throws RepositoryException
     */
    boolean isPasswordExpired(final String userId) throws RepositoryException;

    /**
     * Synchronizes user info from the security store backend with the repository if applicable.
     * This method is supposed to be called after authentication especially when the user
     * is synchronized from external security data store.
     * @param userId
     */
    void syncUserInfo(String userId);

    /**
     * Updates the last login datetime. This method is supposed to be called after authentication
     * especially when the user is synchronized from external security data store.
     * @param userId
     */
    void updateLastLogin(String userId);

    /**
     * Saves the user node data. This method is supposed to be called after authentication
     * especially when the user node is sychronized from external security data store.
     * @throws RepositoryException
     */
    void saveUsers() throws RepositoryException;

    /**
     * Checks if the username should be treated in a case-sensitive way.
     * @return true if the username should be treated in a case-sensitive way.
     */
    boolean isCaseSensitive();

}
