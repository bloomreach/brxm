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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
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
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.security.UserPrincipal;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.api.PasswordHelper;
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

    // configurable Repository auth options
    private String anonymousId;
    private String systemuserId;
    private char[] systemuserPassword;
    private String usersNode;
    private long maxCacheTime;

    // local authentication state:
    // the principals, i.e. the authenticated identities
    private final Set principals = new HashSet();

    private static Map<String, char[]> userCache = new HashMap<String, char[]>();
    private static long lastCachePurge = 0L;

    // options
    private static final String OPT_ANONYMOUS_ID = "anonymousId";
    private static final String OPT_SYSTEMUSER_ID = "systemuserId";
    private static final String OPT_SYSTEMUSER_PASSWORD = "systemuserPassword";
    private static final String OPT_USERS_NODE = "usersPath";
    private static final String OPT_MAX_CACHE_TIME = "maxCacheTimeMilliSec";

    // defaults
    private static final String DEFAULT_ANONYMOUS_ID = "anonymous";
    private static final String DEFAULT_SYSTEMUSER_ID = "systemuser";
    private static final char[] DEFAULT_SYSTEMUSER_PASSWORD = "systempass".toCharArray();
    private static final String DEFAULT_USERS_NODE = "configuration/users";
    private static final long DEFAULT_MAX_CACHE_TIME = 10000L;

    /**
     * Get Logger 
     */
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Constructor
     */
    public RepositoryLoginModule() {
    }

    private boolean authenticate(String username, char[] password) {
        if (username == null || password == null || "".equals(username) || password.length == 0) {
            log.info("Empty username or password not allowed.");
            return false;
        }
        
        // try to use authentication cache
        try {
            synchronized (userCache) {
                long now = System.currentTimeMillis();
                if ((maxCacheTime == 0) || ((now - lastCachePurge) > maxCacheTime)) {
                    userCache.clear();
                    lastCachePurge = now;
                }

                if (log.isDebugEnabled()) {
                    log.debug("userCache.size() = " + userCache.size());
                    log.debug("Looking for user in cache: " + username);
                }

                if (userCache.containsKey(username)) {
                    if (log.isDebugEnabled()) {
                        log.debug("User found in cache: " + username);
                    }

                    boolean authenticated = false;
                    authenticated = PasswordHelper.checkHash(new String(password), new String(userCache.get(username)));
                    if (authenticated) {
                        return true;
                    }
                }
            }

            // look for user in the repository
            HippoRepository repository;
            repository = HippoRepositoryFactory.getHippoRepository();
            Session session = repository.login(new SimpleCredentials(systemuserId, systemuserPassword));
            Node root = session.getRootNode();

            if (log.isDebugEnabled()) {
                log.debug("Searching for user: " + username);
            }

            Node node;
            if (root.hasNode(usersNode + "/" + username)) {
                if (log.isDebugEnabled()) {
                    log.debug("Found user node: " + usersNode + "/" + username);
                }
                node = root.getNode(usersNode + "/" + username);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("User not found: " + username);
                }
                return false;
            }

            if (node.hasProperty("hippo:password")) {
                boolean authenticated = false;
                authenticated = PasswordHelper.checkHash(new String(password), node.getProperty("hippo:password").getValue().getString()); 
                if (authenticated) {
                    synchronized (userCache) {
                        userCache.put(username, password);
                    }
                    return true;
                }
            }

        } catch (RepositoryException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        return false;
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

        // set defaults
        this.anonymousId = DEFAULT_ANONYMOUS_ID;
        this.systemuserId = DEFAULT_SYSTEMUSER_ID;
        this.systemuserPassword = DEFAULT_SYSTEMUSER_PASSWORD;
        this.usersNode = DEFAULT_USERS_NODE;
        this.maxCacheTime = DEFAULT_MAX_CACHE_TIME;

        // fetch default JAAS parameters
        debug = "true".equalsIgnoreCase((String) options.get("debug"));
        tryFirstPass = "true".equalsIgnoreCase((String) options.get("tryFirstPass"));
        useFirstPass = "true".equalsIgnoreCase((String) options.get("useFirstPass"));
        storePass = "true".equalsIgnoreCase((String) options.get("storePass"));
        clearPass = "true".equalsIgnoreCase((String) options.get("clearPass"));

        // fetch repository auth paramaters
        String anonymous = (String) options.get(OPT_ANONYMOUS_ID);
        String id = (String) options.get(OPT_SYSTEMUSER_ID);
        String password = (String) options.get(OPT_SYSTEMUSER_PASSWORD);
        String node = (String) options.get(OPT_USERS_NODE);
        String cacheTime = (String) options.get(OPT_MAX_CACHE_TIME);

        if (anonymous != null) {
            this.anonymousId = anonymous;
        }
        if (id != null) {
            this.systemuserId = id;
        }
        if (password != null) {
            this.systemuserPassword = password.toCharArray();
        }
        if (node != null) {
            this.usersNode = node;
        }
        if (cacheTime != null) {
            try {
                this.maxCacheTime = Long.parseLong(cacheTime);
            } catch (NumberFormatException ex) {
                log.warn("Could not convert setting " + OPT_MAX_CACHE_TIME + " '" + cacheTime
                        + "' to a long, using default: " + DEFAULT_MAX_CACHE_TIME);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("RepositoryLoginModule config:");
            log.debug("* anonymousId    : " + anonymousId);
            log.debug("* systemuserId   : " + systemuserId);
            log.trace("* systemPassword : " + new String(systemuserPassword));
            log.debug("* usersNode      : " + usersNode);
            //log.debug("* tryFirstPass   : " + tryFirstPass);
            //log.debug("* useFirstPass   : " + useFirstPass);
            //log.debug("* storePass      : " + storePass);
            //log.debug("* clearPass      : " + clearPass);
            log.debug("* maxCacheTime   : " + maxCacheTime);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean login() throws LoginException {
        if (callbackHandler == null) {
            throw new LoginException("no CallbackHandler available");
        }

        boolean authenticated = false;
        String username = null;

        try {
            // Get credentials using a JAAS callback
            CredentialsCallback ccb = new CredentialsCallback();
            callbackHandler.handle(new Callback[] { ccb });
            Credentials creds = ccb.getCredentials();
            // Use the credentials to set up principals
            if (creds != null) {
                if (creds instanceof SimpleCredentials) {
                    SimpleCredentials sc = (SimpleCredentials) creds;
                    username = sc.getUserID();

                    if (systemuserId.equals(username)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Trying to authenticate as: systemuser (" + sc.getUserID() + ")");
                        }
                        if (new String(systemuserPassword).equals(new String(sc.getPassword()))) {
                            authenticated = true;
                            principals.add(new SystemPrincipal());
                            log.info("Authenticated as the systemuser");
                        }
                    } else if (username != null) {
                        if (debug) {
                            System.out.println("Trying to authenticate as: " + sc.getUserID());
                        }
                        if (authenticate(username, sc.getPassword())) {
                            authenticated = true;
                            principals.add(new UserPrincipal(username));
                            log.info("Authenticated as " + username);

                        }
                    }
                }
            }

            // check for anonymous login
            if (creds == null || username == null) {
                authenticated = true;
                log.info("Authenticated as anonymous.");
                principals.add(new AnonymousPrincipal());
            }
        } catch (java.io.IOException ioe) {
            throw new LoginException(ioe.toString());
        } catch (UnsupportedCallbackException uce) {
            throw new LoginException(uce.getCallback().toString() + " not available");
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
        subject.getPrincipals().removeAll(principals);
        principals.clear();
        return true;
    }

}
