/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.security.AccessControlManager;
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
import org.apache.jackrabbit.core.config.LoginModuleConfig;
import org.apache.jackrabbit.core.config.SecurityConfig;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.SystemPrincipal;
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
import org.apache.jackrabbit.util.ISO9075;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.security.domain.Domain;
import org.hippoecm.repository.security.domain.FacetAuthDomain;
import org.hippoecm.repository.security.domain.InvalidDomainException;
import org.hippoecm.repository.security.group.DummyGroupManager;
import org.hippoecm.repository.security.group.GroupManager;
import org.hippoecm.repository.security.principals.UserPrincipal;
import org.hippoecm.repository.security.service.SessionUserImpl;
import org.hippoecm.repository.security.user.DummyUserManager;
import org.hippoecm.repository.security.user.HippoUserManager;
import org.onehippo.repository.security.SessionUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bloomreach.xm.repository.security.AbstractRole;
import com.bloomreach.xm.repository.security.RepositorySecurityProviders;
import com.bloomreach.xm.repository.security.Role;
import com.bloomreach.xm.repository.security.impl.RepositorySecurityProvidersImpl;

import static org.onehippo.repository.security.SecurityConstants.CONFIG_SECURITY_PATH;

public class SecurityManager implements HippoSecurityManager {

    // TODO: this string is matched as node name in the repository.
    public static final String INTERNAL_PROVIDER = "internal";

    private static final Logger log = LoggerFactory.getLogger(SecurityManager.class);

    // initial JR system session, not impersonated, never logout
    private Session systemSession;
    private final Map<String, SecurityProvider> providers = new LinkedHashMap<>();
    private SecurityConfig config;
    private boolean maintenanceMode;
    private PrincipalProviderRegistry principalProviderRegistry;
    private PermissionManager permissionManager;
    private RepositorySecurityProvidersImpl securityProviders;

    private AuthContextProvider authCtxProvider;

