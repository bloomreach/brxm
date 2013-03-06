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
     * Get all the groups managed by a particular provider.
     *
     * @return an iterator of group nodes
     * @throws RepositoryException
     */
    NodeIterator listUsers(String providerId, long offset, long limit) throws RepositoryException;
}
