/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.components.cms.modules;

import javax.jcr.*;
import javax.jcr.observation.Event;

import org.apache.jackrabbit.api.observation.JackrabbitEvent;
import org.onehippo.cms7.essentials.components.cms.blog.BlogImporterHelper;
import org.onehippo.cms7.essentials.components.cms.handlers.AuthorFieldHandler;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.onehippo.repository.modules.RequiresService;
import org.onehippo.repository.scheduling.RepositoryScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * The BlogListenerModule is a daemon module started by the CMS (based on the corresponding hippo:modules
 * repository configuration). It has two main objectives:
 *
 *   1) register the AuthorFieldHandler with the HippoEventBus, such that the saving of blog post documents
 *      triggers the update of the blog post's author field.
 *   2) schedule (and reschedule upon reconfiguration) the blog importer job.
 */
@RequiresService(types = {RepositoryScheduler.class})
public class BlogListenerModule extends AbstractReconfigurableDaemonModule {

    public static final Logger log = LoggerFactory.getLogger(BlogListenerModule.class);

    private static final SimpleCredentials credentials = new SimpleCredentials("system", new char[]{});
    private static final Object lock = new Object();
    private static final String CONFIG_LOCK_ISDEEP_PROPERTY = "jcr:lockIsDeep";
    private static final String CONFIG_LOCK_OWNER = "jcr:lockOwner";

    private Session session;
    private AuthorFieldHandler listener;

    @Override
    protected void doConfigure(final Node moduleConfigNode) throws RepositoryException {
        // we read the configuration on the fly.
    }

    @Override
    protected void doInitialize(final Session session) throws RepositoryException {
        this.session = session;

        final Node moduleConfigNode = getModuleConfigNode(session);
        // Inline the Blog importer rescheduling?
        BlogImporterHelper.rescheduleJob(moduleConfigNode);

        // create and register listener (refactor into separate function)
        final String projectNamespace = BlogImporterHelper.getProjectNamespace(moduleConfigNode);
        if (!Strings.isNullOrEmpty(projectNamespace)) {
            listener = new AuthorFieldHandler(projectNamespace, session);
            HippoServiceRegistry.registerService(listener, HippoEventBus.class);
        } else {
            log.warn("No projectNamespace configured in [org.onehippo.cms7.essentials.components.cms.modules.EventBusListenerModule]");
        }
    }

    @Override
    protected boolean isReconfigureEvent(Event event) throws RepositoryException {
        String eventPath = event.getPath();
        return !((JackrabbitEvent) event).isExternal()
                && !eventPath.endsWith(CONFIG_LOCK_ISDEEP_PROPERTY)
                && !eventPath.endsWith(CONFIG_LOCK_OWNER);
    }

    /**
     * The BlogImporterSchedulingHelper writes to the repository when rescheduling jobs, which triggers a warning
     * when done from the "notification thread". To avoid this warning, we run the job rescheduling as a separate
     * thread.
     */
    @Override
    protected void onConfigurationChange(final Node moduleConfigNode) throws RepositoryException {
        final Session threadSession = session.impersonate(credentials);

        new Thread() {
            @Override
            public void run() {
                try {
                    synchronized (lock) {
                        BlogImporterHelper.rescheduleJob(getModuleConfigNode(threadSession));
                    }
                } catch (RepositoryException e) {
                    log.error("Failed to reconfigure event log cleaner", e);
                } finally {
                    threadSession.logout();
                }
            }
        }.start();
    }

    @Override
    protected void doShutdown() {
        if (listener != null) {
            HippoServiceRegistry.unregisterService(listener, HippoEventBus.class);
        }
        session.logout();
    }

    private Node getModuleConfigNode(final Session session) throws RepositoryException {
        return session.getNode(moduleConfigPath);
    }
}
