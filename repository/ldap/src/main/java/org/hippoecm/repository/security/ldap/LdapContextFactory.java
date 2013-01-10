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
package org.hippoecm.repository.security.ldap;

import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Default implementation of {@link LdapContextFactory} that can be configured or extended to
 * customize the way {@link javax.naming.ldap.LdapContext} objects are retrieved.</p>
 *
 * <p>This implementation of {@link LdapContextFactory} is used by the {@link AbstractLdapRealm} if a
 * factory is not explicitly configured.</p>
 *
 * <p>Connection pooling is enabled by default on this factory, but can be disabled using the
 * {@link #usePooling} property.</p>
 *
 */
public class LdapContextFactory {


    /**
     * The Sun LDAP property used to enable connection pooling.  This is used in the default implementation
     * to enable LDAP connection pooling.
     */
    protected static final String SUN_CONNECTION_POOLING_PROPERTY = "com.sun.jndi.ldap.connect.pool";
    protected static final String SUN_CONNECTION_TIMEOUT_PROPERTY = "com.sun.jndi.ldap.connect.timeout";

    private static final Logger log = LoggerFactory.getLogger(LdapContextFactory.class);

    private String authentication = "simple";

    private String principalSuffix = null;

    private String searchBase = null;

    private String contextFactoryClassName = "com.sun.jndi.ldap.LdapCtxFactory";

    private String url = null;

    private String referral = "follow";

    private String systemUsername = null;

    private char[] systemPassword = null;

    private boolean usePooling = true;

    private String connectTimeoutMs = "1000";

    private Map<String, String> additionalEnvironment;

    /**
     * Sets the type of LDAP authentication to perform when connecting to the LDAP server.  Defaults to "simple"
     *
     * @param authentication the type of LDAP authentication to perform.
     */
    public void setAuthentication(String authentication) {
        this.authentication = authentication;
    }

    /**
     * A suffix appended to the username. This is typically for
     * domain names.  (e.g. "@MyDomain.local")
     *
     * @param principalSuffix the suffix.
     */
    public void setPrincipalSuffix(String principalSuffix) {
        this.principalSuffix = principalSuffix;
    }

    /**
     * The search base for the search to perform in the LDAP server.
     * (e.g. OU=OrganizationName,DC=MyDomain,DC=local )
     *
     * @param searchBase the search base.
     */
    public void setSearchBase(String searchBase) {
        this.searchBase = searchBase;
    }
    
    /**
     * The search base for the search to perform in the LDAP server.
     * (e.g. OU=OrganizationName,DC=MyDomain,DC=local)
     *
     * @return searchBase the search base.
     */
    public String getSearchBase() {
        return searchBase;
    }
    
    /**
     * The context factory to use. This defaults to the SUN LDAP JNDI implementation
     * but can be overridden to use custom LDAP factories.
     *
     * @param contextFactoryClassName the context factory that should be used.
     */
    public void setContextFactoryClassName(String contextFactoryClassName) {
        this.contextFactoryClassName = contextFactoryClassName;
    }

    /**
     * The LDAP url to connect to. (e.g. ldap://<ldapDirectoryHostname>:<port>)
     *
     * @param url the LDAP url.
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Sets the LDAP referral property.  Defaults to "follow"
     *
     * @param referral the referral property.
     */
    public void setReferral(String referral) {
        this.referral = referral;
    }

    /**
     * Set the connection timeout in ms (Sun impl only)
     * @param timeoutMs
     */
    public void setConnectionTimeoutMs(String timeout) {
        this.connectTimeoutMs = timeout;
    }
    /**
     * The system username that will be used when connecting to the LDAP server to retrieve authorization
     * information about a user.  This must be specified for LDAP authorization to work, but is not required for
     * only authentication.
     *
     * @param systemUsername the username to use when logging into the LDAP server for authorization.
     */
    public void setSystemUsername(String systemUsername) {
        this.systemUsername = systemUsername;
    }

    /**
     * The system password that will be used when connecting to the LDAP server to retrieve authorization
     * information about a user.  This must be specified for LDAP authorization to work, but is not required for
     * only authentication.
     *
     * @param systemPassword the password to use when logging into the LDAP server for authorization.
     */
    public void setSystemPassword(char[] systemPassword) {
        this.systemPassword = systemPassword;
    }

    /**
     * Determines whether or not LdapContext pooling is enabled for connections made using the system
     * user account.  In the default implementation, this simply
     * sets the <tt>com.sun.jndi.ldap.connect.pool</tt> property in the LDAP context environment.  If you use an
     * LDAP Context Factory that is not Sun's default implementation, you will need to override the
     * default behavior to use this setting in whatever way your underlying LDAP ContextFactory
     * supports.  By default, pooling is enabled.
     *
     * @param usePooling true to enable pooling, or false to disable it.
     */
    public void setUsePooling(boolean usePooling) {
        this.usePooling = usePooling;
    }

    /**
     * These entries are added to the environment map before initializing the LDAP context.
     *
     * @param additionalEnvironment additional environment entries to be configured on the LDAP context.
     */
    public void setAdditionalEnvironment(Map<String, String> additionalEnvironment) {
        this.additionalEnvironment = additionalEnvironment;
    }

    public LdapContext getSystemLdapContext() throws NamingException {
        return getLdapContext(systemUsername, systemPassword);
    }

    public LdapContext getLdapContext(String username, char[] password) throws NamingException {
        if (searchBase == null) {
            throw new IllegalStateException("A search base must be specified.");
        }
        if (url == null) {
            throw new IllegalStateException("An LDAP URL must be specified of the form ldap[s]://<hostname>:<port>");
        }

        if (username != null && principalSuffix != null) {
            username += principalSuffix;
        }

        Hashtable<String, String> env = new Hashtable<String, String>();

        env.put(Context.SECURITY_AUTHENTICATION, authentication);
        if (username != null) {
            env.put(Context.SECURITY_PRINCIPAL, username);
        }
        if (password != null) {
            env.put(Context.SECURITY_CREDENTIALS, new String(password));
        }
        env.put(Context.INITIAL_CONTEXT_FACTORY, contextFactoryClassName);
        env.put(Context.PROVIDER_URL, url);
        env.put(Context.REFERRAL, referral);
        env.put(SUN_CONNECTION_TIMEOUT_PROPERTY, connectTimeoutMs);

        // Only pool connections for system contexts
        if (usePooling && username != null && username.equals(systemUsername)) {
            // Enable connection pooling
            env.put(SUN_CONNECTION_POOLING_PROPERTY, "true");
            log.debug("Initializing LDAP context with pooling [enabled]");
        } else {
            log.debug("Initializing LDAP context with pooling [disabled]");
        }

        if (additionalEnvironment != null) {
            env.putAll(additionalEnvironment);
        }

        if (log.isDebugEnabled()) {
            log.debug("Initializing LDAP context using URL [" + url + "] and username [" + username + "]");
        }

        return new InitialLdapContext(env, null);
    }

    /**
     * Convenience method to user in finally blocks
     * @param ctx
     */
    public void close(LdapContext ctx) {
        if (ctx != null) {
            try {
                ctx.close();
            } catch (NamingException e) {
                log.warn("Exception while closing ldap context", e);
            }
        }
    }
}
