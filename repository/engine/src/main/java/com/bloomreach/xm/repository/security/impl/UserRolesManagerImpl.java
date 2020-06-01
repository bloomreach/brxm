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

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bloomreach.xm.repository.security.UserRole;
import com.bloomreach.xm.repository.security.UserRolesManager;

class UserRolesManagerImpl extends AbstractRolesManager<UserRole> implements UserRolesManager {

    private static final Logger log = LoggerFactory.getLogger(UserRolesManager.class);

    UserRolesManagerImpl(final RepositorySecurityManagerImpl repositorySecurityManager) {
        super(repositorySecurityManager, (UserRolesProviderImpl)repositorySecurityManager.getUserRolesProvider(), log);
    }

    @Override
    public UserRole addUserRole(final UserRole userRoleTemplate)
            throws IllegalArgumentException, AccessDeniedException, RepositoryException {
        return addRole(userRoleTemplate);
    }

    @Override
    public UserRole updateUserRole(final UserRole userRoleTemplate)
            throws IllegalArgumentException, AccessDeniedException, RepositoryException {
        return updateRole(userRoleTemplate);
    }

    @Override
    public boolean deleteUserRole(final String userRoleName)
            throws IllegalArgumentException, AccessDeniedException, RepositoryException {
        return deleteRole(userRoleName);
    }
}
