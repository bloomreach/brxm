/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.logging;

import java.text.ParseException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.Event;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.jackrabbit.api.observation.JackrabbitEvent;
import org.hippoecm.repository.api.HippoNodeIterator;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.onehippo.repository.modules.RequiresService;
import org.onehippo.repository.scheduling.RepositoryJob;
import org.onehippo.repository.scheduling.RepositoryJobCronTrigger;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.onehippo.repository.scheduling.RepositoryJobInfo;
import org.onehippo.repository.scheduling.RepositoryJobTrigger;
import org.onehippo.repository.scheduling.RepositoryScheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cleans up hippo event log entries. The module can be configured at "/hippo:configuration/hippo:modules/eventlogcleanup/hippo:moduleconfig"
 * with the following options:<br/>
 * - property 'cronexpression' (String) a quartz cron expression specifying when to run the clean up job.
 * - property 'maxitems' (Long) the maximum number of items to keep in the logs. Defaults to -1, which means no maximum.
 * - property 'keepitemsfor' (Long) the number of milliseconds to keep items in the logs.
 */
@RequiresService(types = {RepositoryScheduler.class})
public class EventLogCleanupModule extends AbstractReconfigurableDaemonModule {

    private static final Logger log = LoggerFactory.getLogger(EventLogCleanupModule.class);
    private static final String CONFIG_MODULECONFIGPATH = "moduleconfigpath";
    private static final String CONFIG_MAXITEMS_PROPERTY = "maxitems";
    private static final String CONFIG_KEEP_ITEMS_FOR = "keepitemsfor";
    private static final String CONFIG_CRONEXPRESSION_PROPERTY = "cronexpression";
    private static final String CONFIG_LOCK_ISDEEP_PROPERTY = "jcr:lockIsDeep";
    private static final String CONFIG_LOCK_OWNER = "jcr:lockOwner";

    private static final String ITEMS_QUERY_MAX_ITEMS = "SELECT * FROM hippolog:item ORDER BY hippolog:timestamp ASC";
    private static final String ITEMS_QUERY_ITEM_TIMEOUT = "SELECT * FROM hippolog:item ORDER BY hippolog:timestamp ASC";

    private String cronExpression = null;
    private long maxitems = -1;
    private long itemtimeout = -1;
    private String quartzNamePostfix = "";

    public EventLogCleanupModule() {
    }

    /* Constructor for testing. */
    EventLogCleanupModule(String moduleConfigPath, String cronExpression, long maxitems, long itemstimeout, Session session, String testName) throws RepositoryException, SchedulerException, ParseException {
        this.moduleConfigPath = moduleConfigPath;
        this.cronExpression = cronExpression;
        this.maxitems = maxitems;
        this.itemtimeout = itemstimeout;
        this.session = session;
        quartzNamePostfix = testName;
        scheduleJob();
    }

    @Override
    protected void doConfigure(final Node moduleConfig) throws RepositoryException {
        cronExpression = JcrUtils.getStringProperty(moduleConfig, CONFIG_CRONEXPRESSION_PROPERTY, null);
        maxitems = JcrUtils.getLongProperty(moduleConfig, CONFIG_MAXITEMS_PROPERTY, -1l);
        itemtimeout = JcrUtils.getLongProperty(moduleConfig, CONFIG_KEEP_ITEMS_FOR, -1l);
    }

    @Override
    protected void doInitialize(final Session session) throws RepositoryException {
        scheduleJob();
    }

    @Override
    protected void doShutdown() {
    }

    @Override
    protected boolean isReconfigureEvent(Event event) throws RepositoryException {
        String eventPath = event.getPath();
        return !((JackrabbitEvent) event).isExternal() && !eventPath.endsWith(CONFIG_LOCK_ISDEEP_PROPERTY) && !eventPath.endsWith(CONFIG_LOCK_OWNER);
    }

    @Override
    protected void onConfigurationChange(final Node moduleConfig) throws RepositoryException {
        try {
            synchronized (this) {
                unscheduleJob();
                doConfigure(session.getNode(moduleConfigPath));
                scheduleJob();
            }
        } catch (RepositoryException e) {
            log.error("Failed to reconfigure event log cleaner", e);
        }
    }

    private void scheduleJob() throws RepositoryException {
        if (cronExpression == null) {
            return;
        }

        final RepositoryScheduler scheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);

