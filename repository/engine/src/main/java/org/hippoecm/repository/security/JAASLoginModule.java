/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
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

public class JAASLoginModule implements LoginModule {

    protected Subject subject;
    protected CallbackHandler callbackHandler;
    protected Set<Principal> principals;
    protected boolean validLogin;
    protected boolean allowPreAuthorized = false;
    protected Map<String,?> sharedState;

    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        principals = new HashSet<Principal>();
        validLogin = false;
        this.sharedState = sharedState;
        allowPreAuthorized = options.containsKey("preAuthorized");
    }

    public boolean login() throws LoginException {
        if(callbackHandler == null) {
            throw new LoginException("No CallbackHandler set");
        }
        List<Callback> callbacks = new LinkedList<Callback>();
        ImpersonationCallback impersonationCallback = new ImpersonationCallback();
        String username = null;
        char[] password = null;
        boolean validCredentials = false;

        callbacks.clear();
        callbacks.add(impersonationCallback);
        try {
            callbackHandler.handle(callbacks.toArray(new Callback[callbacks.size()]));
            Subject impersonator = impersonationCallback.getImpersonator();
            if (impersonator != null) {
                if (!impersonator.getPrincipals(AnonymousPrincipal.class).isEmpty()) {
                    throw new LoginException("Anonymous user is not allowed to impersonate");
                } else if (!impersonator.getPrincipals(SystemPrincipal.class).isEmpty()) {
                    principals.add(new SystemPrincipal());
                    validLogin = true;
                    return true;
                } else {
                    if (impersonator.getPrincipals(UserPrincipal.class).isEmpty()) {
                        throw new LoginException("Valid user principals required for impersonation");
                    }
                    Principal iup = impersonator.getPrincipals(UserPrincipal.class).iterator().next();
                    // TODO: check somehow if the user is allowed to impersonate
                }
                validLogin = true;
            }
        } catch (UnsupportedCallbackException ignored) {
        } catch (IOException ignored) {
        }

        if (!validCredentials) {
            callbacks.clear();
            CredentialsCallback credentialsCallback = new CredentialsCallback();
            callbacks.add(credentialsCallback);
            try {
                callbackHandler.handle(callbacks.toArray(new Callback[callbacks.size()]));
                Credentials credentials = credentialsCallback.getCredentials();
                if (credentials instanceof SimpleCredentials) {
                    username = ((SimpleCredentials) credentials).getUserID();
                    password = ((SimpleCredentials) credentials).getPassword();
                    validCredentials = true;
                } else {
                    if(allowPreAuthorized) {
                        Object authorizedName = sharedState.get("javax.security.auth.login.name");
                        if (authorizedName instanceof String) {
                            validCredentials = true;
                            validLogin = true;
                            username = (String) authorizedName;
                        } else if(authorizedName instanceof Subject) {
                            validCredentials = true;
                            validLogin = true;
                            // FIXME: get username from subject
                        } else {
                            validLogin = false;
                        }
                    }
                }
            } catch (UnsupportedCallbackException ignored) {
            } catch (IOException ignored) {
            }
        }

        if (!validCredentials) {
            callbacks.clear();
            NameCallback nameCallback = new NameCallback("username");
            PasswordCallback passwordCallback = new PasswordCallback("password", false);
            callbacks.add(nameCallback);
            callbacks.add(passwordCallback);
            try {
                callbackHandler.handle(callbacks.toArray(new Callback[callbacks.size()]));
                username = nameCallback.getName();
                password = passwordCallback.getPassword();
                if(username != null) {
                    validCredentials = true;
                }
            } catch (UnsupportedCallbackException ignored) {
            } catch (IOException ignored) {
            }
        }

        callbacks.clear();
        RepositoryCallback repositoryCallback = new RepositoryCallback();
        callbacks.add(repositoryCallback);
        try {
            callbackHandler.handle(callbacks.toArray(new Callback[callbacks.size()]));
            HippoSecurityManager securityManager = null;
            try {
                if(repositoryCallback.getSession() != null) {
                    securityManager =  ((HippoSecurityManager) ((RepositoryImpl) repositoryCallback.getSession().getRepository()).getSecurityManager());
                }
            } catch(RepositoryException ignored) {
            }
            
            // SimpleCredentials use clone() on the password parameter, so set it to an empty char array if null.
            if (password == null) {
                password = new char[0];
            }
            if(!validLogin) {
                if (username != null) {
                    if (securityManager != null) {
                        final AuthenticationStatus authenticationStatus = securityManager.authenticate(new SimpleCredentials(username, password));
                        if (authenticationStatus != AuthenticationStatus.SUCCEEDED) {
                            UnsuccessfulAuthenticationHandler.handle(authenticationStatus, username);
                        }
                    } else {
                        throw new LoginException("unable to authenticate");
                    }
                }
                validLogin = !principals.isEmpty();
            }
            if(securityManager != null) {
                securityManager.assignPrincipals(principals, new SimpleCredentials(username, password));
                if (username == null) {
                    principals.add(new AnonymousPrincipal());
                } else {
                    principals.add(new UserPrincipal(username));
                }
            }
        } catch (UnsupportedCallbackException ignored) {
        } catch (IOException ignored) {
        }
        return validLogin;
    }

    public boolean commit() throws LoginException {
        subject.getPrincipals().addAll(principals);
        return validLogin;
    }

    public boolean abort() throws LoginException {
        subject.getPrincipals().removeAll(principals);
        principals.clear();
        return validLogin;
    }

    public boolean logout() throws LoginException {
        subject.getPrincipals().removeAll(principals);
        principals.clear();
        return validLogin;
    }
}
