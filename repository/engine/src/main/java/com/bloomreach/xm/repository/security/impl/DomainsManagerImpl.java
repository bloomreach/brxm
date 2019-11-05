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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;

import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.util.ISO9075;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.onehippo.repository.security.SecurityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bloomreach.xm.repository.security.AuthRole;
import com.bloomreach.xm.repository.security.DomainAuth;
import com.bloomreach.xm.repository.security.DomainsManager;

import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_DESCRIPTION;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_GROUPS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_ROLE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_USERROLE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_USERS;
import static org.hippoecm.repository.api.HippoNodeType.NT_AUTHROLE;
import static org.hippoecm.repository.api.HippoNodeType.NT_DOMAIN;
import static org.hippoecm.repository.api.HippoNodeType.NT_DOMAINFOLDER;
import static org.onehippo.repository.security.SecurityConstants.CONFIGURATION_FOLDER_PATH;
import static org.onehippo.repository.security.SecurityConstants.CONFIGURATION_FOLDER_PATH_PREFIX;

public class DomainsManagerImpl implements DomainsManager {

    private static final Logger log = LoggerFactory.getLogger(DomainsManager.class);
    private static final String ALL_DOMAINS_QUERY = "//element(*," + NT_DOMAINFOLDER + ")/element(*," + NT_DOMAIN + ")";
    private static final String ALL_AUTHROLES_QUERY = ALL_DOMAINS_QUERY + "/element(*," + NT_AUTHROLE + ")";
    private static final String AUTHROLES_FOR_USERROLE_QUERY = ALL_AUTHROLES_QUERY + "[@" + HIPPO_USERROLE + "='{}']";
    private static final String AUTHROLES_FOR_GROUP_QUERY = ALL_AUTHROLES_QUERY + "[@" + HIPPO_GROUPS + "='{}']";
    private static final String AUTHROLES_FOR_USER_QUERY = ALL_AUTHROLES_QUERY + "[@" + HIPPO_USERS + "='{}']";

    private final RepositorySecurityManagerImpl repositorySecurityManager;

    DomainsManagerImpl(final RepositorySecurityManagerImpl repositorySecurityManager) {
        this.repositorySecurityManager = repositorySecurityManager;
    }

    private void silentRefreshSystemSession() {
        try {
            repositorySecurityManager.getSystemSession().refresh(false);
        } catch (RepositoryException ignore) {
        }
    }

    private Session getSystemSession() {
        return repositorySecurityManager.getSystemSession();
    }

    private boolean isValidDomainFolderNode(final Node domainFolderNode) throws RepositoryException {
        if (HippoNodeType.NT_DOMAINFOLDER.equals(domainFolderNode.getPrimaryNodeType().getName()) &&
                (domainFolderNode.getDepth() == 2 &&
                        CONFIGURATION_FOLDER_PATH.equals(domainFolderNode.getParent().getPath()))) {
            return true;
        }
        return domainFolderNode.isNodeType(HippoNodeType.NT_FEDERATEDDOMAINFOLDER) &&
                (domainFolderNode.getDepth() > 1 &&
                        !domainFolderNode.getPath().startsWith(CONFIGURATION_FOLDER_PATH_PREFIX));
    }

    private boolean isValidDomainNode(final Node domainNode) throws RepositoryException {
        return domainNode.isNodeType(NT_DOMAIN) && isValidDomainFolderNode(domainNode.getParent());
    }

    private boolean isValidAuthRoleNode(final Node authRoleNode) throws RepositoryException {
        return authRoleNode.isNodeType(NT_AUTHROLE) && isValidDomainNode(authRoleNode.getParent());
    }

    private SortedSet<DomainAuth> getDomainAuths(final String xpathQuery, boolean viaAuthRole) throws RepositoryException {
        SortedSet<DomainAuth> domainAuths = new TreeSet<>();
        final Query q = getSystemSession().getWorkspace().getQueryManager().createQuery(xpathQuery, Query.XPATH);
        final NodeIterator nodeIter = q.execute().getNodes();
        final Set<String> domainPaths = new HashSet<>();
        while (nodeIter.hasNext()) {
            final Node node = nodeIter.nextNode();
            if (viaAuthRole) {
                final String path = node.getPath();
                if (!domainPaths.contains(path) && isValidAuthRoleNode(node)) {
                    domainAuths.add(new DomainAuthImpl(node.getParent()));
                    domainPaths.add(path);
                }
            } else {
                if (isValidDomainNode(node)) {
                    domainAuths.add(new DomainAuthImpl(node));
                }
            }
        }
        return Collections.unmodifiableSortedSet(domainAuths);
    }

