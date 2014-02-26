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

import java.util.Date;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.lock.LockException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;

import org.apache.jackrabbit.api.observation.JackrabbitEvent;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.security.AbstractSecurityProvider;
import org.hippoecm.repository.security.SecurityProviderContext;
import org.hippoecm.repository.security.group.DummyGroupManager;
import org.hippoecm.repository.security.user.DummyUserManager;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.scheduling.RepositoryJob;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.onehippo.repository.scheduling.RepositoryJobInfo;
import org.onehippo.repository.scheduling.RepositoryJobSimpleTrigger;
import org.onehippo.repository.scheduling.RepositoryJobTrigger;
import org.onehippo.repository.scheduling.RepositoryScheduler;
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

    private static final long DEFAULT_CACHE_MAX_AGE = 600 * 1000;

    private static final String SCHEDULER_GROUP_NAME = "security";
    private static final String SCHEDULER_JOB_NAME = "ldap-sync";
    private static final String SCHEDULER_TRIGGER_NAME = "repeater";

    private static final String PROVIDER_ID_ATTRIBUTE = "provider-id";
    private static final String PROVIDER_PATH_ATTRIBUTE = "provider-path";
    private static final String USERS_PATH_ATTRIBUTE = "users-path";
    private static final String GROUPS_PATH_ATTRIBUTE = "groups-path";

    private SecurityProviderContext context;
    private EventListener listener;

    private static final Logger log = LoggerFactory.getLogger(LdapSecurityProvider.class);

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
                final JackrabbitEvent event = (JackrabbitEvent) events.nextEvent();
                try {
                    reloadConfig(!event.isExternal());
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
        reloadConfig(false);

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

    public synchronized void reloadConfig(boolean reschedule) throws RepositoryException {
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

            long cacheMaxAge = DEFAULT_CACHE_MAX_AGE;
            try {
                context.getSession().refresh(false);
                if (providerNode.hasProperty(PROPERTY_CACHE_MAX_AGE)) {
                    cacheMaxAge = providerNode.getProperty(PROPERTY_CACHE_MAX_AGE).getLong() * 1000;
                }
            } catch (RepositoryException e) {
                log.info("No refresh time found using default of: {} ms.", cacheMaxAge);
            }

            final RepositoryScheduler schedulerService = HippoServiceRegistry.getService(RepositoryScheduler.class);
            if (reschedule || !schedulerService.checkExists(SCHEDULER_JOB_NAME, SCHEDULER_GROUP_NAME)) {
                try {
                    schedulerService.deleteJob(SCHEDULER_JOB_NAME, SCHEDULER_GROUP_NAME);
                    RepositoryJobTrigger trigger = new RepositoryJobSimpleTrigger(SCHEDULER_TRIGGER_NAME, new Date(),
                            RepositoryJobSimpleTrigger.REPEAT_INDEFINITELY, cacheMaxAge);
                    RepositoryJobInfo jobInfo = new RepositoryJobInfo(SCHEDULER_JOB_NAME, SCHEDULER_GROUP_NAME, SyncJob.class);
                    jobInfo.setAttribute(PROVIDER_ID_ATTRIBUTE, context.getProviderId());
                    jobInfo.setAttribute(PROVIDER_PATH_ATTRIBUTE, context.getProviderPath());
                    jobInfo.setAttribute(USERS_PATH_ATTRIBUTE, context.getUsersPath());
                    jobInfo.setAttribute(GROUPS_PATH_ATTRIBUTE, context.getGroupsPath());
                    schedulerService.scheduleJob(jobInfo, trigger);
                } catch (LockException e) {
                    log.warn("Failed to reschedule ldap sync job: job is being executed");
                } catch (RepositoryException e) {
                    log.error("Failed to reschedule ldap sync job", e);
                }
            }

        } catch (NamingException e) {
            // wrap error
            throw new RepositoryException("Error while setting up ldap system context.", e);
        }
    }

    public static class SyncJob implements RepositoryJob {

        @Override
        public void execute(final RepositoryJobExecutionContext context) throws RepositoryException {

            final String providerId = context.getAttribute(PROVIDER_ID_ATTRIBUTE);
            final String providerPath = context.getAttribute(PROVIDER_PATH_ATTRIBUTE);
            final String usersPath = context.getAttribute(USERS_PATH_ATTRIBUTE);
            final String groupsPath = context.getAttribute(GROUPS_PATH_ATTRIBUTE);

            Session syncSession = null;
            try {

                log.info("Start ldap sync for: {}", providerId);

                syncSession = context.createSession(new SimpleCredentials("system", new char[] {}));
                final Node providerNode = syncSession.getRootNode().getNode(providerPath);
                final LdapContextFactory lcf = LdapUtils.createContextFactory(providerNode);

                if (providerNode.hasNode(HippoNodeType.NT_USERPROVIDER)) {
                    LdapManagerContext userContext = new LdapManagerContext(lcf, syncSession, providerPath, usersPath);
                    LdapUserManager userMgr = new LdapUserManager();
                    userMgr.init(userContext);
                    userMgr.updateUsers();
                }

                if (providerNode.hasNode(HippoNodeType.NT_GROUPPROVIDER)) {
                    LdapManagerContext groupContext = new LdapManagerContext(lcf, syncSession, providerPath, groupsPath);
                    LdapGroupManager groupMgr = new LdapGroupManager();
                    groupMgr.init(groupContext);
                    groupMgr.updateGroups();
                }

                syncSession.save();

                log.info("Ldap users and groups synced for: {}", providerId);
            } catch (RepositoryException e) {
                log.error("Unable to sync users and groups for provider: " + providerId, e);
            } finally {
                if (syncSession != null) {
                    syncSession.logout();
                }
            }
        }
    }

}
