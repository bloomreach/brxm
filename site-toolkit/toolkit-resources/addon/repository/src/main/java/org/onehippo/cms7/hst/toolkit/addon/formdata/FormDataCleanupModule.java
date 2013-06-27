/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.hst.toolkit.addon.formdata;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.Event;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.jackrabbit.api.observation.JackrabbitEvent;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.onehippo.repository.scheduling.RepositoryJob;
import org.onehippo.repository.scheduling.RepositoryJobCronTrigger;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.onehippo.repository.scheduling.RepositoryJobInfo;
import org.onehippo.repository.scheduling.RepositoryJobTrigger;
import org.onehippo.repository.scheduling.RepositoryScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deletes old hst:formdata nodes. Configuration is at /hippo:configuration/hippo:modules/formdatacleanup/hippo:moduleconfig.
 * The following properties can be configured:
 * 'cronexpression' (String) a quartz cron expression specifying when to run
 * 'minutestolive' (Long) the maximum lifetime of a hst:formdata node in minutes, based on its hst:creationtime value
 */
public class FormDataCleanupModule extends AbstractReconfigurableDaemonModule {

    private static final Logger log = LoggerFactory.getLogger(FormDataCleanupModule.class);

    private static final String CONFIG_MODULECONFIGPATH = "moduleconfigpath";
    private static final String CONFIG_CRONEXPRESSION_PROPERTY = "cronexpression";
    private static final String CONFIG_MINUTES_TO_LIVE_PROPERTY = "minutestolive";
    private static final String CONFIG_LOCK_ISDEEP_PROPERTY = "jcr:lockIsDeep";
    private static final String CONFIG_LOCK_OWNER = "jcr:lockOwner";

    private static final String SCHEDULER_JOB_NAME = "FormDataCleanup";
    private static final String SCHEDULER_GROUP_NAME = "default";

    private static String FORMDATA_QUERY = "SELECT * FROM hst:formdata ORDER BY hst:creationtime ASC";

    private String cronExpression;
    private long minutesToLive;
    private RepositoryJobInfo jobInfo;


    @Override
    protected void doConfigure(final Node moduleConfig) throws RepositoryException {
        cronExpression = JcrUtils.getStringProperty(moduleConfig, CONFIG_CRONEXPRESSION_PROPERTY, null);
        minutesToLive = JcrUtils.getLongProperty(moduleConfig, CONFIG_MINUTES_TO_LIVE_PROPERTY, 30l);
    }

    @Override
    protected void doInitialize(final Session session) throws RepositoryException {
        final RepositoryScheduler repositoryScheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);
        if (repositoryScheduler.checkExists(SCHEDULER_JOB_NAME, SCHEDULER_GROUP_NAME)) {
            return;
        }
        scheduleJob();
    }

    @Override
    protected void doShutdown() {
    }

    @Override
    protected boolean isReconfigureEvent(Event event) throws RepositoryException {
        if (((JackrabbitEvent)event).isExternal()) {
            return false;
        }
        String eventPath = event.getPath();
        return !eventPath.endsWith(CONFIG_LOCK_ISDEEP_PROPERTY) && !eventPath.endsWith(CONFIG_LOCK_OWNER);
    }

    @Override
    protected void onConfigurationChange(final Node moduleConfig) throws RepositoryException {
        synchronized (FormDataCleanupModule.this) {
            try {
                unscheduleJob();
                configure(session.getNode(moduleConfigPath));
                scheduleJob();
            } catch (RepositoryException e) {
                log.error("Failed to reconfigure form data cleaner", e);
            }
        }
    }

    private void scheduleJob() throws RepositoryException {
        if (cronExpression == null) {
            return;
        }
        final RepositoryScheduler repositoryScheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);
        jobInfo = new RepositoryJobInfo(SCHEDULER_JOB_NAME, SCHEDULER_GROUP_NAME, FormDataCleanupJob.class);
        jobInfo.setAttribute(CONFIG_MODULECONFIGPATH, moduleConfigPath);
        jobInfo.setAttribute(CONFIG_MINUTES_TO_LIVE_PROPERTY, String.valueOf(minutesToLive));
        final RepositoryJobTrigger jobTrigger = new RepositoryJobCronTrigger(SCHEDULER_JOB_NAME + "Trigger", cronExpression);
        repositoryScheduler.scheduleJob(jobInfo, jobTrigger);
    }

    private void unscheduleJob() throws RepositoryException {
        final RepositoryScheduler repositoryScheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);
        repositoryScheduler.deleteJob(jobInfo.getName(), jobInfo.getGroup());
        jobInfo = null;
    }

    public static class FormDataCleanupJob implements RepositoryJob {

        @Override
        public void execute(final RepositoryJobExecutionContext context) throws RepositoryException {
            log.info("Running form data cleanup job");
            final Session session = context.getSession(new SimpleCredentials("system", new char[] {}));
            long minutesToLive = Long.parseLong(context.getAttribute(CONFIG_MINUTES_TO_LIVE_PROPERTY));
            removeOldFormData(minutesToLive, session);
        }

        private void removeOldFormData(long minutesToLive, final Session session) throws RepositoryException {
            final QueryManager queryManager = session.getWorkspace().getQueryManager();
            final Query query = queryManager.createQuery(FORMDATA_QUERY, Query.SQL);
            final NodeIterator nodes = query.execute().getNodes();
            final long tooOldTimeStamp = System.currentTimeMillis() - minutesToLive * 60 * 1000L;
            int count = 0;
            while (nodes.hasNext()) {
                try {
                    final Node node = nodes.nextNode();
                    if (node.getProperty("hst:creationtime").getDate().getTimeInMillis() > tooOldTimeStamp) {
                        break;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Removing form data item at " + node.getPath());
                    }
                    remove(node, 2);
                    if (count++ % 10 == 0) {
                        session.save();
                        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                    }
                } catch (RepositoryException e) {
                    log.error("Error while cleaning up form data", e);
                }
            }
            if (session.hasPendingChanges()) {
                session.save();
            }
            if (count > 0) {
                log.info("Done cleaning " + count + " items");
            } else {
                log.info("No timed out items");
            }
        }

        private void remove(final Node node, int ancestorsToRemove) throws RepositoryException {
            final Node parent = node.getParent();
            node.remove();
            if (ancestorsToRemove > 0 && parent != null && parent.getName().length() == 1 && parent.isNodeType("hst:formdatacontainer") && parent.getNodes().getSize() == 0) {
                remove(parent, ancestorsToRemove - 1);
            }
        }

    }

}

