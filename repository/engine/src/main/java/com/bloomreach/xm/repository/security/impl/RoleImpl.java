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
package com.bloomreach.xm.repository.security.impl;

import java.util.Set;

import com.bloomreach.xm.repository.security.Role;

/**
 * Implementation of a {@link Role}
 */
class RoleImpl extends AbstractRoleImpl implements Role {

    private final Set<String> privileges;

    RoleImpl(final String name, final String description, final boolean system, final Set<String> roles,
             final Set<String> privileges) {
        super(name, description, system, roles);
        this.privileges = privileges;
    }

    @Override
    public Set<String> getPrivileges() {
        return privileges;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof RoleImpl && getName().equals(((RoleImpl)obj).getName());
    }
}
