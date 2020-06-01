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
package org.hippoecm.hst.security.impl;

import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.hippoecm.hst.security.AuthenticationProvider;
import org.hippoecm.hst.security.Role;
import org.hippoecm.hst.security.User;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DefaultLoginModule
 * @version $Id$
 */
public class DefaultLoginModule implements LoginModule {

    private static final Logger log = LoggerFactory.getLogger(DefaultLoginModule.class);

    /** <p>LoginModule debug mode is turned off by default.</p> */
    protected boolean debug;
    
    /** <p>LoginModule public credentials storing mode is turned off by default.</p> */
    protected boolean storePubCreds;
    
    /** <p>LoginModule private credentials storing mode is turned off by default.</p> */
    protected boolean storePrivCreds;
    
    /** <p>The authentication status.</p> */
    protected boolean success;

    /** <p>The commit status.</p> */
    protected boolean commitSuccess;

    /** <p>The Subject to be authenticated.</p> */
    protected Subject subject;

    /** <p>A CallbackHandler for communicating with the end user (prompting for usernames and passwords, for example).</p> */
    protected CallbackHandler callbackHandler;

    /** <p>State shared with other configured LoginModules.</p> */
    protected Map<String, ?> sharedState;

    /** <p>Options specified in the login Configuration for this particular LoginModule.</p> */
    protected Map<String, ?> options;

    /** <p>The authentication provider service.</p> */
    protected AuthenticationProvider authProvider;

    /** <p>The user name.</p> */
    protected String username;

    protected User user;

    /**
     * <p>The default login module constructor.</p>
     */
    public DefaultLoginModule() {
        this(null);
    }

    /**
     * Create a new login module that uses the given authentication provider
     * @param authProvider the authentication provider to use
     */
    protected DefaultLoginModule(AuthenticationProvider authProvider) {
        this.authProvider = authProvider;
        
        debug = false;
        storePubCreds = false;
        storePrivCreds = false;
        success = false;
        commitSuccess = false;
        username = null;
    }

    /**
     * @see javax.security.auth.spi.LoginModule#abort()
     */
    public boolean abort() throws LoginException {
        // Clean out state
        success = false;
        commitSuccess = false;
        username = null;

        if (callbackHandler instanceof PassiveCallbackHandler) {
            ((PassiveCallbackHandler) callbackHandler).clearPassword();
        }

        logout();

        return true;
    }

    /**
     * @see javax.security.auth.spi.LoginModule#commit()
     */
    public boolean commit() throws LoginException {
        if (success) {
            if (subject.isReadOnly()) {
                throw new LoginException("Subject is Readonly");
            }
            
            try {
                commitSubject(subject, user);
                
                username = null;
                user = null;
                commitSuccess = true;

                if (callbackHandler instanceof PassiveCallbackHandler) {
                    ((PassiveCallbackHandler) callbackHandler).clearPassword();
                }
            } catch (Exception ex) {
                if (log.isDebugEnabled()) {
                    log.warn("Failed to commit: " + ex, ex);
                } else {
                    log.warn("Failed to commit: " + ex);
                }
                throw new LoginException(ex.getMessage());
            }
        }

        return commitSuccess;
    }

    /**
     * @see javax.security.auth.spi.LoginModule#login()
     */
    public boolean login() throws LoginException {
        if (callbackHandler == null) {
            throw new LoginException("Error: no CallbackHandler available "
                    + "to garner authentication information from the user");
        }
        
        try {
            // Setup default callback handlers.
            Callback[] callbacks = new Callback[] { new NameCallback("Username: "),
                    new PasswordCallback("Password: ", false) };

            callbackHandler.handle(callbacks);

            username = ((NameCallback) callbacks[0]).getName();
            String password = new String(((PasswordCallback) callbacks[1]).getPassword());

            ((PasswordCallback) callbacks[1]).clearPassword();

            success = false;

            try {
                user = getAuthenticationProvider().authenticate(username, password.toCharArray());
                
                if (storePrivCreds) {
                    subject.getPrivateCredentials().add(createSubjectRepositoryCredentials(username, password.toCharArray()));
                }
            } catch (SecurityException se) {
                if (se.getCause() != null) {
                    if (log.isDebugEnabled()) {
                        log.info("Failed to authenticate: " + se.getCause(), se.getCause());
                    } else {
                        log.info("Failed to authenticate: " + se.getCause());
                    }
                } else {
                    log.info("Failed to authenticate: " + se);
                }
                
                throw new FailedLoginException("Authentication failed");
            }

            success = true;
            callbacks[0] = null;
            callbacks[1] = null;

            return (true);
        } catch (LoginException ex) {
            throw ex;
        } catch (Exception ex) {
            success = false;
            throw new LoginException(ex.getMessage());
        }
    }

    /**
     * @see javax.security.auth.spi.LoginModule#logout()
     */
    public boolean logout() throws LoginException {
        // TODO Can we set subject to null?
        user = null;
        subject.getPrincipals().clear();
        subject.getPrivateCredentials().clear();
        subject.getPublicCredentials().clear();
        success = false;
        commitSuccess = false;

        return true;
    }

    /**
     * @see javax.security.auth.spi.LoginModule#initialize(javax.security.auth.Subject, javax.security.auth.callback.CallbackHandler, java.util.Map, java.util.Map)
     */
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
            Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;

        if (options.containsKey("debug")) {
            debug = "true".equalsIgnoreCase((String) options.get("debug"));
        }
        
        if (options.containsKey("storePubCreds")) {
            storePrivCreds = "true".equalsIgnoreCase((String) options.get("storePrivCreds"));
        }
        
        if (options.containsKey("storePrivCreds")) {
            storePrivCreds = "true".equalsIgnoreCase((String) options.get("storePrivCreds"));
        }
    }

    /**
     * Default setup of the logged on Subject Principals for Tomcat
     * @param containerSubject
     * @param user
     */
    protected void commitSubject(Subject containerSubject, User user) {
        subject.getPrincipals().add(user);
        
        Set<Role> roles = authProvider.getRolesByUsername(user.getName());
        
        for (Role role : roles) {
            subject.getPrincipals().add(role);
        }
    }
    
    protected AuthenticationProvider getAuthenticationProvider() throws SecurityException {
        if (authProvider == null) {
            authProvider = HstServices.getComponentManager().getComponent(AuthenticationProvider.class.getName());
            
            if (authProvider == null) {
                throw new SecurityException("AuthenticationProvider is not found in HST-2 service component assembly.");
            }
        }
        
        return authProvider;
    }
    
    /**
     * Creates repository credentials for the authenticated user.
     * <P>
     * This method is invoked when the 'storedPrivCreds' option is true,
     * to store a repository credentials for the authenticated user.
     * By default, this method creates a repository credentials with the same user/password credentials
     * used during authentication.
     * </P>
     * <P>
     * A child class can override this method to behave differently.
     * </P>
     * @param username
     * @param password
     * @return
     */
    protected Credentials createSubjectRepositoryCredentials(String username, char [] password) {
        return new SimpleCredentials(username, password);
    }
}
