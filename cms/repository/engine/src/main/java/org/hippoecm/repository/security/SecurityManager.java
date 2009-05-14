/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository.security;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.security.domain.Domain;
import org.hippoecm.repository.security.group.GroupManager;
import org.hippoecm.repository.security.role.Role;
import org.hippoecm.repository.security.user.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityManager {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    // TODO: this string is matched as node name in the repository.
    public final static String INTERNAL_PROVIDER = "internal";
    public final static String SECURITY_CONFIG_PATH = HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.SECURITY_PATH;

    private String usersPath;
    private String groupsPath;
    private String rolesPath;
    private String domainsPath;

    private Session session;
    private EventListener listener;
    private final Map<String, SecurityProvider> providers = new LinkedHashMap<String, SecurityProvider>();

    /**
     * The instance
     */
    private static final SecurityManager instance = new SecurityManager();

    /**
     * Logger
     */
    private final Logger log = LoggerFactory.getLogger(SecurityManager.class);

    /**
     * Get the SecurityManager instance with lazy initialization.
     * @return
     */
    public static SecurityManager getInstance() {
        return instance;
    }

    /**
     * Initialize the SecurityManager. Only run the initialization if the session
     * is no longer valid. In the initialization the observation is set on the
     * security node to detect changes to the security providers configurations.
     * @param session
     * @throws RepositoryException
     */
    public void init(Session session) throws RepositoryException {
        if (this.session != null && this.session.isLive()) {
            return;
        }
        this.session = session;

        // initial create
        createSecurityProviders();

        // start a listener to check if provider are added or removed
        ObservationManager obMgr = session.getWorkspace().getObservationManager();
        listener = new EventListener() {
            public void onEvent(EventIterator events) {
                try {
                    clearProviders();
                    createSecurityProviders();
                } catch (RepositoryException e) {
                    log.info("Failed to reload config for provider: {}", e.getMessage());
                }
            }
        };
        obMgr.addEventListener(listener, Event.NODE_ADDED | Event.NODE_REMOVED, "/" + SECURITY_CONFIG_PATH, true, null,
                new String[] { HippoNodeType.NT_SECURITYPROVIDER }, true);

    }

    /**
     * Try to authenticate the user. If the user exists in the repository it will
     * authenticate against the responsible security provider or the internal provider
     * if none is set.
     * If the user is not found in the repository it will try to authenticate against
     * all security providers until a successful authentication is found. It uses the
     * natural node order. If the authentication is successful a user node will be
     * created.
     * @param creds
     * @return true only if the authentication is successful
     */
    public boolean authenticate(SimpleCredentials creds) {
        String userId = creds.getUserID();
        try {
            if (providers.size() == 0) {
                log.error("No security providers found: login is not possible!");
                return false;
            }

            Node user = providers.get(INTERNAL_PROVIDER).getUserManager().getUser(userId);

            // find security provider.
            String providerId = null;
            if (user != null) {
                // user exists. It either must have the provider property set or it is an internal
                // managed user.
                if (user.hasProperty(HippoNodeType.HIPPO_SECURITYPROVIDER)) {
                    providerId = user.getProperty(HippoNodeType.HIPPO_SECURITYPROVIDER).getString();
                    if (!providers.containsKey(providerId)) {
                        log.info("Unable to authenticate user: {}, no such provider: {}", userId, providerId);
                        return false;
                    }
                } else {
                    providerId = INTERNAL_PROVIDER;
                }

                // check the password
                if (!providers.get(providerId).getUserManager().authenticate(creds)) {
                    log.debug("Invalid username and password: {}, provider: {}", userId, providerId);
                    return false;
                }
            } else {
                // loop over providers and try to authenticate.
                boolean authenticated = false;
                for(Iterator<String> iter = providers.keySet().iterator(); iter.hasNext();) {
                    providerId = iter.next();
                    log.debug("Trying to authenticate user {} with provider {}", userId, providerId);
                    if (providers.get(providerId).getUserManager().authenticate(creds)) {
                        authenticated = true;
                        break;
                    }
                }
                if (!authenticated) {
                    log.debug("No provider found or invalid username and password: {}", userId);
                    return false;
                }
            }

            log.debug("Found provider: {} for authenticated user: {}", providerId, userId);
            creds.setAttribute("providerId", providerId);
            
            UserManager userMgr = providers.get(providerId).getUserManager();
            GroupManager groupMgr = providers.get(providerId).getGroupManager();

            // check if user is active
            if (!userMgr.isActive(userId)) {
                log.debug("User not active: {}, provider: {}", userId, providerId);
                return false;
            }
            
            // internal provider doesn't need to sync
            if (INTERNAL_PROVIDER.equals(providerId)) {
                return true;
            }

            // sync user info and create user node if needed
            userMgr.syncUserInfo(userId);
            userMgr.updateLastLogin(userId);
            userMgr.saveUsers();

            // sync group info
            groupMgr.syncMemberships(userMgr.getUser(userId));
            groupMgr.saveGroups();

            // TODO: move to cron?
            providers.get(providerId).sync();

            return true;
        } catch (RepositoryException e) {
            log.warn("Error while trying to authenticate user: " + userId, e);
            return false;
        }
    }

    /**
     * Call the remove method for all providers to give them a chance to shutdown
     * properly.
     */
    private void clearProviders() {
        // clear out 'old' providers
        for (SecurityProvider provider : providers.values()) {
            provider.remove();
        }
        providers.clear();
    }

    /**
     * Create the security providers based on the configuration in the repository. The
     * providers are created by the SecurityProviderFactory.
     * @throws RepositoryException
     */
    private void createSecurityProviders() throws RepositoryException {
        Node configNode = session.getRootNode().getNode(SECURITY_CONFIG_PATH);
        usersPath = configNode.getProperty(HippoNodeType.HIPPO_USERSPATH).getString();
        groupsPath = configNode.getProperty(HippoNodeType.HIPPO_GROUPSPATH).getString();
        rolesPath = configNode.getProperty(HippoNodeType.HIPPO_ROLESPATH).getString();
        domainsPath = configNode.getProperty(HippoNodeType.HIPPO_DOMAINSPATH).getString();
        SecurityProviderFactory spf = new SecurityProviderFactory(SECURITY_CONFIG_PATH, usersPath, groupsPath, rolesPath, domainsPath);

        StringBuffer statement = new StringBuffer();
        statement.append("SELECT * FROM ").append(HippoNodeType.NT_SECURITYPROVIDER);
        statement.append(" WHERE");
        statement.append(" jcr:path LIKE '/").append(SECURITY_CONFIG_PATH).append("/%").append("'");
        Query q = session.getWorkspace().getQueryManager().createQuery(statement.toString(), Query.SQL);
        QueryResult result = q.execute();
        NodeIterator providerIter = result.getNodes();
        while (providerIter.hasNext()) {
            Node provider = providerIter.nextNode();
            String name = null;
            try {
                name = provider.getName();
                log.debug("Found secutiry provider: '{}'", name);
                providers.put(name, spf.create(session, name));
                log.info("Security provider '{}' initialized.", name);
            } catch (ClassNotFoundException e) {
                log.error("Class not found for security provider: " + e.getMessage());
                log.debug("Stack: ", e);
            } catch (InstantiationException e) {
                log.error("Could not instantiate class for security provider: " + e.getMessage());
                log.debug("Stack: ", e);
            } catch (NoSuchMethodError e) {
                log.error("Method not found for security provider: " + e.getMessage());
                log.debug("Stack: ", e);
            } catch (IllegalAccessException e) {
                log.error("Not allowed to instantiate class for security provider: " + e.getMessage());
                log.debug("Stack: ", e);
            } catch (RepositoryException e) {
                log.error("Error while creating security provider: " + e.getMessage());
                log.debug("Stack: ", e);
            }
        }
        if (providers.size() == 0) {
            log.error("No security providers found: login will not be possible!");
        }
    }

    /**
     * Get the memberships for a user. See the AbstractUserManager.getMemberships for details.
     * @param rawUserId the unparsed userId
     * @return a set of Strings with the memberships or an empty set if no memberships are found.
     */
    public Set<String> getMemberships(String rawUserId, String providerId) {
        try {
            if (providers.containsKey(providerId)) {
                return providers.get(providerId).getGroupManager().getMemberships(rawUserId);
            } else {
                return providers.get(INTERNAL_PROVIDER).getGroupManager().getMemberships(sanitizeUserId(rawUserId, providerId));
            }
        } catch (RepositoryException e) {
            log.warn("Unable to get memberships for userId: " + rawUserId, e);
            return new HashSet<String>(0);
        }
    }

    /**
     * Get the domains in which the user has a role.
     * @param rawUserId the unparsed userId
     * @return
     */
    public Set<Domain> getDomainsForUser(String rawUserId, String providerId) {
        String userId = sanitizeUserId(rawUserId, providerId);
        Set<Domain> domains = new HashSet<Domain>();
        StringBuffer statement = new StringBuffer();
        statement.append("SELECT * FROM ").append(HippoNodeType.NT_AUTHROLE);
        statement.append(" WHERE");
        statement.append(" jcr:path LIKE '/").append(domainsPath).append("/%").append("'");
        statement.append(" AND ");
        statement.append(HippoNodeType.HIPPO_USERS).append(" = '").append(userId).append("'");
        try {
            Query q = session.getWorkspace().getQueryManager().createQuery(statement.toString(), Query.SQL);
            QueryResult result = q.execute();
            NodeIterator nodeIter = result.getNodes();
            while (nodeIter.hasNext()) {
                // the parent of the auth role node is the domain node
                Domain domain = new Domain(nodeIter.nextNode().getParent());
                log.trace("Domain '{}' found for user: {}", domain.getName(), userId);
                domains.add(domain);
            }
        } catch (RepositoryException e) {
            log.error("Error while searching for domains for user: " + userId, e);
        }
        return domains;
    }

    /**
     * Get the domains in which the group has a role.
     * @param rawGroupId
     * @return
     */
    public Set<Domain> getDomainsForGroup(String rawGroupId, String providerId) {
        String groupId = sanitizeGroupId(rawGroupId, providerId);
        Set<Domain> domains = new HashSet<Domain>();
        StringBuffer statement = new StringBuffer();
        statement.append("SELECT * FROM ").append(HippoNodeType.NT_AUTHROLE);
        statement.append(" WHERE");
        statement.append(" jcr:path LIKE '/").append(domainsPath).append("/%").append("'");
        statement.append(" AND ");
        statement.append(HippoNodeType.HIPPO_GROUPS).append(" = '").append(groupId).append("'");
        try {
            Query q = session.getWorkspace().getQueryManager().createQuery(statement.toString(), Query.SQL);
            QueryResult result = q.execute();
            NodeIterator nodeIter = result.getNodes();
            while (nodeIter.hasNext()) {
                // the parent of the auth role node is the domain node
                Domain domain = new Domain(nodeIter.nextNode().getParent());
                log.trace("Domain '{}' found for group: {}", domain.getName(), groupId);
                domains.add(domain);
            }
        } catch (RepositoryException e) {
            log.error("Error while searching for domains for group: " + groupId, e);
        }
        return domains;
    }

//    /**
//     * Get the numerical permissions of a role.
//     * @param roleId
//     * @return
//     * @deprecated
//     */
//    public int getJCRPermissionsForRole(String roleId) {
//        int permissions = 0;
//        Node roleNode;
//
//        // does the role already exists
//        log.trace("Looking for role: {} in path: {}", roleId, rolesPath);
//        String path = rolesPath + "/" + roleId;
//        try {
//            try {
//                roleNode = session.getRootNode().getNode(path);
//                log.trace("Found role node: {}", roleNode.getName());
//            } catch (PathNotFoundException e) {
//                log.warn("Role not found: {}", roleId);
//                return Role.NONE;
//            }
//            try {
//                if (roleNode.getProperty(HippoNodeType.HIPPO_JCRREAD).getBoolean()) {
//                    log.trace("Adding jcr read permissions for role: {}", roleId);
//                    permissions += Role.READ;
//                }
//            } catch (PathNotFoundException e) {
//                // ignore, role doesn't has the permission
//            }
//
//            try {
//                if (roleNode.getProperty(HippoNodeType.HIPPO_JCRWRITE).getBoolean()) {
//                    log.trace("Adding jcr write permissions for role: {}", roleId);
//                    permissions += Role.WRITE;
//                }
//            } catch (PathNotFoundException e) {
//                // ignore, role doesn't has the permission
//            }
//
//            try {
//                if (roleNode.getProperty(HippoNodeType.HIPPO_JCRREMOVE).getBoolean()) {
//                    log.trace("Adding jcr remove permissions for role: {}", roleId);
//                    permissions += Role.REMOVE;
//                }
//            } catch (PathNotFoundException e) {
//                // ignore, role doesn't has the permission
//            }
//        } catch (RepositoryException e) {
//            log.error("Error while looking up role: " + roleId, e);
//            return Role.NONE;
//        }
//        return permissions;
//    }

    /**
     * Get the roles included be roleId
     * @param roleId
     * @return
     */
    public Set<String> getRolesForRole(String roleId) {
        return getRolesForRole(roleId, new HashSet<String>());
    }

    /**
     * Internal helper method to recursively find the roles belonging to a role
     * @param roleId
     * @param currentRoles
     * @return
     */
    private Set<String> getRolesForRole(String roleId, Set<String> currentRoles) {
        Node roleNode;
        log.trace("Looking for role: {} in path: {}", roleId, rolesPath);
        String path = rolesPath + "/" + roleId;
        try {
            roleNode = session.getRootNode().getNode(path);
            log.trace("Found role node: {}", roleNode.getName());
            if (roleNode.hasProperty(HippoNodeType.HIPPO_ROLES)) {
                Value[] values = roleNode.getProperty(HippoNodeType.HIPPO_ROLES).getValues();
                for (Value value : values) {
                    if (!currentRoles.contains(value.getString())) {
                        currentRoles.add(value.getString());
                        currentRoles.addAll(getRolesForRole(value.getString(), currentRoles));
                    }
                }
            }
        } catch (PathNotFoundException e) {
            // log at info level instead of warn, this occurs a lot during unit tests
            log.info("Role not found: {}", roleId);
        } catch (RepositoryException e) {
            log.error("Error while looking up role: " + roleId, e);
        }
        return currentRoles;
    }
    
    /**
     * Get the privileges of a role
     * @param roleId
     * @return
     */
    public Set<String> getPrivilegesForRole(String roleId) {
        Set<String> privileges = new HashSet<String>();
        Node roleNode;
        log.trace("Looking for role: {} in path: {}", roleId, rolesPath);
        String path = rolesPath + "/" + roleId;
        try {
            roleNode = session.getRootNode().getNode(path);
            log.trace("Found role node: {}", roleNode.getName());
            if (roleNode.hasProperty(HippoNodeType.HIPPO_PRIVILEGES)) {
                Value[] values = roleNode.getProperty(HippoNodeType.HIPPO_PRIVILEGES).getValues();
                for (Value value : values) {
                    // FIXME: temp hack for aggregate privileges as defined in jsr-283, 6.11.1.2
                    String privilege = value.getString();
                    if ("jcr:write".equals(privilege)) {
                        privileges.add("jcr:write");
                        privileges.add("jcr:setProperties");
                        privileges.add("jcr:addChildNodes");
                        privileges.add("jcr:removeChildNodes");
                    } else if ("jcr:all".equals(privilege)) {
                        privileges.add("jcr:read");
                        // jcr:acp
                        privileges.add("jcr:getAccessControlPolicy");
                        privileges.add("jcr:setAccessControlPolicy");
                        // jcr:wrte
                        privileges.add("jcr:setProperties");
                        privileges.add("jcr:addChildNodes");
                        privileges.add("jcr:removeChildNodes");
                        
                    } else {
                        privileges.add(privilege);
                    }
                }
            }
        } catch (PathNotFoundException e) {
            // log at info level instead of warn, this occurs a lot during unit tests
            log.info("Role not found: {}", roleId);
        } catch (RepositoryException e) {
            log.error("Error while looking up role: " + roleId, e);
        }
        return privileges;
    }
    
    /**
     * Sanitize the raw userId input according to the case sensitivity of the 
     * security provider.
     * @param rawUserId
     * @param providerId
     * @return the trimmed and if needed converted to lowercase userId
     */
    private String sanitizeUserId(String rawUserId, String providerId) {
        if (rawUserId == null) {
            return null;
        }
        if (providers.containsKey(providerId)) {
            if (providers.get(providerId).getUserManager().isCaseSensitive()) {
                return rawUserId.trim();
            } else {
                return rawUserId.trim().toLowerCase();
            }
        } else {
            // fallback to internal provider
            if (providers.get(INTERNAL_PROVIDER).getUserManager().isCaseSensitive()) {
                return rawUserId.trim();
            } else {
                return rawUserId.trim().toLowerCase();
            }
            
        }
    }

    /**
     * Sanitize the raw userId input according to the case sensitivity of the 
     * security provider.
     * @param rawGroupId
     * @param providerId
     * @return the trimmed and if needed converted to lowercase groupId
     */
    private String sanitizeGroupId(String rawGroupId, String providerId) {
        if (rawGroupId == null) {
            return null;
        }
        if (providers.containsKey(providerId)) {
            if (providers.get(providerId).getGroupManager().isCaseSensitive()) {
                return rawGroupId.trim();
            } else {
                return rawGroupId.trim().toLowerCase();
            }
        } else {
            // fallback to internal provider
            if (providers.get(INTERNAL_PROVIDER).getGroupManager().isCaseSensitive()) {
                return rawGroupId.trim();
            } else {
                return rawGroupId.trim().toLowerCase();
            }
            
        }
    }
}
