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

import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.jackrabbit.core.security.AnonymousPrincipal;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.security.UserPrincipal;
import org.apache.jackrabbit.core.security.authentication.CredentialsCallback;
import org.apache.jackrabbit.core.security.authentication.ImpersonationCallback;
import org.apache.jackrabbit.core.security.authentication.RepositoryCallback;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HippoLoginModule implements LoginModule {

    /** SVN id placeholder */

    // initial state
    private Subject subject;
    private CallbackHandler callbackHandler;

    // configurable JAAS options
    @SuppressWarnings("unused")
    private final boolean debug = false;

    private boolean validLogin;

    // local authentication state:
    // the principals, i.e. the authenticated identities
    private final Set<Principal> principals = new HashSet<Principal>();

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    //----------------------------------------------------------< LoginModule >
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
            Map<String, ?> options) {

        // set state
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        validLogin = false;
    }

    public boolean login() throws LoginException {
        if (callbackHandler == null) {
            throw new LoginException("no CallbackHandler available");
        }

        try {
            // Get credentials using a JAAS callback
            CredentialsCallback ccb = new CredentialsCallback();
            RepositoryCallback repositoryCallback = new RepositoryCallback();
            callbackHandler.handle(new Callback[] { ccb, repositoryCallback });
            SimpleCredentials creds = (SimpleCredentials) ccb.getCredentials();
            final String userId = (creds == null) ? "" : creds.getUserID();
            HippoSecurityManager securityManager = ((HippoSecurityManager)((RepositoryImpl)repositoryCallback.getSession().getRepository()).getSecurityManager());

            ImpersonationCallback impersonationCallback = new ImpersonationCallback();
            try {
                callbackHandler.handle(new Callback[] { impersonationCallback });
            } catch(UnsupportedCallbackException ignored) {
            }

            // check for impersonation
            Subject impersonator = impersonationCallback.getImpersonator();
            if (impersonator != null) {
                // anonymous cannot impersonate
                if (!impersonator.getPrincipals(AnonymousPrincipal.class).isEmpty()) {
                    log.info("Denied Anymous impersonating as {}", userId);
                    return false;
                }

                // system session impersonate
                if (!impersonator.getPrincipals(SystemPrincipal.class).isEmpty()) {
                    log.debug("SystemSession impersonating to new SystemSession");
                    principals.add(new SystemPrincipal());
                    return (validLogin = true);
                }

                // check for valid user
                if (impersonator.getPrincipals(UserPrincipal.class).isEmpty()) {
                    log.info("Denied unknown user impersonating as {}", userId);
                    return false;
                }

                Principal iup = impersonator.getPrincipals(UserPrincipal.class).iterator().next();
                String impersonarorId = iup.getName();
                // TODO: check somehow if the user is allowed to impersonate

                log.info("Impersonating as {} by {}", userId, impersonarorId);
                securityManager.assignPrincipals(principals, creds);

                return (validLogin = true);
            }

            // check for anonymous login
            if (creds == null || creds.getUserID() == null) {
                log.debug("Authenticated as Anonymous user.");
                securityManager.assignPrincipals(principals, creds);
                return (validLogin = true);
            }

            // basic security check
            if ("".equals(userId) || creds.getPassword() == null || creds.getPassword().length == 0) {
                log.debug("Empty username or password not allowed.");
                return false;
            }

            log.debug("Trying to authenticate as {}", userId);
            final AuthenticationStatus authenticationStatus = securityManager.authenticate(creds);
            if (authenticationStatus == AuthenticationStatus.SUCCEEDED) {
                log.info("Authenticated as {}", userId);
                securityManager.assignPrincipals(principals, creds);
                validLogin = true;
            } else {
                if (authenticationStatus == AuthenticationStatus.FAILED) {
                    log.info("NOT Authenticated as {}", userId);
                }

                principals.clear();
                UnsuccessfulAuthenticationHandler.handle(authenticationStatus, userId);
            }
        } catch (ClassCastException e) {
            log.error("Error during login", e);
            throw new LoginException(e.getMessage());
        } catch (RepositoryException e) {
            log.error("Error during login", e);
            throw new LoginException(e.getMessage());
        } catch (java.io.IOException e) {
            log.error("Error during login", e);
            throw new LoginException(e.getMessage());
        } catch (UnsupportedCallbackException e) {
            log.error("Error during login", e);
            throw new LoginException(e.getCallback().toString() + " not available");
        }
        return validLogin;
    }

    public boolean commit() throws LoginException {
        // add a principals (authenticated identities) to the Subject
        subject.getPrincipals().addAll(principals);
        return validLogin;
    }

    public boolean abort() throws LoginException {
        logout();
        return validLogin;
    }

    public boolean logout() throws LoginException {
        subject.getPrincipals().removeAll(principals);
        principals.clear();
        return validLogin;
    }

}
