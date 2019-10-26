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

import java.util.Objects;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;

import com.bloomreach.xm.repository.security.AbstractRole;

import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_DESCRIPTION;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_SYSTEM;

public class AbstractRolesManager<R extends AbstractRole> {

    protected final RepositorySecurityManagerImpl repositorySecurityManager;
    protected final AbstractRolesProvider<R> rolesProvider;
    protected final String roleTypeClassName;
    protected final Logger log;

    protected AbstractRolesManager(final RepositorySecurityManagerImpl repositorySecurityManager,
                                   final AbstractRolesProvider<R> rolesProvider, final Logger log) {
        this.repositorySecurityManager = repositorySecurityManager;
        this.rolesProvider = rolesProvider;
        this.roleTypeClassName = rolesProvider.getRoleType().getSimpleName();
        this.log = log;
    }

    private Node getRolesNodeFromSystemSession() throws RepositoryException {
        return repositorySecurityManager.getSystemSession().getNode(rolesProvider.getRolesPath());
    }

    protected void silentRefreshSystemSession() {
        try {
            repositorySecurityManager.getSystemSession().refresh(false);
        } catch (RepositoryException ignore) {
        }
    }

    public R addRole(final R roleTemplate)
            throws IllegalArgumentException, AccessDeniedException, RepositoryException {
        repositorySecurityManager.checkClosed();
        if (roleTemplate == null || StringUtils.isBlank(roleTemplate.getName())) {
            throw new IllegalArgumentException(roleTypeClassName + " cannot be null or without name");
        }
        final String name = roleTemplate.getName().trim();
        if (rolesProvider.hasRole(name)) {
            throw new ItemExistsException(roleTypeClassName + " '"+name+"' already exists");
        }
        if (roleTemplate.isSystem() && !repositorySecurityManager.getHippoSession().isSystemSession()) {
            log.error("Unauthorized attempt to create a system {} '{}'", roleTypeClassName.toLowerCase(), name);
            throw new AccessDeniedException("Not allowed to create a system " + roleTypeClassName.toLowerCase());
        }
        try {
            final Node newRoleNodeFromSystemSession =
                    getRolesNodeFromSystemSession().addNode(NodeNameCodec.encode(name), rolesProvider.getRolesTypeName());
            updateRoleNode(roleTemplate, newRoleNodeFromSystemSession);
            newRoleNodeFromSystemSession.getSession().save();
            log.info("{} '{}'{} added by user {}",
                    roleTypeClassName, name, roleTemplate.isSystem() ? " (system)" : "",
                    repositorySecurityManager.getHippoSession().getUserID());
            return rolesProvider.getRole(name);
        } catch (RepositoryException e) {
            silentRefreshSystemSession();
            throw e;
        }
    }

    public R updateRole(final R roleTemplate)
            throws IllegalArgumentException, AccessDeniedException, RepositoryException {
        repositorySecurityManager.checkClosed();
        if (roleTemplate == null || StringUtils.isBlank(roleTemplate.getName())) {
            throw new IllegalArgumentException(roleTypeClassName + " cannot be null or without name");
        }
        final String name = roleTemplate.getName().trim();
        final R currentRole = rolesProvider.getRole(name);
        if (currentRole == null) {
            throw new ItemNotFoundException(roleTypeClassName + " '"+roleTemplate+"' no longer exists");
        }
        if (currentRole.isSystem() && !repositorySecurityManager.getHippoSession().isSystemSession()) {
            log.error("Unauthorized attempt to change system {} '{}'", roleTypeClassName.toLowerCase(), name);
            throw new AccessDeniedException("Not allowed to change system " +
                    roleTypeClassName.toLowerCase() + " '" + currentRole.getName() + "'");
        }
        if (roleTemplate.isSystem() && !repositorySecurityManager.getHippoSession().isSystemSession()) {
            log.error("Unauthorized attempt to set system status of {} '{}'", roleTypeClassName.toLowerCase(), name);
            throw new AccessDeniedException("Not allowed to set system status of " +
                    roleTypeClassName.toLowerCase() + " '" + currentRole.getName() + "'");
        }
        try {
            final Node roleNodeFromSystemSession = getRolesNodeFromSystemSession().getNode(NodeNameCodec.encode(name));
            if (updateRoleNode(roleTemplate, roleNodeFromSystemSession)) {
                roleNodeFromSystemSession.getSession().save();
                log.info("{} '{} updated by user {}",
                        roleTypeClassName, name, repositorySecurityManager.getHippoSession().getUserID());
                return rolesProvider.getRole(name);
            } else {
                return currentRole;
            }
        } catch (RepositoryException e) {
            silentRefreshSystemSession();
            throw e;
        }
    }

    public boolean deleteRole(final String roleName)
            throws IllegalArgumentException, AccessDeniedException, RepositoryException {
        repositorySecurityManager.checkClosed();
        if (StringUtils.isBlank(roleName)) {
            throw new IllegalArgumentException(roleTypeClassName + " name cannot be null or blank");
        }
        final String name = roleName.trim();
        final R currentRole = rolesProvider.getRole(name);
        if (currentRole == null) {
            return false;
        }
        if (currentRole.isSystem() && !repositorySecurityManager.getHippoSession().isSystemSession()) {
            log.error("Unauthorized attempt to delete system {} '{}'", roleTypeClassName.toLowerCase(), name);
            throw new AccessDeniedException("Not allowed to delete a system " + roleTypeClassName.toLowerCase());
        }
        try {
            final Node roleNodeFromSystemSession = getRolesNodeFromSystemSession().getNode(NodeNameCodec.encode(name));
            roleNodeFromSystemSession.remove();
            roleNodeFromSystemSession.getSession().save();
            log.info("{} '{} deleted by user {}",
                    roleTypeClassName, name, repositorySecurityManager.getHippoSession().getUserID());
            return true;
        } catch (ItemNotFoundException e) {
            return false;
        } catch (RepositoryException e) {
            silentRefreshSystemSession();
            throw e;
        }
    }

    protected boolean updateRoleNode(final R roleTemplate, final Node roleNode) throws RepositoryException {
        boolean updated = false;
        final Boolean nodeSystem = JcrUtils.getBooleanProperty(roleNode, HIPPO_SYSTEM, Boolean.FALSE);
        if (!Boolean.valueOf(roleTemplate.isSystem()).equals(nodeSystem)) {
            if (roleTemplate.isSystem()) {
                roleNode.setProperty(HIPPO_SYSTEM, true);
            } else if (nodeSystem != null) {
                roleNode.getProperty(HIPPO_SYSTEM).remove();
            }
            updated = true;
        }
        final String nodeDescription = JcrUtils.getStringProperty(roleNode, HIPPOSYS_DESCRIPTION, null);
        if (!Objects.equals(roleTemplate.getDescription(), nodeDescription)) {
            roleNode.setProperty(HIPPOSYS_DESCRIPTION, roleTemplate.getDescription());
            updated = true;
        }
        final Set<String> nodeRoles = JcrUtils.getStringSetProperty(roleNode, rolesProvider.getRolesPropertyName(), null);
        if (!Objects.equals(roleTemplate.getRoles(), nodeRoles)) {
            if (roleTemplate.getRoles().isEmpty()) {
                roleNode.setProperty(rolesProvider.getRolesPropertyName(), (String[])null);
            } else {
                roleNode.setProperty(rolesProvider.getRolesPropertyName(), roleTemplate.getRoles().toArray(new String[0]));
            }
            updated = true;
        }
        return updated;
    }
}
