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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.bloomreach.xm.repository.security.Role;
import com.bloomreach.xm.repository.security.RolesProvider;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PRIVILEGES;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_ROLES;
import static org.hippoecm.repository.api.HippoNodeType.NT_ROLE;

/**
 * Concrete implementation for loading and referencing {@link Role}s in a fully thread-safe way, with asynchronous
 * background reloading triggered by a <em>synchronous</em> JCR event listener replacing the underlying model atomically.
 */
class RolesProviderImpl extends AbstractRolesProvider<Role> implements RolesProvider {

    RolesProviderImpl(final Session systemSession, final String rolesPath) throws RepositoryException {
        super(systemSession, rolesPath, NT_ROLE, HIPPO_ROLES, Role.class);
    }

    /**
     * Factory of {@link Role} instances.
     * @param node role node
     * @param name already JCR decoded role node name
     * @param description role description
     * @param system indicator if the role is a system role
     * @param roleNames all directly (not recursively) implied roles
     * @return a new {@link Role} instance
     * @throws RepositoryException if something went wrong
     */
    @Override
    protected Role createRole(final Node node, final String name, final String description, final boolean system,
                              final Set<String> roleNames)
            throws RepositoryException {
        Set<String> privileges = getValues(node, HIPPO_PRIVILEGES);
        return new RoleImpl(name, description, system, roleNames, privileges);
    }
}
