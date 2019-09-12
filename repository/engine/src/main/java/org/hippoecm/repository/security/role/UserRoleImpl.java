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
package org.hippoecm.repository.security.role;

import java.util.Set;

/**
 * Implementation of a {@link UserRole}
 */
class UserRoleImpl implements UserRole {
    private final String name;
    private final boolean system;
    private final Set<String> roles;

    UserRoleImpl(final String name, final boolean system, final Set<String> roles) {
        this.name = name;
        this.system = system;
        this.roles = roles;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isSystem() {
        return system;
    }

    @Override
    public Set<String> getRoles() {
        return roles;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof UserRoleImpl && name.equals(((UserRoleImpl)obj).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
