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

import java.util.Calendar;
import java.util.Set;

/**
 * Represents a user in the repository.
 */
public interface User {

    /**
     * Get the id of the user.
     *
     * @return  the id of the user
     */
    String getId();

    /**
     * Whether this user is marked as a system user.
     *
     * @return  whether this user is marked as a system user.
     */
    boolean isSystemUser();

    /**
     * Whether this user is marked as active.
     *
     * @return  whether this user is marked as active.
     */
    boolean isActive();

    /**
     * Get the immutable set of {@link Group#getId() Group identities} this user is a member of, which can be used to
     * lookup the referenced {@link Group} object(s) through {@link SecurityService#getGroup(String)}.
     *
     * @return the mutable set of {@link Group#getId() Group identities} this user is a member of.
     */
    Set<String> getMemberships();


    /**
     * Get the first name property of this user.
     *
     * @return  the first name property of this user or {@code null} if not present
     */
    String getFirstName();

    /**
     * Get the last name property of this user.
     *
     * @return  the last name property of this user or {@code null} if not present
     */
    String getLastName();

    /**
     * Get the email property of this user.
     *
     * @return  the email property of this user or {@code null} if not present
     */
    String getEmail();

    /**
     * Get the last login property of this user.
     *
     * @return  the last login property of this user or {@code null} if not present
     */
    Calendar getLastLogin();

    /**
     * Get an additional user property by name.
     * <p>
     * Only single properties of type String, Boolean, Date, Double or Long are returned, while internal properties are hidden.
     * </p>
     *
     * @return  the additional property of the user identified by {@code propertyName},
     * or {@code null} if not present/available
     */
    String getProperty(String propertyName);

}
