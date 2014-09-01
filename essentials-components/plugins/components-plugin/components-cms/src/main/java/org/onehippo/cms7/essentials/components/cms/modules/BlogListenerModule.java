/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.observation.Event;

import org.apache.commons.lang.ArrayUtils;
import org.apache.jackrabbit.api.observation.JackrabbitEvent;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.essentials.components.cms.blog.BlogImporterJob;
import org.onehippo.cms7.essentials.components.cms.handlers.AuthorFieldHandler;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.onehippo.repository.modules.RequiresService;
import org.onehippo.repository.scheduling.RepositoryJobCronTrigger;
import org.onehippo.repository.scheduling.RepositoryJobInfo;
import org.onehippo.repository.scheduling.RepositoryJobTrigger;
import org.onehippo.repository.scheduling.RepositoryScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

/**
 * The EventBusListenerModule is a daemon module started by the CMS (based on the corresponding hippo:modules
 * repository configuration. It registers a HippoEventBus listener, which then dispatches events. Currently,
 * we're only interested in the "save" event for blogposts. The blog-post specific code can be found in the
 * BlogUpdater class.
 */
@RequiresService(types = {RepositoryScheduler.class})
public class BlogListenerModule extends AbstractReconfigurableDaemonModule {

    public static final Logger log = LoggerFactory.getLogger(BlogListenerModule.class);


    private final Object lock = new Object();
    private static final String CONFIG_MODULECONFIGPATH = "moduleconfigpath";
    private static final String CONFIG_CRONEXPRESSION_PROPERTY = "cronExpression";
    private static final String CONFIG_LOCK_ISDEEP_PROPERTY = "jcr:lockIsDeep";
    private static final String CONFIG_LOCK_OWNER = "jcr:lockOwner";


    private Session session;
    private AuthorFieldHandler listener;


    private String cronExpression;
    private Boolean active;
    private Boolean runNow;
    private String blogBasePath;
    private String authorsBasePath;
    private String projectNamespace;
    private String[] urls;
    private String[] authors;

    public static final String SCHEDULER_NAME = "BlogListenerModule";
    public static final String SCHEDULER_GROUP_NAME = "essentials";
    private static final String JOB_NAME = SCHEDULER_NAME + "Job";

    @Override
    protected void doConfigure(final Node moduleConfig) throws RepositoryException {
        cronExpression = JcrUtils.getStringProperty(moduleConfig, CONFIG_CRONEXPRESSION_PROPERTY, null);
        projectNamespace = JcrUtils.getStringProperty(moduleConfig, "projectNamespace", null);
        if (!Strings.isNullOrEmpty(projectNamespace)) {
            listener = new AuthorFieldHandler(projectNamespace);
        } else {
            log.warn("No projectNamespace configured in [org.onehippo.cms7.essentials.components.cms.modules.EventBusListenerModule]");
        }
        active = JcrUtils.getBooleanProperty(moduleConfig, "active", Boolean.FALSE);
        runNow = JcrUtils.getBooleanProperty(moduleConfig, "runInstantly", Boolean.FALSE);
        blogBasePath = JcrUtils.getStringProperty(moduleConfig, BlogImporterJob.BLOGS_BASE_PATH, null);
        authorsBasePath = JcrUtils.getStringProperty(moduleConfig, BlogImporterJob.AUTHORS_BASE_PATH, null);
        urls = readStrings(moduleConfig, BlogImporterJob.URLS);
        authors = readStrings(moduleConfig, BlogImporterJob.AUTHORS);
        if (authors.length != urls.length) {
            log.error("Authors and URL size mismatch, no blogs will be imported.");
            authors = ArrayUtils.EMPTY_STRING_ARRAY;
            urls = ArrayUtils.EMPTY_STRING_ARRAY;
        }
    }


