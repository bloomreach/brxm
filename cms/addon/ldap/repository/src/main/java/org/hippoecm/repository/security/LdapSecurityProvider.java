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


import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.security.group.DummyGroupManager;
import org.hippoecm.repository.security.group.LdapGroupManager;
import org.hippoecm.repository.security.ldap.LdapContextFactory;
import org.hippoecm.repository.security.ldap.LdapUtils;
import org.hippoecm.repository.security.user.DummyUserManager;
import org.hippoecm.repository.security.user.LdapUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LdapSecurityProvider extends AbstractSecurityProvider {

    @SuppressWarnings("unused")
    final static String SVN_ID = "$Id$";

    private static long lastUpdate = 0;
    private static boolean syncRunning = false;
    private final static Object mutex = new Object();

    private static long cacheTime;
    private final static long DEFAULT_CACHE_TIME = 600 * 1000;

    //private Session session;
    private SecurityProviderContext context;
    private Node providerNode;
    private EventListener listener;

    // the nodetypes don't have to be exposed through the api because they are ldap specific
    final public static String NT_LDAPMAPPING = "hippoldap:mapping";
    final public static String NT_LDAPSEARCH = "hippoldap:search";
    final public static String NT_LDAPGROUPPROVIDER = "hippoldap:groupprovider";
    final public static String NT_LDAPROLEPROVIDER = "hippoldap:roleprovider";
    final public static String NT_LDAPUSERPROVIDER = "hippoldap:userprovider";
    final public static String NT_LDAPSECURITYPROVIDER = "hippoldap:securityprovider";
    
    // the properties don't have to be exposed through the api because they are ldap specific
    public final static String PROPERTY_PROVIDER_URL = "hippoldap:providerurl";
    public final static String PROPERTY_AUTHENTICATION = "hippoldap:authentication";
    public final static String PROPERTY_INITIAL_FACTORY = "hippoldap:initialfactory";
    public final static String PROPERTY_SOCKET_FACTORY = "hippoldap:socketfactory";
    public final static String PROPERTY_SEARCH_BASE = "hippoldap:searchbase";
    public final static String PROPERTY_PRINCIPAL = "hippoldap:principal";
    public final static String PROPERTY_CREDENTIALS = "hippoldap:credentials";
    public final static String PROPERTY_CACHE_MAX_AGE = "hippoldap:cachemaxage";
    public final static String PROPERTY_LDAP_DN = "hippoldap:dn";

    /**
     * Logger
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    //------------------------< Interface Impl >--------------------------//
    /**
     * {@inheritDoc}
     */
    public void init(SecurityProviderContext context) throws RepositoryException {
        this.context = context;

        Session session = context.getSession();

        providerNode = session.getRootNode().getNode(context.getSecurityPath() + "/" + context.getProviderId());

        log.info("Initializing security provider: '{}'.", context.getProviderId());

        /* Register a listener for the provider node.  Whenever a node
         * or property is added, refresh the security provider.
         */
        ObservationManager obMgr = session.getWorkspace().getObservationManager();
        listener = new EventListener() {
            public void onEvent(EventIterator events) {
                try {
                    reloadConfig();
                } catch (InvalidItemStateException e) {
                    log.debug("Invalid state while reloading config, provider probably removed: " + e.getMessage());
                } catch (RepositoryException e) {
                    log.warn("Failed to reload config for provider: {}", e.getMessage());
                }
            }
        };
        obMgr.addEventListener(listener, Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED,
                providerNode.getPath(), true, null, null, true);

        // initial config load
        reloadConfig();
    }

    @Override
    public void sync() {
        synchronized (mutex) {
            if (syncRunning) {
                return;
            }
            syncRunning = true;
        }
        cacheTime = DEFAULT_CACHE_TIME;
        try {
            if (providerNode.hasProperty(PROPERTY_CACHE_MAX_AGE)) {
                cacheTime = providerNode.getProperty(PROPERTY_CACHE_MAX_AGE).getLong() * 1000;
            }
        } catch (RepositoryException e) {
            log.info("No refresh time found using default of: {} ms.", cacheTime);
        }
        if ((System.currentTimeMillis() - lastUpdate) < cacheTime) {
            // keep using cache
            return;
        }
        log.debug("Start ldap sync");
        ((LdapUserManager) userManager).updateUsers();
        ((LdapGroupManager) groupManager).updateGroups();
        lastUpdate = System.currentTimeMillis();
        log.info("Ldap users and groups synced.");
        syncRunning = false;
    }
    
    @Override
    public void remove() {
        ObservationManager obMgr;
        try {
            // be on the defensive side
            if (context != null && context.getSession() != null && listener != null) {
                obMgr = context.getSession().getWorkspace().getObservationManager();
                obMgr.removeEventListener(listener);
            }
        } catch (RepositoryException e) {
            log.warn("Unable to remove listener: " + e.getMessage());
        }
    }

    //------------------------< Private methods >--------------------------//
    public void reloadConfig() throws RepositoryException {
        synchronized (mutex) {
            try {
                log.info("Reading config for security provider: '{}'.", context.getProviderId());

                // try login to test factory
                LdapContextFactory lcf = LdapUtils.createContextFactory(providerNode);
                LdapContext sysContext = lcf.getSystemLdapContext();
                LdapUtils.closeContext(sysContext);

                // login succeeded, create manager contexts
                LdapManagerContext userContext = new LdapManagerContext(lcf, context.getProviderNode(), context.getUsersPath());

                if (providerNode.hasNode(HippoNodeType.NT_USERPROVIDER)) {
                    userManager = new LdapUserManager();
                    userManager.init(userContext);
                } else {
                    userManager = new DummyUserManager();
                }

                LdapManagerContext groupContext = new LdapManagerContext(lcf, context.getProviderNode(), context.getGroupsPath());
                if (providerNode.hasNode(HippoNodeType.NT_GROUPPROVIDER)) {
                    groupManager = new LdapGroupManager();
                    groupManager.init(groupContext);
                } else {
                    groupManager = new DummyGroupManager();
                }
            } catch (NamingException e) {
                // wrap error
                throw new RepositoryException("Error while setting up ldap system context.", e);
            }
        }
    }

}
