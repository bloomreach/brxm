/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.AccessManagerConfig;
import org.apache.jackrabbit.core.config.LoginModuleConfig;
import org.apache.jackrabbit.core.config.SecurityConfig;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.AnonymousPrincipal;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.UserPrincipal;
import org.apache.jackrabbit.core.security.authentication.AuthContext;
import org.apache.jackrabbit.core.security.authentication.AuthContextProvider;
import org.apache.jackrabbit.core.security.authentication.CallbackHandlerImpl;
import org.apache.jackrabbit.core.security.authentication.CredentialsCallback;
import org.apache.jackrabbit.core.security.authentication.ImpersonationCallback;
import org.apache.jackrabbit.core.security.authentication.JAASAuthContext;
import org.apache.jackrabbit.core.security.authentication.LocalAuthContext;
import org.apache.jackrabbit.core.security.authentication.RepositoryCallback;
import org.apache.jackrabbit.core.security.principal.PrincipalProvider;
import org.apache.jackrabbit.core.security.principal.PrincipalProviderRegistry;
import org.apache.jackrabbit.core.security.principal.ProviderRegistryImpl;
import org.apache.jackrabbit.core.security.simple.SimpleAccessManager;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.security.domain.Domain;
import org.hippoecm.repository.security.group.DummyGroupManager;
import org.hippoecm.repository.security.group.GroupManager;
import org.hippoecm.repository.security.principals.FacetAuthPrincipal;
import org.hippoecm.repository.security.principals.GroupPrincipal;
import org.hippoecm.repository.security.role.DummyRoleManager;
import org.hippoecm.repository.security.role.RoleManager;
import org.hippoecm.repository.security.user.DummyUserManager;
import org.hippoecm.repository.security.user.HippoUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityManager implements HippoSecurityManager {

    // TODO: this string is matched as node name in the repository.
    public static final String INTERNAL_PROVIDER = "internal";
    public static final String SECURITY_CONFIG_PATH = HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.SECURITY_PATH;

    private static final Logger log = LoggerFactory.getLogger(SecurityManager.class);

    private String usersPath;
    private String groupsPath;
    private String rolesPath;
    private String domainsPath;

    private Session systemSession;
    private final Map<String, SecurityProvider> providers = new LinkedHashMap<String, SecurityProvider>();
    private String adminID;
    private String anonymID;
    private SecurityConfig config;
    private boolean maintenanceMode;
    private PrincipalProviderRegistry principalProviderRegistry;

    private AuthContextProvider authCtxProvider;

    public void configure() throws RepositoryException {
        Node configNode = systemSession.getRootNode().getNode(SECURITY_CONFIG_PATH);
        usersPath = configNode.getProperty(HippoNodeType.HIPPO_USERSPATH).getString();
        groupsPath = configNode.getProperty(HippoNodeType.HIPPO_GROUPSPATH).getString();
        rolesPath = configNode.getProperty(HippoNodeType.HIPPO_ROLESPATH).getString();
        domainsPath = configNode.getProperty(HippoNodeType.HIPPO_DOMAINSPATH).getString();
        SecurityProviderFactory spf = new SecurityProviderFactory(SECURITY_CONFIG_PATH, usersPath, groupsPath, rolesPath, domainsPath, maintenanceMode);

        StringBuilder statement = new StringBuilder();
        statement.append("SELECT * FROM ").append(HippoNodeType.NT_SECURITYPROVIDER);
        statement.append(" WHERE");
        statement.append(" jcr:path LIKE '/").append(SECURITY_CONFIG_PATH).append("/%").append("'");
        Query q = systemSession.getWorkspace().getQueryManager().createQuery(statement.toString(), Query.SQL);
        QueryResult result = q.execute();
        NodeIterator providerIter = result.getNodes();
        while (providerIter.hasNext()) {
            Node provider = providerIter.nextNode();
            String name;
            try {
                name = provider.getName();
                log.debug("Found secutiry provider: '{}'", name);
                providers.put(name, spf.create(systemSession, name));
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

    class HippoJAASAuthContext extends JAASAuthContext {
        public HippoJAASAuthContext(String appName, CallbackHandler cbHandler, Subject subject) {
            super(appName, cbHandler, subject);
        }
    }

    class HippoLocalAuthContext extends LocalAuthContext {
        public HippoLocalAuthContext(LoginModuleConfig config, CallbackHandler cbHandler, Subject subject) {
            super(config, cbHandler, subject);
        }
    }

    public void init(Repository repository, Session session) throws RepositoryException {
        systemSession = session;
        config = ((RepositoryImpl) repository).getConfig().getSecurityConfig();

        // read the LoginModule configuration
        final LoginModuleConfig loginModConf = config.getLoginModuleConfig();
        authCtxProvider = new AuthContextProvider(config.getAppName(), loginModConf) {
            @Override
            public AuthContext getAuthContext(Credentials credentials,
                                              Subject subject,
                                              Session session,
                                              PrincipalProviderRegistry principalProviderRegistry,
                                              String adminId,
                                              String anonymousId)
                    throws RepositoryException {

                CallbackHandler cbHandler = new CallbackHandlerImpl(credentials, session, principalProviderRegistry, adminId, anonymousId) {
                        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                            List<Callback> list = new LinkedList<Callback>();
                            for(Callback callback : callbacks) {
                                if (callback instanceof NameCallback ||
                                    callback instanceof PasswordCallback ||
                                    callback instanceof ImpersonationCallback ||
                                    callback instanceof RepositoryCallback ||
                                    callback instanceof CredentialsCallback) {
                                    list.add(callback);
                                }
                            }
                            super.handle(list.toArray(new Callback[list.size()]));
                        }
                };

                if (isJAAS()) {
                    return new HippoJAASAuthContext(config.getAppName(), cbHandler, subject);
                } else if (isLocal()) {
                    return new HippoLocalAuthContext(loginModConf, cbHandler, subject);
                } else {
                    throw new RepositoryException("No Login-Configuration");
                }
            }
        };
        if (authCtxProvider.isJAAS()) {
            log.info("init: using JAAS LoginModule configuration for " + config.getAppName());
        } else if (authCtxProvider.isLocal()) {
            log.info("init: using Repository LoginModule configuration for " + config.getAppName());
        } else {
            String msg = "No valid LoginModule configuriation for " + config.getAppName();
            log.error(msg);
            throw new RepositoryException(msg);
        }

        Properties[] moduleConfig = authCtxProvider.getModuleConfig();

        // retrieve default-ids (admin and anomymous) from login-module-configuration.
        for (final Properties aModuleConfig : moduleConfig) {
            if (aModuleConfig.containsKey(LoginModuleConfig.PARAM_ADMIN_ID)) {
                adminID = aModuleConfig.getProperty(LoginModuleConfig.PARAM_ADMIN_ID);
            }
            if (aModuleConfig.containsKey(LoginModuleConfig.PARAM_ANONYMOUS_ID)) {
                anonymID = aModuleConfig.getProperty(LoginModuleConfig.PARAM_ANONYMOUS_ID);
            }
            if (aModuleConfig.containsKey("maintenanceMode")) {
                maintenanceMode = Boolean.parseBoolean(aModuleConfig.getProperty("maintenanceMode"));
            }
        }
        // fallback:
        if (adminID == null) {
            log.debug("No adminID defined in LoginModule/JAAS config -> using default.");
            adminID = SecurityConstants.ADMIN_ID;
        }
        if (anonymID == null) {
            log.debug("No anonymousID defined in LoginModule/JAAS config -> using default.");
            anonymID = SecurityConstants.ANONYMOUS_ID;
        }

        principalProviderRegistry = new ProviderRegistryImpl(new DefaultPrincipalProvider());
        // register all configured principal providers.
        for (final Properties aModuleConfig : moduleConfig) {
            principalProviderRegistry.registerProvider(aModuleConfig);
        }

        providers.put(INTERNAL_PROVIDER, new SimpleSecurityProvider());

    }

    /**
     * Try to authenticate the user. If the user exists in the repository it will
     * authenticate against the responsible security provider or the internal provider
     * if none is set.
     * If the user is not found in the repository it will try to authenticate against
     * all security providers until a successful authentication is found. It uses the
     * natural node order. If the authentication is successful a user node will be
     * created.
     * @return {@link AuthenticationStatus#SUCCEEDED} only if the authentication is successful,
     * {@link AuthenticationStatus#CREDENTIAL_EXPIRED} if the user credentials are expired,
     * otherwise {@link AuthenticationStatus#FAILED}
     */
    public AuthenticationStatus authenticate(SimpleCredentials creds) {
        String userId = creds.getUserID();
        try {
            if (providers.size() == 0) {
                log.error("No security providers found: login is not possible!");
                return AuthenticationStatus.FAILED;
            }

            Node user = ((HippoUserManager)providers.get(INTERNAL_PROVIDER).getUserManager()).getUser(userId);

            // find security provider.
            String providerId = null;
            if (user != null) {
                // user exists. It either must have the provider property set or it is an internal
                // managed user.
                if (user.hasProperty(HippoNodeType.HIPPO_SECURITYPROVIDER)) {
                    providerId = user.getProperty(HippoNodeType.HIPPO_SECURITYPROVIDER).getString();
                    if (!providers.containsKey(providerId)) {
                        log.info("Unable to authenticate user: {}, no such provider: {}", userId, providerId);
                        return AuthenticationStatus.FAILED;
                    }
                } else {
                    providerId = INTERNAL_PROVIDER;
                }

                // check the password
                if (!((HippoUserManager)providers.get(providerId).getUserManager()).authenticate(creds)) {
                    log.debug("Invalid username and password: {}, provider: {}", userId, providerId);
                    return AuthenticationStatus.FAILED;
                }
            } else {
                // loop over providers and try to authenticate.
                boolean authenticated = false;
                for(Iterator<String> iter = providers.keySet().iterator(); iter.hasNext();) {
                    providerId = iter.next();
                    log.debug("Trying to authenticate user {} with provider {}", userId, providerId);
                    if (((HippoUserManager)providers.get(providerId).getUserManager()).authenticate(creds)) {
                        authenticated = true;
                        break;
                    }
                }
                if (!authenticated) {
                    log.debug("No provider found or invalid username and password: {}", userId);
                    return AuthenticationStatus.FAILED;
                }
            }

            log.debug("Found provider: {} for authenticated user: {}", providerId, userId);
            creds.setAttribute("providerId", providerId);
            
            HippoUserManager userMgr = (HippoUserManager)providers.get(providerId).getUserManager();
            GroupManager groupMgr = providers.get(providerId).getGroupManager();

            // Check if user is active
            if (!userMgr.isActive(userId)) {
                log.debug("Inactive user: {}, provider: {}", userId, providerId);
                return AuthenticationStatus.ACCOUNT_EXPIRED;
            } else if (userMgr.isPasswordExpired(userId)) {
                log.debug("Password expired for user: {}, provider: {}", userId, providerId);
                return AuthenticationStatus.CREDENTIAL_EXPIRED;
            }

            // internal provider doesn't need to sync
            if (INTERNAL_PROVIDER.equals(providerId)) {
                return AuthenticationStatus.SUCCEEDED;
            }

            // The sync blocks are synchronized because the underlying
            // methods can share the same jcr session and the jcr session is
            // not thread safe. This is a "best effort" solution as the usrMgr
            // and the groupMgr could also share the same session but generally
            // do not operate on the same nodes.

            synchronized(userMgr) {
                userMgr.syncUserInfo(userId);
                userMgr.updateLastLogin(userId);
                userMgr.saveUsers();
            }

            synchronized(groupMgr) {
                groupMgr.syncMemberships(userMgr.getUser(userId));
                groupMgr.saveGroups();
            }

            return AuthenticationStatus.SUCCEEDED;
        } catch (RepositoryException e) {
            log.warn("Error while trying to authenticate user: " + userId, e);
            return AuthenticationStatus.FAILED;
        }
    }

    /**
     * Get the memberships for a user. See the {@link GroupManager#getMembershipIds(String)} for details.
     *
     *
     * @param rawUserId the unparsed userId
     * @return a set of Strings with the memberships or an empty set if no memberships are found.
     */
    private Set<String> getMemberships(String rawUserId, String providerId) {
        try {
            if (providers.containsKey(providerId)) {
                return providers.get(providerId).getGroupManager().getMembershipIds(rawUserId);
            } else {
                return providers.get(INTERNAL_PROVIDER).getGroupManager().getMembershipIds(sanitizeUserId(rawUserId, providerId));
            }
        } catch (RepositoryException e) {
            log.warn("Unable to get memberships for userId: " + rawUserId, e);
            return null;
        }
    }

    /**
     * Get the domains in which the user has a role.
     * @param rawUserId the unparsed userId
     */
    private Set<Domain> getDomainsForUser(String rawUserId, String providerId) throws RepositoryException {
        if (rawUserId == null) {
            return Collections.emptySet();
        }
        String userId = NodeNameCodec.decode(sanitizeUserId(rawUserId, providerId));
        Set<Domain> domains = new HashSet<>();
        StringBuilder statement = new StringBuilder();
        statement.append("SELECT * FROM ").append(HippoNodeType.NT_AUTHROLE);
        statement.append(" WHERE");
        statement.append(" jcr:path LIKE '/").append(domainsPath).append("/%").append("'");
        statement.append(" AND ");
        statement.append(HippoNodeType.HIPPO_USERS).append(" = '").append(userId.replace("'", "''")).append("'");
        try {
            Query q = systemSession.getWorkspace().getQueryManager().createQuery(statement.toString(), Query.SQL);
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
     */
    private Set<Domain> getDomainsForGroup(String rawGroupId, String providerId) throws RepositoryException {
        String groupId = NodeNameCodec.decode(sanitizeGroupId(rawGroupId, providerId));

        Set<Domain> domains = new HashSet<Domain>();
        StringBuilder statement = new StringBuilder();
        statement.append("SELECT * FROM ").append(HippoNodeType.NT_AUTHROLE);
        statement.append(" WHERE");
        statement.append(" jcr:path LIKE '/").append(domainsPath).append("/%").append("'");
        statement.append(" AND ");
        statement.append(HippoNodeType.HIPPO_GROUPS).append(" = '").append(groupId.replaceAll("'", "''")).append("'");
        try {
            Query q = systemSession.getWorkspace().getQueryManager().createQuery(statement.toString(), Query.SQL);
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

    private Set<String> getRolesForRole(String roleId) {
        return getRolesForRole(roleId, new HashSet<String>());
    }

    private Set<String> getRolesForRole(String roleId, Set<String> currentRoles) {
        Node roleNode;
        log.trace("Looking for role: {} in path: {}", roleId, rolesPath);
        String path = rolesPath + "/" + roleId;
        try {
            roleNode = systemSession.getRootNode().getNode(path);
            log.trace("Found role node: {}", roleNode.getName());
            if (roleNode.hasProperty(HippoNodeType.HIPPO_ROLES)) {
                Value[] values = roleNode.getProperty(HippoNodeType.HIPPO_ROLES).getValues();
                for (Value value : values) {
                    if (!currentRoles.contains(value.getString())) {
                        currentRoles.add(value.getString());
                        currentRoles.addAll(getRolesForRole( value.getString(), currentRoles));
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
    
    private Set<String> getPrivilegesForRole(String roleId) {
        Set<String> privileges = new HashSet<String>();
        Node roleNode;
        log.trace("Looking for role: {} in path: {}", roleId, rolesPath);
        String path = rolesPath + "/" + roleId;
        try {
            roleNode = systemSession.getRootNode().getNode(path);
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
     * @return the trimmed and if needed converted to lowercase userId
     */
    private String sanitizeUserId(String rawUserId, String providerId) throws RepositoryException {
        if (rawUserId == null) {
            return null;
        }
        if (providers.containsKey(providerId)) {
            if (((HippoUserManager)providers.get(providerId).getUserManager()).isCaseSensitive()) {
                return rawUserId.trim();
            } else {
                return rawUserId.trim().toLowerCase();
            }
        } else {
            // fallback to internal provider
            if (((HippoUserManager)providers.get(INTERNAL_PROVIDER).getUserManager()).isCaseSensitive()) {
                return rawUserId.trim();
            } else {
                return rawUserId.trim().toLowerCase();
            }
            
        }
    }

    /**
     * Sanitize the raw userId input according to the case sensitivity of the 
     * security provider.
     * @return the trimmed and if needed converted to lowercase groupId
     */
    private String sanitizeGroupId(String rawGroupId, String providerId) throws RepositoryException {
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

    public void assignPrincipals(Set<Principal>principals, SimpleCredentials creds) {
        String userId = null;
        String providerId = null;

        if (creds != null) {
            userId = creds.getUserID();
            providerId = (String) creds.getAttribute("providerId");
        }

        // XXX: Fixme, why is this needed??
        if (userId != null && userId.equals("system")) {
            userId = null;
        }

        try {
           assignUserPrincipals(principals, userId);
           assignGroupPrincipals(principals, userId, providerId);
           assignFacetAuthPrincipals(principals, userId, providerId);
        } catch(RepositoryException ex) {
            log.warn("unable to assign principals for user", ex);
        }
    }

    private void assignUserPrincipals(Set<Principal> principals, String userId) {
        if(userId == null) {
            principals.add(new AnonymousPrincipal());
        } else {
            principals.add(new UserPrincipal(userId));
        }
    }

    private void assignGroupPrincipals(Set<Principal> principals, String userId, String providerId) {
        for (String groupId : getMemberships(userId, providerId)) {
            principals.add(new GroupPrincipal(groupId));
        }
    }

    private void assignFacetAuthPrincipals(Set<Principal> principals, String userId, String providerId) throws RepositoryException {
        // Find domains that the user is associated with
        Set<Domain> userDomains = new HashSet<Domain>();
        userDomains.addAll(getDomainsForUser(userId, providerId));
        for (Principal principal : principals) {
            if (principal instanceof GroupPrincipal) {
                userDomains.addAll(getDomainsForGroup(principal.getName(), providerId));
            }
        }

        // Add facet auth principals
        for (Domain domain : userDomains) {

            // get roles for a user for a domain
            log.debug("User {} has domain {}", userId, domain.getName());
            Set<String> roles = new HashSet<String>();
            roles.addAll(domain.getRolesForUser(userId));
            for (Principal principal : principals) {
                if (principal instanceof GroupPrincipal) {
                    roles.addAll(domain.getRolesForGroup(principal.getName()));
                }
            }

            // check for indirectly included roles
            Set<String> includedRoles = new HashSet<String>();
            for (String roleId : roles) {
                includedRoles.addAll(getRolesForRole(roleId));
            }
            roles.addAll(includedRoles);

            log.info("User {} has roles {} for domain {} ", new Object[] { userId, roles, domain.getName() });

            // get all privileges associated with the roles
            Set<String> privileges = new HashSet<String>();
            for (String roleId : roles) {
                privileges.addAll(getPrivilegesForRole(roleId));
            }
            log.info("User {} has privileges {} for domain {} ", new Object[] { userId, privileges, domain.getName() });

            if (privileges.size() > 0 && domain.getDomainRules().size() > 0) {
                // create and add facet auth principal
                FacetAuthPrincipal fap = new FacetAuthPrincipal(domain.getName(), domain.getDomainRules(), roles, privileges);
                principals.add(fap);
            }
        }
    }

    public String getUserID(Subject subject, String workspace) {
        String uid = null;
        // if SimpleCredentials are present, the UserID can easily be retrieved.
        Iterator creds = subject.getPublicCredentials(SimpleCredentials.class).iterator();
        if (creds.hasNext()) {
            SimpleCredentials sc = (SimpleCredentials) creds.next();
            uid = sc.getUserID();
            if (!subject.getPrincipals(FacetAuthPrincipal.class).isEmpty()) {
                uid = uid + "-" + UUID.randomUUID().toString();
            }
        } else {
            // assume that UserID and principal name
            // are the same (not totally correct) and thus return the name
            // of the first non-group principal.
            for (Iterator it = subject.getPrincipals().iterator(); it.hasNext();) {
                Principal p = (Principal) it.next();
                // FIXME: dead code
            }
        }
        return uid;
    }

    @Override
    public HippoUserManager getUserManager(final Session session) throws RepositoryException {
        return getUserManager(session, INTERNAL_PROVIDER);
    }

    @Override
    public HippoUserManager getUserManager(final Session session, final String providerId) throws RepositoryException {
        return (HippoUserManager) providers.get(providerId).getUserManager(session);
    }

    @Override
    public GroupManager getGroupManager(final Session session, final String providerId) throws RepositoryException {
        return providers.get(providerId).getGroupManager(session);
    }

    public void dispose(String workspace) {
    }

    public void close() {
    }

   public AuthContext getAuthContext(Credentials credentials, Subject subject, String workspaceName) throws RepositoryException {
        return authCtxProvider.getAuthContext(credentials, subject, systemSession, principalProviderRegistry, null/*"admin"*/, /*"anonymous"*/null);
    }

    public AccessManager getAccessManager(Session session, AMContext amContext) throws RepositoryException {
        try {
            AccessManagerConfig amc = config.getAccessManagerConfig();
            AccessManager accessMgr;
            if (amc == null) {
                accessMgr = new SimpleAccessManager();
            } else {
                accessMgr = amc.newInstance(AccessManager.class);
            }
            accessMgr.init(amContext);
            return accessMgr;
        } catch (AccessDeniedException ade) {
            throw ade;
        } catch (Exception e) {
            String msg = "Failed to instantiate AccessManager implementation";
            log.error(msg, e);
            throw new RepositoryException(msg, e);
        }
    }

    public PrincipalManager getPrincipalManager(Session session) throws RepositoryException {
        return new DefaultPrincipalManager();
    }

    class DefaultPrincipalManager implements PrincipalManager {

        public boolean hasPrincipal(String principalName) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Principal getPrincipal(String principalName) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public PrincipalIterator findPrincipals(String simpleFilter) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public PrincipalIterator findPrincipals(String simpleFilter, int searchType) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public PrincipalIterator getPrincipals(int searchType) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public PrincipalIterator getGroupMembership(Principal principal) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Principal getEveryone() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    class DefaultPrincipalProvider implements PrincipalProvider {

        public Principal getPrincipal(String arg0) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public PrincipalIterator findPrincipals(String arg0) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public PrincipalIterator findPrincipals(String arg0, int arg1) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public PrincipalIterator getPrincipals(int arg0) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public PrincipalIterator getGroupMembership(Principal arg0) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void init(Properties arg0) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void close() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean canReadPrincipal(Session arg0, Principal arg1) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    class SimpleSecurityProvider implements SecurityProvider {
        private UserManager userManager = new DummyUserManager();
        private GroupManager groupManager = new DummyGroupManager();
        private RoleManager roleManager = new DummyRoleManager();
        public void init(SecurityProviderContext context) throws RepositoryException {
        }
        public void sync() {
        }
        public void remove() {
        }
        public UserManager getUserManager() throws RepositoryException {
            return userManager;
        }
        public GroupManager getGroupManager() throws RepositoryException {
            return groupManager;
        }
        public RoleManager getRoleManager() throws RepositoryException {
            return roleManager;
        }

        @Override
        public UserManager getUserManager(final Session session) throws RepositoryException {
            return userManager;
        }

        @Override
        public GroupManager getGroupManager(final Session session) throws RepositoryException {
            return groupManager;
        }
    }

}
