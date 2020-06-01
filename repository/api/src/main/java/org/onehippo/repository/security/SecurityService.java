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
package org.onehippo.repository.security;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

/**
 * A service for obtaining security related information from the repository.
 */
public interface SecurityService {

    /**
     * Check if the user with the given {@code userId} exists.
     * @param userId  the id of the user to check
     * @return  whether the user with the given {@code userId} exists
     * @throws RepositoryException
     */
    boolean hasUser(final String userId) throws RepositoryException;

    /**
     * Get the {@link User} object identified by the given {@code userId}.
     *
     * @param userId  the id of the user to obtain.
     * @return  the {@link User} identified by the given {@code userId}
     * @throws ItemNotFoundException if no user with the given id could be found
     * @throws RepositoryException
     */
    User getUser(final String userId) throws ItemNotFoundException, RepositoryException;

    /**
     * Get all the {@link User}s in the repository.
     *
     * @return the list of {@link User}s in the repository. Never {@code null}.
     * @param offset  the start offset of the result; only has effect when value is larger than zero;
     *                defaults to no offset
     * @param limit  maximum size of the result; only has effect when value is larger than zero; defaults to no limit
     * @throws RepositoryException
     */
    Iterable<User> getUsers(long offset, long limit) throws RepositoryException;

    /**
     * Get all the {@link Group}s in the repository.
     *
     * @return the list of {@link Group}s in the repository. Never {@code null}.
     * @param offset  the start offset of the result; only has effect when value is larger than zero;
     *                defaults to no offset
     * @param limit  maximum size of the result; only has effect when value is larger than zero; defaults to no limit
     * @throws RepositoryException
     */
    Iterable<Group> getGroups(long offset, long limit) throws RepositoryException;

    /**
     * Check if the group with given {@code groupId} exists.
     * @param groupId  the id of the group to check
     * @return  whether the group with the given {@code groupId} exists
     * @throws RepositoryException
     */
    boolean hasGroup(final String groupId) throws RepositoryException;

    /**
     * Get the (@link Group} object identified by the given {@code groupId}.
     *
     * @param groupId  the id of the group to obtain.
     * @return  the {@link Group} identified by the given {@code groupId}
     * @throws ItemNotFoundException  if no group with the given id could be found
     * @throws RepositoryException
     */
    Group getGroup(final String groupId) throws ItemNotFoundException, RepositoryException;

}
