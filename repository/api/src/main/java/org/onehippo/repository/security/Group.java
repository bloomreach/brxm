/*
 *  Copyright 2013-2019 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Set;

/**
 * Represents a group of {@link User}s in the repository.
 */
public interface Group {

    /**
     * Get the id of the group.
     *
     * @return  the id of the group
     */
    String getId();

    /**
     * Get the immutable set of {@link User#getId() User identifiers} who are members of this group. which can be used
     * to lookup the referenced {@link User} object(s) through {@link SecurityService#getUser(String)}.
     * <p>
     * Note: this set is lazy loaded, once.
     * </p>
     *
     * @return the {@link User#getId() User identifiers} who are members of this group
     */
    Set<String> getMembers();

    /**
     * Get the directly assigned user role names for this group.
     * <p>
     * The user role names are <em>not</em> resolved, nor include possible implied user roles names
     * </p>
     * @return the directly assigned user role names
     */
    Set<String> getUserRoles();

    /**
     * Get the description property of this group.
     *
     * @return  the description property of this group, or {@code null} if not present
     */
    String getDescription();

    /**
     * Whether this group is marked as a system group.
     *
     * @return  whether this group is a system group
     */
    boolean isSystemGroup();

    /**
     * Whether this is an external user
     * @return whether this is an external user
     */
    boolean isExternal();

    /**
     * Get an additional group property by name.
     * <p>
     * Only single properties of type String, Boolean, Date, Double or Long are returned, while internal properties are hidden.
     * </p>
     *
     * @return  the additional property of the group identified by {@code propertyName},
     * or {@code null} if not present/available
     */
    String getProperty(String propertyName);

}
