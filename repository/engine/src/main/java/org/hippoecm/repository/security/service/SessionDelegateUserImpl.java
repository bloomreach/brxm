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
import org.onehippo.repository.security.User;

public class SessionDelegateUserImpl implements SessionDelegateUser {

    private final String id;
    private final Set<String> memberships;
    private final Set<String> userIds;
    private final User delegateUser;

    public SessionDelegateUserImpl(final Set<String> ids, final Set<String> memberships, final User delegateUser) {
        if (ids == null) {
            throw new IllegalArgumentException("ids cannot be null");
        }
        if (ids.size() != 2) {
            throw new IllegalArgumentException("ids must and may only contain two elements");
        }
        if (memberships == null) {
            throw new IllegalArgumentException("memberships cannot be null");
        }
        if (delegateUser == null) {
            throw new IllegalArgumentException("delegateUser cannot be null");
        }
        this.id = String.join(",", ids);
        this.userIds = Collections.unmodifiableSet(new LinkedHashSet<>(ids));
        this.memberships = Collections.unmodifiableSet(new HashSet<>(memberships));
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
    public String getProperty(final String propertyName) {
        return null;
    }

    @Override
    public Set<String> getMemberships() {
        return memberships;
    }

    public Set<String> getIds() {
        return userIds;
    }

    public User getDelegateUser() {
        return delegateUser;
    }

    @Override
    public String toString() {
        return "SessionDelegateUser: " + getId();
    }
}
