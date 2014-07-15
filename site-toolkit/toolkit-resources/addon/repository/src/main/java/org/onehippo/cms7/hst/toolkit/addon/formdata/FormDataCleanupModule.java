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
import javax.jcr.Value;
import javax.jcr.observation.Event;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.cluster.RepositoryClusterService;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.onehippo.repository.modules.RequiresService;
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
@RequiresService( types = { RepositoryScheduler.class } )
public class FormDataCleanupModule extends AbstractReconfigurableDaemonModule {

    private static final Logger log = LoggerFactory.getLogger(FormDataCleanupModule.class);

    private static final String CONFIG_MODULECONFIGPATH = "moduleconfigpath";
    private static final String CONFIG_CRONEXPRESSION_PROPERTY = "cronexpression";
    private static final String CONFIG_MINUTES_TO_LIVE_PROPERTY = "minutestolive";
    private static final String CONFIG_BATCH_SIZE = "batchsize";
    private static final String CONFIG_EXCLUDE_PATHS = "excludepaths";
    private static final String CONFIG_LOCK_ISDEEP_PROPERTY = "jcr:lockIsDeep";
    private static final String CONFIG_LOCK_OWNER = "jcr:lockOwner";

    private static String FORMDATA_QUERY = "SELECT * FROM hst:formdata ORDER BY hst:creationtime ASC";

    private String cronExpression;
    // 1 day
    private static final Long DEFAULT_MINUTES_TO_LIVE = 24 * 60L;
    private Long minutesToLive = DEFAULT_MINUTES_TO_LIVE;

    private static final Long DEFAULT_BATCH_SIZE = 100L;
    private Long batchSize = DEFAULT_BATCH_SIZE;

    private String excludePaths = "";
    private RepositoryJobInfo jobInfo;
    private String schedulerJobName = "FormDataCleanup";
    private String schedulerGroupName = "default";


    @SuppressWarnings("UnusedDeclaration")
    public FormDataCleanupModule() {
    }

    /* Test only */
    public FormDataCleanupModule(String moduleName, String moduleConfigPath, String cronExpression, Long minutesToLive,
                                 String excludePaths)
            throws RepositoryException {
        this.moduleName = moduleName;
        this.moduleConfigPath = moduleConfigPath;
        this.cronExpression = cronExpression;
        this.minutesToLive = minutesToLive;
        this.schedulerJobName += "-test";
        this.excludePaths = excludePaths;
        scheduleJob();
    }

    @Override
    protected void doConfigure(final Node moduleConfig) throws RepositoryException {
        cronExpression = JcrUtils.getStringProperty(moduleConfig, CONFIG_CRONEXPRESSION_PROPERTY, null);
        minutesToLive = JcrUtils.getLongProperty(moduleConfig, CONFIG_MINUTES_TO_LIVE_PROPERTY, DEFAULT_MINUTES_TO_LIVE);
        batchSize = JcrUtils.getLongProperty(moduleConfig, CONFIG_BATCH_SIZE, DEFAULT_BATCH_SIZE);
        if (moduleConfig.hasProperty(CONFIG_EXCLUDE_PATHS)) {
            StringBuilder buf = new StringBuilder();
            Value[] values = moduleConfig.getProperty(CONFIG_EXCLUDE_PATHS).getValues();
            for (int i = 0; i < values.length; i++) {
                buf.append(values[i].getString());
                buf.append('|');
            }
            excludePaths = buf.toString();
        }
        log.info("FormDataCleanupModule configuration : cronExpression = {}, minutesToLive = {}, excludePaths = {}, " +
                "batchSize = {}",
                cronExpression, String.valueOf(minutesToLive), excludePaths.toString(), String.valueOf(batchSize));
    }

    @Override
    protected void doInitialize(final Session session) throws RepositoryException {
        final RepositoryScheduler repositoryScheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);
        if (repositoryScheduler.checkExists(schedulerJobName, schedulerGroupName)) {
            return;
        }
        scheduleJob();
    }

    @Override
    protected void doShutdown() {
    }

    @Override
    protected boolean isReconfigureEvent(Event event) throws RepositoryException {
        final RepositoryClusterService repositoryClusterService = HippoServiceRegistry.getService(RepositoryClusterService.class);
        if (repositoryClusterService.isExternalEvent(event)) {
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
        if (cronExpression == null || minutesToLive == -1) {
            return;
        }
        final RepositoryScheduler repositoryScheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);
        jobInfo = new RepositoryJobInfo(schedulerJobName, schedulerGroupName, FormDataCleanupJob.class);
        jobInfo.setAttribute(CONFIG_MODULECONFIGPATH, moduleConfigPath);
        jobInfo.setAttribute(CONFIG_MINUTES_TO_LIVE_PROPERTY, String.valueOf(minutesToLive));
        jobInfo.setAttribute(CONFIG_BATCH_SIZE, String.valueOf(batchSize));
        jobInfo.setAttribute(CONFIG_EXCLUDE_PATHS, excludePaths);
        final RepositoryJobTrigger jobTrigger = new RepositoryJobCronTrigger(schedulerJobName + "Trigger", cronExpression);
        repositoryScheduler.scheduleJob(jobInfo, jobTrigger);
    }

    private void unscheduleJob() throws RepositoryException {
        final RepositoryScheduler repositoryScheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);
        repositoryScheduler.deleteJob(schedulerJobName, schedulerGroupName);
        jobInfo = null;
    }

    public static class FormDataCleanupJob implements RepositoryJob {

        @Override
        public void execute(final RepositoryJobExecutionContext context) throws RepositoryException {
            log.info("Running form data cleanup job");
            final Session session = context.createSession(new SimpleCredentials("system", new char[]{}));
            try {
                long minutesToLive = Long.parseLong(context.getAttribute(CONFIG_MINUTES_TO_LIVE_PROPERTY));
                long batchSize;
                try {
                    batchSize = Long.parseLong(context.getAttribute(CONFIG_BATCH_SIZE));
                } catch (NumberFormatException e) {
                    log.warn("Incorrect batch size '"+context.getAttribute(CONFIG_BATCH_SIZE)+"'. Setting default to 100");
                    batchSize = 100;
                }
                String[] excludePaths = context.getAttribute(CONFIG_EXCLUDE_PATHS).split("\\|");
                removeOldFormData(minutesToLive, batchSize, excludePaths, session);
            } finally {
                session.logout();
            }
        }

        private void removeOldFormData(long minutesToLive,
                                       final long batchSize,
                                       final String[] excludePaths,
                                       final Session session) throws RepositoryException {
            final QueryManager queryManager = session.getWorkspace().getQueryManager();
            final Query query = queryManager.createQuery(FORMDATA_QUERY, Query.SQL);
            final NodeIterator nodes = query.execute().getNodes();
            final long tooOldTimeStamp = System.currentTimeMillis() - minutesToLive * 60 * 1000L;
            int count = 0;
            outer:
            while (nodes.hasNext()) {
                try {
                    final Node node = nodes.nextNode();
                    if (node.getProperty("hst:creationtime").getDate().getTimeInMillis() > tooOldTimeStamp) {
                        break outer;
                    }
                    for (String path : excludePaths) {
                        if (!"".equals(path) && node.getPath().startsWith(path)) {
                            continue outer;
                        }
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Removing form data item at " + node.getPath());
                    }
                    remove(node, 2);
                    if (count++ % batchSize == 0) {
                        session.save();
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ignored) {
                        }
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

