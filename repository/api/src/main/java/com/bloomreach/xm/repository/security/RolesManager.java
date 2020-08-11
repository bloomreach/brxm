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
package com.bloomreach.xm.repository.security;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoSession;

/**
 * Provides administrative (crud) roles management.
 */
public interface RolesManager {

    /**
     * Add a new role.
     * @param roleTemplate A <em>template</em> for the role to create from, for example a {@link RoleBean} instance
     * @return the created role (retrieved from the {@link RolesProvider}
     * @throws IllegalArgumentException when the role is null or roleTemplate.name is null/blank
     * @throws AccessDeniedException if not allowed to create a role, or trying to create a system role which is only
     * allowed for system users: {@link HippoSession#isSystemSession}
     * @throws RepositoryException if the underlying HippoSession is no longer live, or something else went wrong
     */
    Role addRole(final Role roleTemplate)
            throws IllegalArgumentException, AccessDeniedException, RepositoryException;

    /**
     * Update a role.
     * @param roleTemplate A <em>template</em> for the role to update from, for example a {@link RoleBean} instance
     * @return the (possibly) updated role (retrieved from the {@link RolesProvider}
     * @throws IllegalArgumentException when the role is null or roleTemplate.name is null/blank
     * @throws AccessDeniedException if not allowed to set the system status, or change a system role, which is only
     * allowed for system users: {@link HippoSession#isSystemSession}
     * @throws RepositoryException if the underlying HippoSession is no longer live, or something else went wrong
     */
    Role updateRole(final Role roleTemplate)
            throws IllegalArgumentException, AccessDeniedException, RepositoryException;

    /**
     * Delete a role.
     * @param roleName The name of the role to delete
     * @return true if the role was deleted; false if the role didn't exist (anymore)
     * @throws IllegalArgumentException when the roleName is null/blank
     * @throws AccessDeniedException if not allowed to delete a system role which is only
     * allowed for system users: {@link HippoSession#isSystemSession}
     * @throws RepositoryException if the underlying HippoSession is no longer live, or something else went wrong
     */
    boolean deleteRole(final String roleName)
            throws IllegalArgumentException, AccessDeniedException, RepositoryException;
}
