/*
 *  Copyright 2012-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.admin.permissions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.hippoecm.frontend.plugins.cms.admin.SecurityManagerHelper;
import org.hippoecm.frontend.plugins.cms.admin.domains.DetachableDomain;
import org.hippoecm.frontend.plugins.cms.admin.groups.Group;
import org.hippoecm.frontend.plugins.cms.admin.users.User;

import com.bloomreach.xm.repository.security.AuthRole;
import com.bloomreach.xm.repository.security.DomainAuth;
import com.bloomreach.xm.repository.security.UserRole;

/**
 * Holds an AuthRole - Domain combination
 */
public class PermissionBean {
    
    private final DetachableDomain domain;
    private final AuthRole authRole;

    public PermissionBean(final DetachableDomain domain, final AuthRole authRole) {
        this.domain = domain;
        this.authRole = authRole;
    }

    public DetachableDomain getDomain() {
        return domain;
    }

    public AuthRole getAuthRole() {
        return authRole;
    }

    /**
     * Returns all permissions for a group.
     *
     * @param group the {@link Group} to return all permissions for
     * @return a {@link List} of {@link PermissionBean}s containing all permissions for this group
     */
    public static List<PermissionBean> forGroup(Group group) {
        List<PermissionBean> permissionBeans = new ArrayList<>();
        try {
            Set<DomainAuth> groupDomains = SecurityManagerHelper.getDomainsManager().getDomainAuthsForGroup(group.getGroupname());
            for (DomainAuth domain : groupDomains) {
                for (AuthRole authRole : domain.getAuthRolesMap().values()) {
                    if (authRole.getGroups().contains(group.getGroupname())) {
                        PermissionBean permissionBean = new PermissionBean(new DetachableDomain(domain), authRole);
                        permissionBeans.add(permissionBean);
                    }
                }
            }
            permissionBeans.sort(Comparator.comparing(o -> o.getAuthRole().getPath()));
        } catch (RepositoryException e) {
            throw new IllegalStateException("Repository error occured, cannot get PermissionBeans for group.", e);
        }
        return permissionBeans;
    }

    /**
     * Returns all permissions for a user.
     *
     * @param user the {@link User} to return all permissions for
     * @return a {@link List} of {@link PermissionBean}s containing all permissions for this user
     */
    public static List<PermissionBean> forUser(User user) {
        List<PermissionBean> permissionBeans = new ArrayList<>();
        try {
            Set<DomainAuth> groupDomains = SecurityManagerHelper.getDomainsManager().getDomainAuthsForUser(user.getUsername());
            for (DomainAuth domain : groupDomains) {
                for (AuthRole authRole : domain.getAuthRolesMap().values()) {
                    if (authRole.getUsers().contains(user.getUsername())) {
                        PermissionBean permissionBean = new PermissionBean(new DetachableDomain(domain), authRole);
                        permissionBeans.add(permissionBean);
                    }
                }
            }
            permissionBeans.sort(Comparator.comparing(o -> o.getAuthRole().getPath()));
        } catch (RepositoryException e) {
            throw new IllegalStateException("Repository error occured, cannot get PermissionBeans for user.", e);
        }
        return permissionBeans;
    }

    /**
     * Returns all permissions for a userRole.
     *
     * @param userRole the {@link UserRole} to return all permissions for
     * @return a {@link List} of {@link PermissionBean}s containing all permissions for this user
     */
    public static List<PermissionBean> forUserRole(UserRole userRole) {
        List<PermissionBean> permissionBeans = new ArrayList<>();
        try {
            Set<DomainAuth> groupDomains = SecurityManagerHelper.getDomainsManager().getDomainAuthsForUserRole(userRole.getName());
            for (DomainAuth domain : groupDomains) {
                for (AuthRole authRole : domain.getAuthRolesMap().values()) {
                    if (userRole.getName().equals(authRole.getUserRole())) {
                        PermissionBean permissionBean = new PermissionBean(new DetachableDomain(domain), authRole);
                        permissionBeans.add(permissionBean);
                    }
                }
            }
            permissionBeans.sort(Comparator.comparing(o -> o.getAuthRole().getPath()));
        } catch (RepositoryException e) {
            throw new IllegalStateException("Repository error occured, cannot get PermissionBeans for userrole.", e);
        }
        return permissionBeans;
    }
}
