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
package org.hippoecm.repository.security.ldap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.ldap.LdapContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LdapUtils {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private final static Logger log = LoggerFactory.getLogger(LdapUtils.class);

    /**
     * Private constructor to prevent instantiation
     */
    private LdapUtils() {
    }

    /**
     * Create LdapContextFactory from configuration path in repository
     * @param session the jcr session which must be able to read the config
     * @param configPath the jcr path containing the ldap configuration
     * @return LdapContextFactory or null when the creation has failed
     */
    static public LdapContextFactory createContextFactory(Node configNode) throws RepositoryException {

        LdapContextFactory lcf = new LdapContextFactory();
        // mandatory properties
        lcf.setUrl(configNode.getProperty(LdapSecurityProvider.PROPERTY_PROVIDER_URL).getString());
        lcf.setAuthentication(configNode.getProperty(LdapSecurityProvider.PROPERTY_AUTHENTICATION).getString());
        lcf.setContextFactoryClassName(configNode.getProperty(LdapSecurityProvider.PROPERTY_INITIAL_FACTORY).getString());

        // optional properties
        if (configNode.hasProperty(LdapSecurityProvider.PROPERTY_CONNECT_TIMEOUT_MS)) {
            lcf.setConnectionTimeoutMs(configNode.getProperty(LdapSecurityProvider.PROPERTY_CONNECT_TIMEOUT_MS).getString());
        }
        if (configNode.hasProperty(LdapSecurityProvider.PROPERTY_SEARCH_BASE)) {
            lcf.setSearchBase(configNode.getProperty(LdapSecurityProvider.PROPERTY_SEARCH_BASE).getString());
        }
        if (configNode.hasProperty(LdapSecurityProvider.PROPERTY_PRINCIPAL)) {
            lcf.setSystemUsername(configNode.getProperty(LdapSecurityProvider.PROPERTY_PRINCIPAL).getString());
        }
        if (configNode.hasProperty(LdapSecurityProvider.PROPERTY_CREDENTIALS)) {
            lcf.setSystemPassword(configNode.getProperty(LdapSecurityProvider.PROPERTY_CREDENTIALS).getString()
                    .toCharArray());
        }
        if (configNode.hasProperty(LdapSecurityProvider.PROPERTY_SOCKET_FACTORY)) {
            Map<String, String> env = new HashMap<String, String>();
            env.put("java.naming.ldap.factory.socket", configNode.getProperty(
                    LdapSecurityProvider.PROPERTY_SOCKET_FACTORY).getString());
            lcf.setAdditionalEnvironment(env);
        }
        return lcf;
    }

    /**
     * Closes an LDAP context, logging any errors, but not throwing
     * an exception if there is a failure.
     *
     * @param ctx the LDAP context to close.
     */
    public static void closeContext(LdapContext ctx) {
        try {
            if (ctx != null) {
                ctx.close();
            }
        } catch (NamingException e) {
            log.error("Exception while closing LDAP context. ", e);
        }
    }

    /**
     * Helper method used to retrieve all attribute values from a particular context attribute.
     *
     * @param attr the LDAP attribute.
     * @return the values of the attribute.
     * @throws javax.naming.NamingException if there is an LDAP error while reading the values.
     */
    public static List<String> getAllAttributeValues(Attribute attr) throws NamingException {
        List<String> values = new ArrayList<String>();
        if (attr == null) {
            return values;
        }
        for (NamingEnumeration<?> e = attr.getAll(); e.hasMoreElements();) {
            Object o = e.nextElement();
            if (o instanceof String) {
                String value = (String) o;
                values.add(value);
            }
        }
        return values;
    }

}
