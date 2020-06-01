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
package org.hippoecm.frontend.plugins.cms.admin.domains;

import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.frontend.plugins.cms.admin.SecurityManagerHelper;

import com.bloomreach.xm.repository.security.AuthRole;
import com.bloomreach.xm.repository.security.AuthRoleBean;

/**
 * Convenient static helper methods for AuthRole crud operations
 */
public final class AuthRolesHelper {

    private AuthRolesHelper() {}

    public static AuthRole addAuthRole(final String name, final String domainPath, final String role)
            throws RepositoryException {
        return SecurityManagerHelper.getDomainsManager().addAuthRole(new AuthRoleBean(name, domainPath, role));
    }

    public static boolean deleteAuthRole(final AuthRole authRole) throws RepositoryException {
        return SecurityManagerHelper.getDomainsManager().deleteAuthRole(authRole);
    }

    public static AuthRole authRoleAddUser(final AuthRole authRole, final String user) throws RepositoryException {
        if (!authRole.getUsers().contains(user)) {
            final AuthRoleBean template = new AuthRoleBean(authRole);
            template.getUsers().add(user);
            return SecurityManagerHelper.getDomainsManager().updateAuthRole(template);
        }
        return authRole;
    }

    public static AuthRole authRoleRemoveUser(final AuthRole authRole, final String user) throws RepositoryException {
        if (authRole.getUsers().contains(user)) {
            final AuthRoleBean template = new AuthRoleBean(authRole);
            template.getUsers().remove(user);
            return SecurityManagerHelper.getDomainsManager().updateAuthRole(template);
        }
        return authRole;
    }

    public static AuthRole authRoleAddGroup(final AuthRole authRole, final String group) throws RepositoryException {
        if (!authRole.getGroups().contains(group)) {
            final AuthRoleBean template = new AuthRoleBean(authRole);
            template.getGroups().add(group);
            return SecurityManagerHelper.getDomainsManager().updateAuthRole(template);
        }
        return authRole;
    }

    public static AuthRole authRoleRemoveGroup(final AuthRole authRole, final String group) throws RepositoryException {
        if (authRole.getGroups().contains(group)) {
            final AuthRoleBean template = new AuthRoleBean(authRole);
            template.getGroups().remove(group);
            return SecurityManagerHelper.getDomainsManager().updateAuthRole(template);
        }
        return authRole;
    }

    public static AuthRole authRoleSetUserRole(final AuthRole authRole, final String userRole) throws RepositoryException {
        if (!StringUtils.equals(userRole, authRole.getUserRole())) {
            final AuthRoleBean template = new AuthRoleBean(authRole);
            template.setUserRole(userRole);
            return SecurityManagerHelper.getDomainsManager().updateAuthRole(template);
        }
        return authRole;
    }
}
