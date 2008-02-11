/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.security;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
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
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.PasswordHelper;
import org.hippoecm.repository.security.user.RepositoryUser;
import org.hippoecm.repository.security.user.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryLoginModule implements LoginModule {


    // initial state
    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, ?> sharedState;
    private Map<String, ?> options;

    // configurable JAAS options
    private boolean debug = false;
    private boolean tryFirstPass = false;
    private boolean useFirstPass = false;
    private boolean storePass = false;
    private boolean clearPass = false;

    // local authentication state:
    // the principals, i.e. the authenticated identities
    private final Set principals = new HashSet();

    // keep the auth state of the user trying to login
    private boolean authenticated = false;


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

            // check for anonymous login
            if (creds == null || creds.getUserID() == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Authenticated as anonymous.");
                }
                // authenticate as anonymous
                principals.add(new AnonymousPrincipal());
                return true;
            }

            Session rootSession = (Session) creds.getAttribute("rootSession");
            if (rootSession == null) {
                throw new LoginException("RootSession not set.");
            }
            if (debug) {
                log.debug("Trying to authenticate as: " + creds.getUserID());
            }

            if (authenticate(rootSession, creds.getUserID(), creds.getPassword())) {
                if (log.isDebugEnabled()) {
                    log.debug("Authenticated as " + creds.getUserID());
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("NOT Authenticated as " + creds.getUserID() + " auth: " + authenticated);
                }
            }

        } catch (ClassCastException e) {
            e.printStackTrace();
            throw new LoginException(e.getMessage());
        } catch (java.io.IOException e) {
            e.printStackTrace();
            throw new LoginException(e.getMessage());
        } catch (UnsupportedCallbackException e) {
            e.printStackTrace();
            throw new LoginException(e.getCallback().toString() + " not available");
        }
        if (authenticated) {
            return !principals.isEmpty();
        } else {
            // authentication failed: clean out state
            principals.clear();
            throw new FailedLoginException();
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
        authenticated = false;
        subject.getPrincipals().removeAll(principals);
        principals.clear();
        return true;
    }

    /**
     * Authenticate the user against the cache or the repository
     * @param session A privileged session which can read usernames and passwords
     * @param userId
     * @param password
     * @return true when authenticated
     */
    private boolean authenticate(Session rootSession, String userId, char[] password) {
        authenticated = false;
        if (userId == null || password == null || "".equals(userId) || password.length == 0) {
            if (log.isDebugEnabled()) {
                log.debug("Empty username or password not allowed.");
            }
            return false;
        }


        String usersPath = HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.USERS_PATH;
        String groupsPath = HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.GROUPS_PATH;
        String rolesPath = HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.ROLES_PATH;

        RepositoryAAContext context = new RepositoryAAContext(rootSession, usersPath, groupsPath, rolesPath);

        RepositoryUser user = new RepositoryUser();
        try {
            user.init(context, userId);
            authenticated =  PasswordHelper.checkHash(new String(password), user.getPasswordHash());
            if (authenticated) {
                principals.addAll(user.getPrincipals());
            }
            return authenticated;
        } catch (UserNotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("User not found: " + userId, e);
            }
            return false;
        } catch (NoSuchAlgorithmException e) {
            log.error("Unable to check password.", e);
            return false;
        } catch (UnsupportedEncodingException e) {
            log.error("Unable to check password.", e);
            return false;
        }
    }
}
