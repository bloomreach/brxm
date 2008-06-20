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

import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.jackrabbit.core.security.AnonymousPrincipal;
import org.apache.jackrabbit.core.security.CredentialsCallback;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.UserPrincipal;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.security.domain.Domain;
import org.hippoecm.repository.security.domain.Domains;
import org.hippoecm.repository.security.group.Group;
import org.hippoecm.repository.security.group.GroupException;
import org.hippoecm.repository.security.group.GroupManager;
import org.hippoecm.repository.security.group.RepositoryGroupManager;
import org.hippoecm.repository.security.principals.FacetAuthPrincipal;
import org.hippoecm.repository.security.principals.GroupPrincipal;
import org.hippoecm.repository.security.role.RepositoryRole;
import org.hippoecm.repository.security.role.Role;
import org.hippoecm.repository.security.role.RoleNotFoundException;
import org.hippoecm.repository.security.user.RepositoryUser;
import org.hippoecm.repository.security.user.User;
import org.hippoecm.repository.security.user.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryLoginModule implements LoginModule {

    /** SVN id placeholder */
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    // initial state
    private Subject subject;
    private CallbackHandler callbackHandler;
    @SuppressWarnings("unused")
    private Map<String, ?> sharedState;

    @SuppressWarnings("unused")
    private Map<String, ?> options;

    // configurable JAAS options
    @SuppressWarnings("unused")
    private boolean debug = false;

    private boolean tryFirstPass = false;
    private boolean useFirstPass = false;
    private boolean storePass = false;
    private boolean clearPass = false;

    // local authentication state:
    // the principals, i.e. the authenticated identities
    private final Set<Principal> principals = new HashSet<Principal>();

    // contexts
    // TODO: Add option to configure different user and group backends (ie ldap)
    private AAContext userContext;
    private AAContext groupContext;
    private AAContext domainContext;
    private AAContext roleContext;

    private GroupManager groupManager = new RepositoryGroupManager();

    // the rootSession
    private Session rootSession;

    // the user
    private User user = new RepositoryUser();

    /**
     * Get Logger
     */
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Constructor
     */
    public RepositoryLoginModule() {
    }

    //----------------------------------------------------------< LoginModule >
    /**
     * {@inheritDoc}
     */
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
            Map<String, ?> options) {

        // set state
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;

        // fetch default JAAS parameters
        debug = "true".equalsIgnoreCase((String) options.get("debug"));
        tryFirstPass = "true".equalsIgnoreCase((String) options.get("tryFirstPass"));
        useFirstPass = "true".equalsIgnoreCase((String) options.get("useFirstPass"));
        storePass = "true".equalsIgnoreCase((String) options.get("storePass"));
        clearPass = "true".equalsIgnoreCase((String) options.get("clearPass"));

        if (log.isDebugEnabled()) {
            log.debug("RepositoryLoginModule JAAS config:");
            log.debug("* tryFirstPass   : " + tryFirstPass);
            log.debug("* useFirstPass   : " + useFirstPass);
            log.debug("* storePass      : " + storePass);
            log.debug("* clearPass      : " + clearPass);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean login() throws LoginException {
        if (callbackHandler == null) {
            throw new LoginException("no CallbackHandler available");
        }

        try {
            // Get credentials using a JAAS callback
            CredentialsCallback ccb = new CredentialsCallback();
            callbackHandler.handle(new Callback[] { ccb });
            SimpleCredentials creds = (SimpleCredentials) ccb.getCredentials();
            rootSession = (Session) creds.getAttribute("rootSession");
            if (rootSession == null) {
                throw new LoginException("RootSession not set.");
            }

            // set the Contexts
            createContexts();

            // check for impersonation
            Object attr = creds.getAttribute(SecurityConstants.IMPERSONATOR_ATTRIBUTE);
            if (attr != null && attr instanceof Subject) {
                Subject impersonator = (Subject) attr;

                // anonymous cannot impersonate
                if (!impersonator.getPrincipals(AnonymousPrincipal.class).isEmpty()) {
                    log.info("Denied Anymous impersonating as {}", creds.getUserID());
                    return false;
                }

                // check for valid user
                if (impersonator.getPrincipals(UserPrincipal.class).isEmpty()) {
                    log.info("Denied unknown user impersonating as {}", creds.getUserID());
                    return false;
                }

                Principal iup = (Principal) impersonator.getPrincipals(UserPrincipal.class).iterator().next();
                String impersonarorId = iup.getName();
                // TODO: check somehow if the user is allowed to imporsonate

                log.info("Impersonating as {} by {}", creds.getUserID(), impersonarorId);
                setUserPrincipals(creds.getUserID());
                setGroupPrincipals(creds.getUserID());
                setFacetAuthPrincipals(creds.getUserID());
                return !principals.isEmpty();
            }

            // check for anonymous login
            if (creds == null || creds.getUserID() == null) {
                // authenticate as anonymous
                principals.add(new AnonymousPrincipal());
                log.info("Authenticated as anonymous.");
                setUserPrincipals(null);
                setGroupPrincipals(null);
                setFacetAuthPrincipals(null);
                return true;
            }

            log.debug("Trying to authenticate as {}", creds.getUserID());
            if (authenticate(creds.getUserID(), creds.getPassword())) {
                log.info("Authenticated as {}", creds.getUserID());
                setUserPrincipals(creds.getUserID());
                setGroupPrincipals(creds.getUserID());
                setFacetAuthPrincipals(creds.getUserID());
                return !principals.isEmpty();
            } else {
                log.info("NOT Authenticated as {}", creds.getUserID());
                // authentication failed: clean out state
                principals.clear();
                throw new FailedLoginException();
            }
        } catch (ClassCastException e) {
            log.error("Error during login", e);
            throw new LoginException(e.getMessage());
        } catch (java.io.IOException e) {
            log.error("Error during login", e);
            throw new LoginException(e.getMessage());
        } catch (UnsupportedCallbackException e) {
            log.error("Error during login", e);
            throw new LoginException(e.getCallback().toString() + " not available");
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean commit() throws LoginException {
        if (principals.isEmpty()) {
            return false;
        } else {
            // add a principals (authenticated identities) to the Subject
            subject.getPrincipals().addAll(principals);
            return true;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean abort() throws LoginException {
        if (principals.isEmpty()) {
            return false;
        } else {
            logout();
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean logout() throws LoginException {
        subject.getPrincipals().removeAll(principals);
        principals.clear();
        return true;
    }

    /**
     * Create user and group context
     * TODO: The context impl should be configurable and depend on the backend (eg JCR, LDAP)
     */
    private void createContexts() {
        String usersPath = HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.USERS_PATH;
        String groupsPath = HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.GROUPS_PATH;
        String rolesPath = HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.ROLES_PATH;
        String domainsPath = HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.DOMAINS_PATH;
        userContext = new RepositoryAAContext(rootSession, usersPath);
        groupContext = new RepositoryAAContext(rootSession, groupsPath);
        domainContext = new RepositoryAAContext(rootSession, domainsPath);
        roleContext = new RepositoryAAContext(rootSession, rolesPath);
    }

    /**
     * Authenticate the user against the cache or the repository
     * @param session A privileged session which can read usernames and passwords
     * @param userId
     * @param password
     * @return true when authenticated
     */
    private boolean authenticate(String userId, char[] password) {
        try {
            // check for anonymous user
            if (userId == null && password == null) {
                principals.add(new AnonymousPrincipal());
                log.debug("Authenticated as Anonymous user.");
                return true;
            }

            // basic security check
            if (userId == null || "".equals(userId) || password == null || password.length == 0) {
                log.debug("Empty username or password not allowed.");
                return false;
            }

            // check the password
            user.init(userContext, userId);
            return user.checkPassword(password) && user.isActive();
        } catch (UserException e) {
            log.info("Unable to authenticate user: {}", userId);
            log.debug("Unable to authenticate user: ", e);
            return false;
        }
    }

    private void setUserPrincipals(String userId) {
        if (userId == null) {
            return;
        }
        UserPrincipal userPrincipal;
        userPrincipal = new UserPrincipal(userId);
        log.debug("Adding principal: {}", userPrincipal);
        principals.add(userPrincipal);
    }

    private void setGroupPrincipals(String userId) {
        try {
            groupManager.init(groupContext);
            Set<Group> memberships = groupManager.listMemeberships(userId);
            for (Group group : memberships) {
                try {
                    GroupPrincipal groupPrincipal = new GroupPrincipal(group.getGroupId());
                    principals.add(groupPrincipal);
                    log.debug("Adding principal: {}", groupPrincipal);
                } catch (GroupException e) {
                    log.warn("Error while adding group principals", e);
                }
            }
        } catch (GroupException e) {
            log.error("Error while adding GroupPrincipals", e);
        }
    }

    private void setFacetAuthPrincipals(String userId) {
        Domains domains = new Domains();
        domains.init(domainContext);

        // Find domains that the user is associated with
        Set<Domain> userDomains = new HashSet<Domain>();
        userDomains.addAll(domains.getDomainsForUser(userId));
        for(Principal principal : principals) {
            if (principal instanceof GroupPrincipal) {
                userDomains.addAll(domains.getDomainsForGroup(principal.getName()));
            }
        }

        // Add facet auth principals
        for (Domain domain : userDomains) {

            // get roles for a user for a domain
            log.debug("User {} has domain {}", userId, domain.getName());
            Set<String> roles = new HashSet<String>();
            roles.addAll(domain.getRolesForUser(userId));
            for(Principal principal : principals) {
                if (principal instanceof GroupPrincipal) {
                    roles.addAll(domain.getRolesForGroup(principal.getName()));
                }
            }

            // merge permissions for the roles for a domain
            int perms = 0;
            for (String roleId : roles) {
                try {
                    Role role = new RepositoryRole();
                    role.init(roleContext, roleId);
                    // perms are bit sets. Use OR to merge permissions
                    perms |= role.getJCRPermissions();
                } catch (RoleNotFoundException e) {
                    log.warn("Role {} used in domain {} not found", roleId, domain.getName());
                }
            }
            log.trace("User {} has perms {} for domain {} ", new Object[]{userId, perms, domain});

            // create and add facet auth principal
            FacetAuthPrincipal fap = new FacetAuthPrincipal(domain.getName(), domain.getDomainRules(), roles, perms);
            principals.add(fap);
        }
    }
}
