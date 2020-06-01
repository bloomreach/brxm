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


import javax.jcr.RepositoryException;

/**
 * Represents a group of {@link User}s in the repository.
 */
public interface Group {

    /**
     * Get the id of the group.
     *
     * @return  the id of the group
     * @throws RepositoryException
     */
    String getId() throws RepositoryException;

    /**
     * Get the members of this group.
     *
     * @return  the {@link User}s that are members of this group.
     * @throws RepositoryException
     */
    Iterable<User> getMembers() throws RepositoryException;

    /**
     * Get the description property of this group.
     *
     * @return  the description property of this group, or {@code null} if not present
     * @throws RepositoryException
     */
    String getDescription() throws RepositoryException;

    /**
     * Whether this group is marked as a system group.
     *
     * @return  whether this group is a system group
     * @throws RepositoryException
     */
    boolean isSystemGroup() throws RepositoryException;

    /**
     * Get an external group property by name.
     *
     * @return  the external property of the group identified by {@code propertyName},
     * or {@code null} if not present
     * @throws RepositoryException
     */
    String getProperty(String propertyName) throws RepositoryException;

}
