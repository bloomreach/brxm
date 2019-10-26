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
 * Provides administrative (crud) userroles management.
 */
public interface UserRolesManager {

    /**
     * Add a new userrole.
     * @param userRoleTemplate A <em>template</em> for the userrole to create from, for example a {@link UserRoleBean} instance
     * @return the created userrole (retrieved from the {@link UserRolesProvider}
     * @throws IllegalArgumentException when the userrole is null or userRoleTemplate.name is null/blank
     * @throws AccessDeniedException if not allowed to create a userrole, or trying to create a system userrole which is
     * only allowed for system users: {@link HippoSession#isSystemSession}
     * @throws RepositoryException if the underlying HippoSession is no longer live, or something else went wrong
     */
    UserRole addUserRole(final UserRole userRoleTemplate)
            throws IllegalArgumentException, AccessDeniedException, RepositoryException;

    /**
     * Update a userrole.
     * @param userRoleTemplate A <em>template</em> for the userrole to update from, for example a {@link UserRoleBean} instance
     * @return the (possibly) updated userrole (retrieved from the {@link UserRolesProvider}
     * @throws IllegalArgumentException when the userrole is null or userRoleTemplate.name is null/blank
     * @throws AccessDeniedException if not allowed to to set the system status, or change a system userrole which is only
     * allowed for system users: {@link HippoSession#isSystemSession}
     * @throws RepositoryException if the underlying HippoSession is no longer live, or something else went wrong
     */
    UserRole updateUserRole(final UserRole userRoleTemplate)
            throws IllegalArgumentException, AccessDeniedException, RepositoryException;

    /**
     * Delete a userrole.
     * @param userRoleName The name of the userrole to delete
     * @return true if the userrole was deleted; false if the userrole didn't exist (anymore)
     * @throws IllegalArgumentException when the userRoleName is null/blank
     * @throws AccessDeniedException if not allowed to delete a system userrole which is only
     * allowed for system users: {@link HippoSession#isSystemSession}
     * @throws RepositoryException if the underlying HippoSession is no longer live, or something else went wrong
     */
    boolean deleteUserRole(final String userRoleName)
            throws IllegalArgumentException, AccessDeniedException, RepositoryException;
}
