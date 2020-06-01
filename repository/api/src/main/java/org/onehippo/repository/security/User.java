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

import java.util.Calendar;
import java.util.Set;

import javax.jcr.RepositoryException;

/**
 * Represents a user in the repository.
 */
public interface User {

    /**
     * Get the id of the user.
     *
     * @return  the id of the user
     * @throws RepositoryException
     */
    String getId() throws RepositoryException;

    /**
     * Whether this user is marked as a system user.
     *
     * @return  whether this user is marked as a system user.
     * @throws RepositoryException
     */
    boolean isSystemUser() throws RepositoryException;

    /**
     * Whether this user is marked as active.
     *
     * @return  whether this user is marked as active.
     * @throws RepositoryException
     */
    boolean isActive() throws RepositoryException;

    /**
     * Get the {@link Group}s this user is a member of.
     *
     * @return the {@link Group}s this user is a member of.
     * @throws RepositoryException
     */
    Iterable<Group> getMemberships() throws RepositoryException;


    /**
     * Get the first name property of this user.
     *
     * @return  the first name property of this user or {@code null} if not present
     * @throws RepositoryException
     */
    String getFirstName() throws RepositoryException;

    /**
     * Get the last name property of this user.
     *
     * @return  the last name property of this user or {@code null} if not present
     * @throws RepositoryException
     */
    String getLastName() throws RepositoryException;


    /**
     * Get the email property of this user.
     *
     * @return  the email property of this user or {@code null} if not present
     * @throws RepositoryException
     */
    String getEmail() throws RepositoryException;

    /**
     * Get the last login property of this user.
     *
     * @return  the last login property of this user or {@code null} if not present
     * @throws RepositoryException
     */
    Calendar getLastLogin() throws RepositoryException;

    /**
     * Get an external user property by name.
     *
     * @return  the external property of the user identified by {@code propertyName},
     * or {@code null} if not present
     * @throws RepositoryException
     */
    String getProperty(String propertyName) throws RepositoryException;

}
