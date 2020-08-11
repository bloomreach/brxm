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
package org.hippoecm.repository.security.service;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.onehippo.repository.security.SessionDelegateUser;
import org.onehippo.repository.security.SessionUser;

public class SessionDelegateUserImpl implements SessionDelegateUser {

    private final String id;
    private final Set<String> memberships;
    private final Set<String> userIds;
    private final Set<String> userRoles;
    private final SessionUser delegateUser;

    public SessionDelegateUserImpl(final SessionUser delegateUser, final SessionUser delegatedUser) {
        if (delegateUser == null) {
            throw new IllegalArgumentException("delegateUser cannot be null");
        }
        if (delegatedUser == null) {
            throw new IllegalArgumentException("delegatedUser cannot be null");
        }
        if (delegateUser instanceof SessionDelegateUser) {
            throw new IllegalArgumentException("delegateUser cannot be a SessionDelegateUser itself");
        }
        final LinkedHashSet<String> mutableIds = new LinkedHashSet<>();
        mutableIds.add(delegateUser.getId());
        if (delegatedUser instanceof SessionDelegateUser) {
            mutableIds.addAll(((SessionDelegateUser)delegatedUser).getIds());
        } else {
            mutableIds.add(delegatedUser.getId());
        }
        this.id = String.join(",", mutableIds);
        this.userIds = Collections.unmodifiableSet(mutableIds);
        final HashSet<String> mutableMemberships = new HashSet<>(delegateUser.getMemberships());
        mutableMemberships.addAll(delegatedUser.getMemberships());
        this.memberships = Collections.unmodifiableSet(mutableMemberships);
        final HashSet<String> mutableUserRoles = new HashSet<>(delegateUser.getUserRoles());
        mutableUserRoles.addAll(delegatedUser.getUserRoles());
        userRoles = Collections.unmodifiableSet(mutableUserRoles);
        this.delegateUser = delegateUser;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isSystemUser() {
        return false;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public boolean isExternal() {
        return false;
    }

    @Override
    public String getFirstName() {
        return null;
    }

    @Override
    public String getLastName() {
        return null;
    }

    @Override
    public String getEmail() {
        return null;
    }

    @Override
    public Calendar getLastLogin() {
        return null;
    }

    @Override
    public Set<String> getPropertyNames() {
        return Collections.emptySet();
    }

    @Override
    public String getProperty(final String propertyName) {
        return null;
    }

    @Override
    public Set<String> getMemberships() {
        return memberships;
    }

    @Override
    public Set<String> getUserRoles() {
        return userRoles;
    }

    public Set<String> getIds() {
        return userIds;
    }

    @Override
    public SessionUser getDelegateUser() {
        return delegateUser;
    }

    @Override
    public String toString() {
        return "SessionDelegateUser: " + getId();
    }
}