    private Node getAuthRoleNode(final String path) throws RepositoryException {
        final Node authRoleNode = getSystemSession().getNode(path);
        if (isValidAuthRoleNode(authRoleNode)) {
            return authRoleNode;
        }
        throw new ItemNotFoundException("No valid " + NT_AUTHROLE + " node found at " + path);
    }

    @Override
    public DomainAuth getDomainAuth(final String domainPath) throws ItemNotFoundException, RepositoryException {
        repositorySecurityManager.checkClosed();
        final Node domainNode = getSystemSession().getNode(domainPath);
        if (isValidDomainNode(domainNode)) {
            return new DomainAuthImpl(domainNode);
        }
        throw new ItemNotFoundException("No valid " + NT_DOMAIN + " node found at " + domainPath);
    }

    @Override
    public SortedSet<DomainAuth> getDomainAuths() throws RepositoryException {
        return getDomainAuths(ALL_DOMAINS_QUERY, false);
    }

    @Override
    public SortedSet<DomainAuth> getDomainAuthsForUser(final String user) throws IllegalArgumentException,
            RepositoryException {
        repositorySecurityManager.checkClosed();
        return getDomainAuths(AUTHROLES_FOR_USER_QUERY.replace("{}", ISO9075.encode(NodeNameCodec.encode(user, true))), true);
    }

    @Override
    public SortedSet<DomainAuth> getDomainAuthsForUserRole(final String userRole) throws IllegalArgumentException,
            RepositoryException {
        repositorySecurityManager.checkClosed();
        return getDomainAuths(AUTHROLES_FOR_USERROLE_QUERY.replace("{}", ISO9075.encode(NodeNameCodec.encode(userRole, true))), true);
    }

    @Override
    public SortedSet<DomainAuth> getDomainAuthsForGroup(final String group) throws IllegalArgumentException,
            RepositoryException {
        repositorySecurityManager.checkClosed();
        return getDomainAuths(AUTHROLES_FOR_GROUP_QUERY.replace("{}", ISO9075.encode(NodeNameCodec.encode(group, true))), true);
    }

    @Override
    public AuthRole getAuthRole(final String authRolePath) throws RepositoryException {
        repositorySecurityManager.checkClosed();
        return (new AuthRoleImpl(getAuthRoleNode(authRolePath)));
    }

    @Override
    public AuthRole addAuthRole(final AuthRole authRoleTemplate) throws IllegalArgumentException,
            AccessDeniedException, RepositoryException {
        repositorySecurityManager.checkClosed();
        if (!repositorySecurityManager.getHippoSession().isUserInRole(SecurityConstants.USERROLE_SECURITY_APPLICATION_MANAGER)) {
            throw new AccessDeniedException("Access Denied.");
        }
        if (authRoleTemplate == null || StringUtils.isBlank(authRoleTemplate.getName()) ||
                StringUtils.isBlank(authRoleTemplate.getDomainPath()) || StringUtils.isBlank(authRoleTemplate.getRole())) {
            throw new IllegalArgumentException("AuthRole cannot be null or without name, domainPath or role");
        }
        final Node domainNode = getSystemSession().getNode(authRoleTemplate.getDomainPath());
        if (!isValidDomainNode(domainNode)) {
            throw new ItemNotFoundException("No valid " + NT_DOMAIN + " node found at " + authRoleTemplate.getDomainPath());
        }
        try {
            final Node authRoleNode = domainNode.addNode(NodeNameCodec.encode(authRoleTemplate.getName()), NT_AUTHROLE);
            authRoleNode.setProperty(HIPPO_ROLE, authRoleTemplate.getRole());
            if (authRoleTemplate.getDescription() != null) {
                authRoleNode.setProperty(HIPPOSYS_DESCRIPTION, authRoleTemplate.getDescription());
            }
            if (authRoleTemplate.getUserRole() != null) {
                authRoleNode.setProperty(HIPPO_USERROLE, authRoleTemplate.getUserRole());
            }
            if (!authRoleTemplate.getGroups().isEmpty()) {
                authRoleNode.setProperty(HIPPO_GROUPS, authRoleTemplate.getGroups().toArray(new String[0]));
            }
            if (!authRoleTemplate.getUsers().isEmpty()) {
                authRoleNode.setProperty(HIPPO_USERS, authRoleTemplate.getGroups().toArray(new String[0]));
            }
            getSystemSession().save();
            AuthRole authRole = new AuthRoleImpl(authRoleNode);
            log.info(NT_AUTHROLE +" at path {} added by user {}", authRole.getPath(),
                    repositorySecurityManager.getHippoSession().getUserID());
            return authRole;
        } catch (RepositoryException e) {
            silentRefreshSystemSession();
            throw e;
        }
    }