        final String jobName = getJobName();
        if (scheduler.checkExists(jobName, "default")) {
            return;
        }

        final RepositoryJobInfo jobInfo = new RepositoryJobInfo(jobName, EventLogCleanupJob.class);
        jobInfo.setAttribute(CONFIG_MAXITEMS_PROPERTY, Long.toString(maxitems));
        jobInfo.setAttribute(CONFIG_KEEP_ITEMS_FOR, Long.toString(itemtimeout));
        jobInfo.setAttribute(CONFIG_MODULECONFIGPATH, moduleConfigPath);

        final RepositoryJobTrigger trigger = new RepositoryJobCronTrigger("EventLogCleanupTrigger" + quartzNamePostfix, cronExpression);

        scheduler.scheduleJob(jobInfo, trigger);
    }

    void unscheduleJob() throws RepositoryException {
        final RepositoryScheduler repositoryScheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);
        final String jobName = getJobName();
        repositoryScheduler.deleteJob(jobName, "default");
    }

    private String getJobName() {
        return "EventLogCleanupJob" + quartzNamePostfix;
    }

    public static class EventLogCleanupJob implements RepositoryJob {

        @Override
        public void execute(final RepositoryJobExecutionContext context) throws RepositoryException {

            Session session = context.createSession(new SimpleCredentials("system", new char[]{}));

            try {
                log.info("Running event log cleanup job");

                long maxitems = Long.valueOf(context.getAttribute(CONFIG_MAXITEMS_PROPERTY));
                long itemtimeout = Long.valueOf(context.getAttribute(CONFIG_KEEP_ITEMS_FOR));

                removeTooManyItems(maxitems, session);
                removeTimedOutItems(itemtimeout, session);
            } finally {
                session.logout();
            }

        }

        private void removeTooManyItems(long maxitems, Session session) throws RepositoryException {
            if (maxitems != -1) {
                final QueryManager queryManager = session.getWorkspace().getQueryManager();
                final Query query = queryManager.createQuery(ITEMS_QUERY_MAX_ITEMS, Query.SQL);
                final NodeIterator nodes = query.execute().getNodes();
                final long totalSize = ((HippoNodeIterator) nodes).getTotalSize();
                final long cleanupSize = totalSize - maxitems;
                for (int i = 0; i < cleanupSize; i++) {
                    try {
                        Node node = nodes.nextNode();
                        if (log.isDebugEnabled()) {
                            log.debug("Removing event log item at " + node.getPath());
                        }
                        remove(node);
                        if (i % 10 == 0) {
                            session.save();
                            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                        }
                    } catch (RepositoryException e) {
                        log.error("Error while cleaning up event log", e);
                    }
                }
                if (session.hasPendingChanges()) {
                    session.save();
                }
                if (cleanupSize > 0) {
                    log.info("Done cleaning " + cleanupSize + " items");
                } else {
                    log.info("No excessive amount of items");
                }
            }
        }

        private void removeTimedOutItems(long itemtimeout, Session session) throws RepositoryException {
            if (itemtimeout != -1) {
                final QueryManager queryManager = session.getWorkspace().getQueryManager();
                final Query query = queryManager.createQuery(ITEMS_QUERY_ITEM_TIMEOUT, Query.SQL);
                final NodeIterator nodes = query.execute().getNodes();
                final long timeoutTimestamp = System.currentTimeMillis() - itemtimeout;
                int i = 0;
                while (nodes.hasNext()) {
                    try {
                        final Node node = nodes.nextNode();
                        if (node.getProperty("hippolog:timestamp").getLong() > timeoutTimestamp) {
                            break;
                        }
                        if (log.isDebugEnabled()) {
                            log.debug("Removing event log item at " + node.getPath());
                        }
                        remove(node);
                        if (i++ % 10 == 0) {
                            session.save();
                            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                        }
                    } catch (RepositoryException e) {
                        log.error("Error while cleaning up event log", e);
                    }
                }
                if (session.hasPendingChanges()) {
                    session.save();
                }
                if (i > 0) {
                    log.info("Done cleaning " + i + " items");
                } else {
                    log.info("No timed out items");
                }
            }
        }

        private void remove(final Node node) throws RepositoryException {
            final Node parent = node.getParent();
            node.remove();
            if (parent != null && parent.getName().length() == 1 && parent.isNodeType("hippolog:folder") && parent.getNodes().getSize() == 0) {
                remove(parent);
            }
        }

    }

}
