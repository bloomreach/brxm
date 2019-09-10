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
package org.hippoecm.repository.security.principals;

import java.security.Principal;

import org.onehippo.repository.security.SessionUser;
import org.onehippo.repository.security.User;

/**
 * A principal wrapping a {@link SessionUser} with {@link #getName()} delegated to {@link User#getId()}.
 * <p>
 * A UserPrincipal compares equal to any other instance of UserPrincipal: only one instance can be contained
 * in a set.
 * </p>
 * <p>
 * The User can be retrieved through {@link #getUser()}.
 * </p>
 */
public final class UserPrincipal implements Principal {

    private final SessionUser user;

    public UserPrincipal(final SessionUser user) {
        if (user == null) {
            throw new IllegalArgumentException("user can not be null");
        }
        this.user = user;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return user.getId();
    }

    public SessionUser getUser() {
        return user;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return ("UserPrincipal: " + getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof UserPrincipal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return this.getClass().hashCode();
    }
}