    private String[] readStrings(final Node node, final String propertyName) {
        try {
            if (node.hasProperty(propertyName)) {
                final Property property = node.getProperty(propertyName);
                final List<String> retVal = new ArrayList<>();
                if (property.isMultiple()) {
                    final Value[] values = property.getValues();
                    for (Value value : values) {
                        final String myUrl = value.getString();
                        retVal.add(myUrl);
                    }
                }
                return retVal.toArray(new String[retVal.size()]);
            }

        } catch (RepositoryException e) {
            log.error("Error reading property", e);
        }
        return ArrayUtils.EMPTY_STRING_ARRAY;
    }

    @Override
    protected void doInitialize(final Session session) throws RepositoryException {
        this.session = session;
        final RepositoryScheduler repositoryScheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);
        if (repositoryScheduler.checkExists(SCHEDULER_NAME + "Job", SCHEDULER_GROUP_NAME)) {
            return;
        }
        scheduleJob();
    }


    private void scheduleJob() throws RepositoryException {
        if (!active || Strings.isNullOrEmpty(cronExpression) || Strings.isNullOrEmpty(projectNamespace) || urls.length == 0) {
            return;
        }

        final RepositoryScheduler scheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);


        if (scheduler.checkExists(JOB_NAME, SCHEDULER_GROUP_NAME)) {
            return;
        }

        final RepositoryJobInfo jobInfo = createJobInfo(JOB_NAME);


        final RepositoryJobTrigger trigger = new RepositoryJobCronTrigger(SCHEDULER_NAME + "Trigger", cronExpression);
        scheduler.scheduleJob(jobInfo, trigger);
        if (runNow) {

            final String name = SCHEDULER_NAME + "TriggerNow";
            final RepositoryJobInfo nowJobInfo = createJobInfo(name);
            final RepositoryJobTrigger runNowTrigger = new RepositoryJobCronTrigger(name, cronExpression);
            scheduler.scheduleJob(nowJobInfo, runNowTrigger);
            // execute and remove:
            scheduler.executeJob(name, nowJobInfo.getGroup());
            scheduler.deleteJob(name, nowJobInfo.getGroup());
        }
    }

    private RepositoryJobInfo createJobInfo(final String jobName) {
        final RepositoryJobInfo jobInfo = new RepositoryJobInfo(jobName, SCHEDULER_GROUP_NAME, BlogImporterJob.class);
        jobInfo.setAttribute(BlogImporterJob.PROJECT_NAMESPACE, projectNamespace);
        jobInfo.setAttribute(BlogImporterJob.AUTHORS_BASE_PATH, authorsBasePath);
        jobInfo.setAttribute(BlogImporterJob.BLOGS_BASE_PATH, blogBasePath);
        jobInfo.setAttribute(BlogImporterJob.AUTHORS, Joiner.on(BlogImporterJob.SPLITTER).join(authors));
        jobInfo.setAttribute(BlogImporterJob.URLS, Joiner.on(BlogImporterJob.SPLITTER).join(urls));
        return jobInfo;
    }

    private void unScheduleJob() throws RepositoryException {
        final RepositoryScheduler repositoryScheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);
        repositoryScheduler.deleteJob(JOB_NAME, SCHEDULER_GROUP_NAME);
    }


    //############################################
    //
    //############################################


    @Override
    protected boolean isReconfigureEvent(Event event) throws RepositoryException {
        String eventPath = event.getPath();
        return !((JackrabbitEvent) event).isExternal() && !eventPath.endsWith(CONFIG_LOCK_ISDEEP_PROPERTY) && !eventPath.endsWith(CONFIG_LOCK_OWNER);
    }

    @Override
    protected void onConfigurationChange(final Node moduleConfig) throws RepositoryException {
        try {
            synchronized (lock) {
                unScheduleJob();
                //noinspection NonPrivateFieldAccessedInSynchronizedContext
                doConfigure(session.getNode(moduleConfigPath));
                scheduleJob();
            }
        } catch (RepositoryException e) {
            log.error("Failed to reconfigure event log cleaner", e);
        }
    }

    @Override
    protected void doShutdown() {
        session.logout();
    }

}
