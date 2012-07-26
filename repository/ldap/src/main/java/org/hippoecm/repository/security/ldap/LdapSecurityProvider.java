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

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.security.AbstractSecurityProvider;
import org.hippoecm.repository.security.SecurityProviderContext;
import org.hippoecm.repository.security.group.DummyGroupManager;
import org.hippoecm.repository.security.user.DummyUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LdapSecurityProvider extends AbstractSecurityProvider {


    // the nodetypes don't have to be exposed through the api because they are ldap specific
    public static final String NT_LDAPMAPPING = "hippoldap:mapping";
    public static final String NT_LDAPUSERSEARCH = "hippoldap:usersearch";
    public static final String NT_LDAPGROUPSEARCH = "hippoldap:groupsearch";
    public static final String NT_LDAPGROUPPROVIDER = "hippoldap:groupprovider";
    public static final String NT_LDAPROLEPROVIDER = "hippoldap:roleprovider";
    public static final String NT_LDAPUSERPROVIDER = "hippoldap:userprovider";
    public static final String NT_LDAPSECURITYPROVIDER = "hippoldap:securityprovider";

    // the properties don't have to be exposed through the api because they are ldap specific
    public static final String PROPERTY_PROVIDER_URL = "hippoldap:providerurl";
    public static final String PROPERTY_AUTHENTICATION = "hippoldap:authentication";
    public static final String PROPERTY_INITIAL_FACTORY = "hippoldap:initialfactory";
    public static final String PROPERTY_SOCKET_FACTORY = "hippoldap:socketfactory";
    public static final String PROPERTY_CONNECT_TIMEOUT_MS = "hippoldap:connecttimeoutms";
    public static final String PROPERTY_SEARCH_BASE = "hippoldap:searchbase";
    public static final String PROPERTY_PRINCIPAL = "hippoldap:principal";
    public static final String PROPERTY_CREDENTIALS = "hippoldap:credentials";
    public static final String PROPERTY_CACHE_MAX_AGE = "hippoldap:cachemaxage";
    public static final String PROPERTY_LDAP_DN = "hippoldap:dn";

    // updates and sync caches
    private long lastUpdate = 0;
    private final Object mutex = new Object();
    private final long DEFAULT_CACHE_MAX_AGE = 600 * 1000;
    private long cacheMaxAge = DEFAULT_CACHE_MAX_AGE;

    //private Session session;
    private SecurityProviderContext context;
    private EventListener listener;
    private BackgroundSync backgroundSync = null;

    /**
     * Logger
     */
    private static final Logger log = LoggerFactory.getLogger(LdapSecurityProvider.class);

    /**
     * {@inheritDoc}
     */
    public void init(SecurityProviderContext context) throws RepositoryException {
        log.info("Initializing security provider: '{}'.", context.getProviderId());
        this.context = context;
        final String pId = context.getProviderId();
        
        
        /* Register a listener for the provider node.  Whenever a node
         * or property is added, refresh the security provider.
         */
        ObservationManager obMgr = context.getSession().getWorkspace().getObservationManager();
        listener = new EventListener() {
            public void onEvent(EventIterator events) {
                try {
                    reloadConfig();
                } catch (InvalidItemStateException e) {
                    log.debug("Invalid state while reloading config, provider {} probably removed: {}", pId, e.getMessage());
                } catch (RepositoryException e) {
                    log.error("Failed to reload config for provider: " + pId, e);
                }
            }
        };
        obMgr.addEventListener(listener, Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_ADDED
                | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED, "/" + context.getProviderPath(), true, null, null, true);

        // initial config load
        reloadConfig();
    }

    @Override
    public void sync() {
        long cacheAge = System.currentTimeMillis()- lastUpdate;
        if (cacheAge < cacheMaxAge) {
            // keep using cache
            log.debug("Time until cache refresh is {} ms for provider {}, cache max age {} ms.",
                    new Object[] { (cacheMaxAge - cacheAge), context.getProviderId(), cacheMaxAge });
            return;
        }

        synchronized (mutex) {
            if (backgroundSync == null) {
                backgroundSync = new BackgroundSync();
                backgroundSync.start();
                lastUpdate = System.currentTimeMillis();
            } else {
                if (!backgroundSync.isRunning()) {
                    // previous sync is done, start new one.
                    backgroundSync = new BackgroundSync();
                    backgroundSync.start();
                    lastUpdate = System.currentTimeMillis();
                } else {
                    log.debug("Ldap sync still running for provider: {}", context.getProviderId());
                }
            }
        }
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

    public synchronized void reloadConfig() throws RepositoryException {
        try {
            log.info("Reading config for security provider: '{}'.", context.getProviderId());

            context.getSession().refresh(false);
            Node providerNode = context.getSession().getRootNode().getNode(context.getProviderPath());

            // try login to test factory
            LdapContextFactory lcf = LdapUtils.createContextFactory(providerNode);
            LdapContext sysContext = lcf.getSystemLdapContext();
            LdapUtils.closeContext(sysContext);

            // login succeeded, create manager contexts

            if (providerNode.hasNode(HippoNodeType.NT_USERPROVIDER)) {
                LdapManagerContext userContext = new LdapManagerContext(lcf, context.getSession(), context
                        .getProviderPath(), context.getUsersPath());

                userManager = new LdapUserManager();
                ((LdapUserManager)userManager).init(userContext);
            } else {
                log.warn("No user manager found, using dummy manager");
                userManager = new DummyUserManager();
            }

            if (providerNode.hasNode(HippoNodeType.NT_GROUPPROVIDER)) {
                LdapManagerContext groupContext = new LdapManagerContext(lcf, context.getSession(), context
                        .getProviderPath(), context.getGroupsPath());

                groupManager = new LdapGroupManager();
                groupManager.init(groupContext);
            } else {
                groupManager = new DummyGroupManager();
                log.warn("No group manager found, using dummy manager");
            }

            cacheMaxAge = DEFAULT_CACHE_MAX_AGE;
            try {
                context.getSession().refresh(false);
                if (providerNode.hasProperty(PROPERTY_CACHE_MAX_AGE)) {
                    cacheMaxAge = providerNode.getProperty(PROPERTY_CACHE_MAX_AGE).getLong() * 1000;
                }
            } catch (RepositoryException e) {
                log.info("No refresh time found using default of: {} ms.", cacheMaxAge);
            }
        } catch (NamingException e) {
            // wrap error
            throw new RepositoryException("Error while setting up ldap system context.", e);
        }
    }

    private class BackgroundSync extends Thread {

        private boolean running = true;

        @Override
        public void run() {
            try {
                log.info("Start ldap sync for: {}", context.getProviderId());

                // create new separate session so saves don't interferer with other sessions
                Session syncSession = context.getSession().impersonate(new SimpleCredentials("system", new char[] {}));
                Node providerNode = syncSession.getRootNode().getNode(context.getProviderPath());

                LdapContextFactory lcf = LdapUtils.createContextFactory(providerNode);

                if (providerNode.hasNode(HippoNodeType.NT_USERPROVIDER)) {
                    LdapManagerContext userContext = new LdapManagerContext(lcf, syncSession,
                            context.getProviderPath(), context.getUsersPath());
                    LdapUserManager userMgr = new LdapUserManager();
                    userMgr.init(userContext);
                    userMgr.updateUsers();
                }

                if (providerNode.hasNode(HippoNodeType.NT_GROUPPROVIDER)) {
                    LdapManagerContext groupContext = new LdapManagerContext(lcf, syncSession, context
                            .getProviderPath(), context.getGroupsPath());
                    LdapGroupManager groupMgr = new LdapGroupManager();
                    groupMgr.init(groupContext);
                    groupMgr.updateGroups();
                }

                syncSession.save();
                syncSession.logout();

                log.info("Ldap users and groups synced for: {}", context.getProviderId());
            } catch (RepositoryException e) {
                log.error("Unable to sync users and groups for provider: " + context.getProviderId(), e);
            } finally {
                running = false;
            }
        }

        public boolean isRunning() {
            return running;
        }
    }

}
