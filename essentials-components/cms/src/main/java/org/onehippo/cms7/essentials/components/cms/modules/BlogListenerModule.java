/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

import com.google.common.base.Joiner;
import org.apache.jackrabbit.api.observation.JackrabbitEvent;
import org.onehippo.cms7.essentials.components.cms.blog.BlogImporterConfiguration;
import org.onehippo.cms7.essentials.components.cms.blog.BlogImporterJob;
import org.onehippo.cms7.essentials.components.cms.handlers.AuthorFieldHandler;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventListenerRegistry;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.onehippo.repository.modules.RequiresService;
import org.onehippo.repository.scheduling.RepositoryJobCronTrigger;
import org.onehippo.repository.scheduling.RepositoryJobInfo;
import org.onehippo.repository.scheduling.RepositoryJobTrigger;
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

    private static final String CONFIG_LOCK_ISDEEP_PROPERTY = "jcr:lockIsDeep";
    private static final String CONFIG_LOCK_OWNER = "jcr:lockOwner";
    private static final String SCHEDULER_NAME = "BlogListenerModule";
    private static final String SCHEDULER_GROUP_NAME = "essentials";
    private static final String JOB_NAME = SCHEDULER_NAME + "Job";

    private AuthorFieldHandler listener;

    @Override
    protected void doConfigure(final Node moduleConfigNode) throws RepositoryException {
        // we read the BlogImporterConfiguration on the fly.
    }

    @Override
    protected void doInitialize(final Session session) throws RepositoryException {
        final BlogImporterConfiguration config = new BlogImporterConfiguration(session.getNode(moduleConfigPath));

        rescheduleJob(config);
        registerListener(config);
    }

    @Override
    protected void doShutdown() {
        unregisterListener();
    }

    @Override
    protected boolean isReconfigureEvent(Event event) throws RepositoryException {
        String eventPath = event.getPath();
        return !((JackrabbitEvent) event).isExternal()
                && !eventPath.endsWith(CONFIG_LOCK_ISDEEP_PROPERTY)
                && !eventPath.endsWith(CONFIG_LOCK_OWNER);
    }

    /**
     * Rescheduling a job writes to the repository, which triggers a warning when done from the "notification thread".
     * To avoid this warning, we run the job rescheduling as a separate thread.
     */
    @Override
    protected void onConfigurationChange(final Node moduleConfigNode) throws RepositoryException {
        // Keep the reading of the configuration outside the thread, so we don't use
        // the JCR session (by means of the moduleConfigNode) in a different thread.
        final BlogImporterConfiguration config = new BlogImporterConfiguration(moduleConfigNode);

        new Thread() {
            @Override
            public void run() {
                rescheduleJob(config);
            }
        }.start();
    }

    private static synchronized void rescheduleJob(final BlogImporterConfiguration config) {
        final RepositoryScheduler scheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);

        try {
            if (scheduler.checkExists(JOB_NAME, SCHEDULER_GROUP_NAME)) {
                scheduler.deleteJob(JOB_NAME, SCHEDULER_GROUP_NAME);
            }

            if (!config.isActive()
                    || Strings.isNullOrEmpty(config.getCronExpression())
                    || Strings.isNullOrEmpty(config.getProjectNamespace())
                    || config.getUrls().isEmpty()) {

                return; // No need to reschedule, we're done.
            }

            final RepositoryJobTrigger trigger = new RepositoryJobCronTrigger(SCHEDULER_NAME + "Trigger", config.getCronExpression());
            final RepositoryJobInfo jobInfo = new RepositoryJobInfo(JOB_NAME, SCHEDULER_GROUP_NAME, BlogImporterJob.class);
            populateJobInfo(jobInfo, config);
            scheduler.scheduleJob(jobInfo, trigger);

            if (config.isRunNow()) {
                scheduler.executeJob(JOB_NAME, SCHEDULER_GROUP_NAME);
            }
        } catch (RepositoryException ex) {
            log.error("Failure (re)scheduling the blog importer.", ex);
        }
    }

    private static void populateJobInfo(final RepositoryJobInfo jobInfo, final BlogImporterConfiguration config) {
        jobInfo.setAttribute(BlogImporterJob.PROJECT_NAMESPACE, config.getProjectNamespace());
        jobInfo.setAttribute(BlogImporterJob.AUTHORS_BASE_PATH, config.getAuthorsBasePath());
        jobInfo.setAttribute(BlogImporterJob.BLOGS_BASE_PATH, config.getBlogBasePath());
        jobInfo.setAttribute(BlogImporterJob.AUTHORS, Joiner.on(BlogImporterJob.SPLITTER).join(config.getAuthors()));
        jobInfo.setAttribute(BlogImporterJob.URLS, Joiner.on(BlogImporterJob.SPLITTER).join(config.getUrls()));
    }

    private void registerListener(final BlogImporterConfiguration config) {
        if (!Strings.isNullOrEmpty(config.getProjectNamespace())) {
            listener = new AuthorFieldHandler(config.getProjectNamespace(), session);
            HippoEventListenerRegistry.get().register(listener);
        } else {
            log.warn("No projectNamespace configured in [org.onehippo.cms7.essentials.components.cms.modules.EventBusListenerModule]");
        }
    }

    private void unregisterListener() {
        if (listener != null) {
            HippoEventListenerRegistry.get().unregister(listener);
        }
    }
}