    @Override
    public AuthRole updateAuthRole(final AuthRole authRoleTemplate) throws IllegalArgumentException,
            AccessDeniedException, RepositoryException {
        repositorySecurityManager.checkClosed();
        if (!repositorySecurityManager.getHippoSession().isUserInRole(SecurityConstants.USERROLE_SECURITY_APPLICATION_MANAGER)) {
            throw new AccessDeniedException("Access Denied.");
        }
        if (authRoleTemplate == null || StringUtils.isBlank(authRoleTemplate.getName()) ||
                StringUtils.isBlank(authRoleTemplate.getDomainPath()) || StringUtils.isBlank(authRoleTemplate.getRole())) {
            throw new IllegalArgumentException("AuthRole cannot be null or without name, domainPath or role");
        }
        try {
            boolean changed = false;
            final Node authRoleNode = getAuthRoleNode(authRoleTemplate.getPath());
            final AuthRoleImpl authRole = new AuthRoleImpl(authRoleNode);
            if (!StringUtils.equals(authRoleTemplate.getRole(), authRole.getRole())) {
                authRoleNode.setProperty(HIPPO_ROLE, authRoleTemplate.getRole());
                changed = true;
            }
            if (!StringUtils.equals(authRoleTemplate.getDescription(), authRole.getDescription())) {
                authRoleNode.setProperty(HIPPOSYS_DESCRIPTION, authRoleTemplate.getDescription());
                changed = true;
            }
            if (!StringUtils.equals(authRoleTemplate.getUserRole(), authRole.getUserRole())) {
                authRoleNode.setProperty(HIPPO_USERROLE, authRoleTemplate.getUserRole());
                changed = true;
            }
            if (!SetUtils.isEqualSet(authRoleTemplate.getGroups(), authRole.getGroups())) {
                if (authRoleTemplate.getGroups().isEmpty()) {
                    authRoleNode.getProperty(HIPPO_GROUPS).remove();
                } else {
                    authRoleNode.setProperty(HIPPO_GROUPS, authRoleTemplate.getGroups().toArray(new String[0]));
                }
                changed = true;
            }
            if (!SetUtils.isEqualSet(authRoleTemplate.getUsers(), authRole.getUsers())) {
                if (authRoleTemplate.getUsers().isEmpty()) {
                    authRoleNode.getProperty(HIPPO_USERS).remove();
                } else {
                    authRoleNode.setProperty(HIPPO_USERS, authRoleTemplate.getUsers().toArray(new String[0]));
                }
                changed = true;
            }
            if (changed) {
                getSystemSession().save();
                log.info(NT_AUTHROLE +" at path {} updated by user {}", authRole.getPath(),
                        repositorySecurityManager.getHippoSession().getUserID());
                return new AuthRoleImpl(authRoleNode);
            }
            return authRole;
        } finally {
            silentRefreshSystemSession();
        }
    }

    @Override
    public boolean deleteAuthRole(final AuthRole authRoleTemplate) throws IllegalArgumentException,
            AccessDeniedException, RepositoryException {
        repositorySecurityManager.checkClosed();
        if (!repositorySecurityManager.getHippoSession().isUserInRole(SecurityConstants.USERROLE_SECURITY_APPLICATION_MANAGER)) {
            throw new AccessDeniedException("Access Denied.");
        }
        if (authRoleTemplate == null || StringUtils.isBlank(authRoleTemplate.getPath())) {
            throw new IllegalArgumentException("AuthRole cannot be null or without path");
        }
        try {
            getAuthRoleNode(authRoleTemplate.getPath()).remove();
            getSystemSession().save();
            log.info(NT_AUTHROLE +" at path {} deleted by user {}", authRoleTemplate.getPath(),
                    repositorySecurityManager.getHippoSession().getUserID());
            return true;
        } catch (PathNotFoundException e) {
            return false;
        } finally {
            silentRefreshSystemSession();
        }
    }
}