    public void configure() throws RepositoryException {
        SecurityProviderFactory spf = new SecurityProviderFactory(maintenanceMode);
        permissionManager = PermissionManager.getInstance();

        StringBuilder statement = new StringBuilder();
        statement.append("SELECT * FROM ").append(HippoNodeType.NT_SECURITYPROVIDER);
        statement.append(" WHERE");
        statement.append(" jcr:path LIKE '").append(CONFIG_SECURITY_PATH).append("/%").append("'");
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
        // the following must be done after the above configuration: only from here on session.impersonate is possible!
        securityProviders = new RepositorySecurityProvidersImpl(systemSession);
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
                        List<Callback> list = new LinkedList<>();
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

        // check for maintenanceMode (default false) override setting in login-module-configuration.
        for (final Properties aModuleConfig : moduleConfig) {
            if (aModuleConfig.containsKey("maintenanceMode")) {
                maintenanceMode = Boolean.parseBoolean(aModuleConfig.getProperty("maintenanceMode"));
            }
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

                //Firstly, check if credentials object already contains providerId
                final String credentialsProviderId = (String) creds.getAttribute("providerId");
                if (credentialsProviderId != null) {
                    //If so, try to authenticate using selected provider
                    providerId = credentialsProviderId;
                    if (providers.containsKey(providerId)) {
                        log.debug("Trying to authenticate user {} with provider {}", userId, providerId);
                        if (((HippoUserManager)providers.get(providerId).getUserManager()).authenticate(creds)) {
                            authenticated = true;
                        }
                    } else {
                        log.debug("The specified provider in credentials object {} does not exist", providerId);
                        return AuthenticationStatus.FAILED;
                    }
                } else {
                    for(Iterator<String> iter = providers.keySet().iterator(); iter.hasNext();) {
                        providerId = iter.next();
                        log.debug("Trying to authenticate user {} with provider {}", userId, providerId);
                        if (((HippoUserManager)providers.get(providerId).getUserManager()).authenticate(creds)) {
                            authenticated = true;
                            break;
                        }
                    }
                }

                if (!authenticated) {
                    log.debug("No provider found or invalid username and password: {}", userId);
                    return AuthenticationStatus.FAILED;
                }
            }

            log.debug("Found provider: {} for authenticated user: {}", providerId, userId);
            if (creds.getAttribute("providerId") == null) {
                creds.setAttribute("providerId", providerId);
            }

            HippoUserManager userMgr = (HippoUserManager)providers.get(providerId).getUserManager();

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

            providers.get(providerId).synchronizeOnLogin(creds);

            return AuthenticationStatus.SUCCEEDED;
        } catch (RepositoryException e) {
            log.warn("Error while trying to authenticate user: " + userId, e);
            return AuthenticationStatus.FAILED;
        }
    }

    /**
     * Get the applicable domains for the user.
     */
    private Set<Domain> getDomains(final String user, final Set<String> groups, final Set<String> userRoles) {
        Set<Domain> domains = new HashSet<>();
        StringBuilder xpath =
                new StringBuilder("//element(*,").append(HippoNodeType.NT_DOMAINFOLDER)
                        .append(")/element(*,").append(HippoNodeType.NT_DOMAIN)
                        .append(")/element(*,").append(HippoNodeType.NT_AUTHROLE).append(")[");

        // filter user
        xpath.append("@").append(HippoNodeType.HIPPO_USERS).append(" = '");
        xpath.append(ISO9075.encode(NodeNameCodec.encode(user, true))).append("'");
        // filter groups
        for (String group : groups) {
            xpath.append(" or @").append(HippoNodeType.HIPPO_GROUPS).append(" = '");
            xpath.append(ISO9075.encode(NodeNameCodec.encode(group, true))).append("'");
        }
        // filter user roles
        for (String userRole : userRoles) {
            xpath.append(" or @").append(HippoNodeType.HIPPO_USERROLE).append(" = '");
            xpath.append(ISO9075.encode(NodeNameCodec.encode(userRole, true))).append("'");
        }
        xpath.append("]");
        try {
            Query q = systemSession.getWorkspace().getQueryManager().createQuery(xpath.toString(), Query.XPATH);
            QueryResult result = q.execute();
            NodeIterator nodeIter = result.getNodes();
            Set<String> skippedStandardDomainFoldersInNotStandardLocation = new HashSet<>();
            Set<String> skippedFederatedDomainFoldersInNotSupportedLocation = new HashSet<>();
            while (nodeIter.hasNext()) {
                // the parent of the auth role node is the domain node
                Node domainNode = nodeIter.nextNode().getParent();
                Node domainFolderNode = domainNode.getParent();
                if (Domain.isInValidStandardDomainFolderLocation(domainFolderNode)) {
                    skippedStandardDomainFoldersInNotStandardLocation.add(domainFolderNode.getPath());
                    continue;
                } else if (Domain.isInValidFederatedDomainFolderLocation(domainFolderNode)) {
                    skippedFederatedDomainFoldersInNotSupportedLocation.add(domainFolderNode.getPath());
                    continue;
                }
                try {
                    Domain domain = new Domain(domainNode);
                    log.trace("Domain '{}' found for user: {}", domain.getName(), user);
                    domains.add(domain);
                } catch (InvalidDomainException e) {
                    log.error("Skipping invalid domain : {}", e.toString());
                }
            }
            if (log.isWarnEnabled()) {
                if (!skippedStandardDomainFoldersInNotStandardLocation.isEmpty()) {
                    log.warn("Skipped all domains found in not-standard domain folder location(s): [{}]",
                            String.join(", ", skippedStandardDomainFoldersInNotStandardLocation));
                }
                if (!skippedFederatedDomainFoldersInNotSupportedLocation.isEmpty()) {
                    log.warn("Skipped all domains found in not-supported federated domain folder location(s): [{}]",
                            String.join(", ", skippedFederatedDomainFoldersInNotSupportedLocation));
                }
            }
        } catch (RepositoryException e) {
            log.error("Error while searching for domains for user: " + user, e);
        }
        return domains;
    }

    public void assignPrincipals(final Set<Principal> principals, final SimpleCredentials creds) throws RepositoryException {
        try {
            if (creds == null || creds.getUserID() == null || SecurityConstants.ANONYMOUS_ID.equals(creds.getUserID())) {
                throw new LoginException("Anonymous user not supported!");
            }

            Principal userPrincipal;
            Set<String> groupIds;
            Set<String> userRoles;

            HippoUserManager userManager;
            GroupManager groupManager;

            String userId = creds.getUserID();
            String providerId = (String) creds.getAttribute("providerId");
            if (providers.containsKey(providerId)) {
                userManager = ((HippoUserManager)providers.get(providerId).getUserManager());
                groupManager = providers.get(providerId).getGroupManager();
            } else {
                // fallback to internal provider
                userManager = (HippoUserManager)providers.get(INTERNAL_PROVIDER).getUserManager();
                groupManager = providers.get(INTERNAL_PROVIDER).getGroupManager();
            }
            SessionUser user = new SessionUserImpl(userManager.getUser(userId), groupManager,
                    roleNames -> securityProviders.getUserRolesProvider().resolveRoleNames(roleNames));
            userRoles = user.getUserRoles();
            groupIds = user.getMemberships();
            userPrincipal = new UserPrincipal(user, getFacetAuthDomains(user.getId(), groupIds, userRoles));
            principals.add(userPrincipal);
        } catch(RepositoryException ex) {
            log.error("unable to assign principals for user", ex);
            throw ex;
        }
    }

    private Set<FacetAuthDomain> getFacetAuthDomains(final String userId, final Set<String> groupIds,
                                                        final Set<String> userRoles) {
        // Find domains that the user is associated with
        Set<Domain> domains = getDomains(userId, groupIds, userRoles);

        HashSet<FacetAuthDomain> facetAuthDomains = new HashSet<>();

        for (Domain domain : domains) {

            // get roles for a user for a domain
            log.debug("User {} has domain {}", userId, domain.getName());
            final Set<String> roleNames = new HashSet<>();
            roleNames.addAll(domain.getRolesForUser(userId));
            for (final String groupName : groupIds) {
                roleNames.addAll(domain.getRolesForGroup(groupName));
            }
            for (final String userRoleName : userRoles) {
                roleNames.addAll(domain.getRolesForUserRole(userRoleName));
            }
            Set<Role> resolvedRoles = securityProviders.getRolesProvider().resolveRoles(roleNames);
            Set<String> roles = resolvedRoles.stream().map(AbstractRole::getName).collect(Collectors.toSet());
            Set<String> privileges = new HashSet<>();
            resolvedRoles.forEach(role -> privileges.addAll(role.getPrivileges()));

            log.info("User {} has roles {} for domain {} ", userId, roles, domain.getName());
            log.info("User {} has privileges {} for domain {} ", userId, privileges, domain.getName());

            if (privileges.size() > 0 && domain.getDomainRules().size() > 0) {
                // create and add facet auth domain
                FacetAuthDomain fad =
                        new FacetAuthDomain(domain.getName(), domain.getPath(), domain.getDomainRules(), roles,
                                privileges, permissionManager.getOrCreatePermissionNames(privileges));
                if (!facetAuthDomains.add(fad)) {
                    log.error("FacetAuthDomain '{}' is not added because already present in set. This should never happen " +
                            "meaning that FacetAuthDomain has an incorrect #equals implementation", fad);
                }
            }
        }
        return facetAuthDomains;
    }

    public String getUserID(final Subject subject, final String workspace) {
        Principal principal = SubjectHelper.getFirstPrincipal(subject, SystemPrincipal.class);
        if (principal == null) {
            principal = SubjectHelper.getFirstPrincipal(subject, UserPrincipal.class);
        }
        if (principal != null) {
            return principal.getName();
        }
        // unexpected...
        return "<unknown>";
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

    @Override
    public RepositorySecurityProviders getRepositorySecurityProviders() {
        return securityProviders;
    }

    public void dispose(String workspace) {
    }

    public void close() {
        securityProviders.close();
        securityProviders = null;
        for (SecurityProvider provider : providers.values()) {
            provider.remove();
        }
        providers.clear();
    }

    public AuthContext getAuthContext(Credentials credentials, Subject subject, String workspaceName) throws RepositoryException {
        return authCtxProvider.getAuthContext(credentials, subject, systemSession, principalProviderRegistry, null/*"admin"*/, /*"anonymous"*/null);
    }

    public AccessManager getAccessManager(Session session, AMContext amContext) throws RepositoryException {
        AccessControlManager acm = session.getAccessControlManager();
        if (acm instanceof AccessManager) {
            return (AccessManager)acm;
        }
        throw new UnsupportedRepositoryOperationException("AccessManager must be implemented by the AccessControlManager");
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
