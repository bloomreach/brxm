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

import java.util.Calendar;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;

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
import org.quartz.Trigger;
import org.quartz.TriggerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final String CONFIG_CRONEXPRESSION_PROPERTY = "cronexpression";
    private static final String CONFIG_LOCK_ISDEEP_PROPERTY = "jcr:lockIsDeep";
    private static final String CONFIG_LOCK_OWNER = "jcr:lockOwner";


    private Session session;
    private AuthorFieldHandler listener;

    private RepositoryJobInfo jobInfo;

    private String cronExpression;
    public static final String SCHEDULER_NAME = "BlogListenerModule";
    public static final String SCHEDULER_GROUP_NAME = "essentials";

    @Override
    protected void doConfigure(final Node moduleConfig) throws RepositoryException {
        cronExpression = JcrUtils.getStringProperty(moduleConfig, CONFIG_CRONEXPRESSION_PROPERTY, null);
        String projectNamespace = JcrUtils.getStringProperty(moduleConfig, "projectNamespace", null);
        if (!Strings.isNullOrEmpty(projectNamespace)) {
            listener = new AuthorFieldHandler(projectNamespace);
        } else {
            log.warn("No projectNamespacePath configured in [org.onehippo.cms7.essentials.components.cms.modules.EventBusListenerModule]");
        }
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
        if (Strings.isNullOrEmpty(cronExpression)) {
            return;
        }

        final RepositoryScheduler scheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);

        final String jobName = SCHEDULER_NAME + "Job";
        if (scheduler.checkExists(jobName, SCHEDULER_GROUP_NAME)) {
            return;
        }

        jobInfo = new RepositoryJobInfo(jobName, BlogImporterJob.class);
        jobInfo.setAttribute(CONFIG_MODULECONFIGPATH, moduleConfigPath);

        final RepositoryJobTrigger trigger = new RepositoryJobCronTrigger(SCHEDULER_NAME + "Trigger", cronExpression);
        scheduler.scheduleJob(jobInfo, trigger);
    }

    private void unScheduleJob() throws RepositoryException {
        if (jobInfo != null) {
            final RepositoryScheduler repositoryScheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);
            repositoryScheduler.deleteJob(jobInfo.getName(), jobInfo.getGroup());
            jobInfo = null;
        }
    }


    private Trigger createRunNowTrigger(final RepositoryJobInfo jobInfo) {

        Trigger trigger = TriggerUtils.makeImmediateTrigger(0, 0);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.SECOND, 5);
        trigger.setStartTime(calendar.getTime());

        trigger.setName(jobInfo.getName());
        trigger.setGroup(jobInfo.getGroup());
        return trigger;

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

    }

}
