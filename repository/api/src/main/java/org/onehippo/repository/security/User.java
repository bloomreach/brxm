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

import java.io.Serializable;
import java.util.Calendar;
import java.util.Set;

import org.hippoecm.repository.api.HippoSession;

/**
 * Represents a user in the repository.
 */
public interface User extends Serializable {

    /**
     * Get the id of the user.
     *
     * @return  the id of the user
     */
    String getId();

    /**
     * Whether this user is marked as a system <em>User</em> (which is something totally different than a
     * {@link HippoSession#isSystemSession() JCR System Session} having all privileges everywhere).
     * <p>
     *     When {@link User#isSystemUser()} returns {@code true} it means that the user is required for the
     *     <em>running system</em>, for example the 'liveuser' in case of the delivery tier.
     *</p>
     * @return  whether this user is marked as user required by the system (not whether this user is a JCR System Session)
     */
    boolean isSystemUser();

    /**
     * Whether this user is marked as active.
     *
     * @return  whether this user is marked as active.
     */
    boolean isActive();

    /**
     * Whether this is an external user
     * @return whether this is an external user
     */
    boolean isExternal();

    /**
     * Get the immutable set of {@link Group#getId() Group identities} this user is a member of, which can be used to
     * lookup the referenced {@link Group} object(s) through {@link SecurityService#getGroup(String)}.
     *
     * @return the mutable set of {@link Group#getId() Group identities} this user is a member of.
     */
    Set<String> getMemberships();

    /**
     * Get the directly assigned user role names for this user.
     * <p>
     * The user role names are <em>not</em> resolved, nor include possible implied user roles names
     * </p>
     * @return the directly assigned user role names
     */
    Set<String> getUserRoles();

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
     * Get the names of the available additional user properties (with a value type String)
     * @see #getProperty(String)
     * @return the names of the available additional user properties (with a value type String)
     */
    Set<String> getPropertyNames();

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
