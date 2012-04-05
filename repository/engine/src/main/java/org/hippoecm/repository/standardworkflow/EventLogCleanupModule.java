/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.repository.standardworkflow;

import java.text.ParseException;
import java.util.Properties;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.hippoecm.repository.api.HippoNodeIterator;
import org.hippoecm.repository.ext.DaemonModule;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cleans up hippo event log entries. The module can be configured at "/hippo:configuration/hippo:modules/eventlogcleanup/hippo:moduleconfig"
 * with the following options:<br/>
 * - property 'cronexpression' (String) a quartz cron expression specifying when to run the clean up job.
 * - property 'maxitems' (Long) the maximum number of items to keep in the logs. Defaults to -1, which means no maximum.
 * - property 'keepitemsfor' (Long) the number of milliseconds to keep items in the logs.
 */
public class EventLogCleanupModule implements DaemonModule {

    private static final Logger log = LoggerFactory.getLogger(EventLogCleanupModule.class);
    private static final String CONFIG_NODE_PATH = "/hippo:configuration/hippo:modules/eventlogcleanup/hippo:moduleconfig";
    private static final String CONFIG_MAXITEMS_PROPERTY = "maxitems";
    private static final String CONFIG_KEEP_ITEMS_FOR = "keepitemsfor";
    private static final String CONFIG_CRONEXPRESSION_PROPERTY = "cronexpression";
    private static final String CONFIG_LOCK_ISDEEP_PROPERTY = "jcr:lockIsDeep";
    private static final String CONFIG_LOCK_OWNER = "jcr:lockOwner";
    private static final Properties SCHEDULER_FACTORY_PROPERTIES = new Properties();
    static {
        SCHEDULER_FACTORY_PROPERTIES.put(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, "HippoEventLogCleanupScheduler");
        SCHEDULER_FACTORY_PROPERTIES.put(StdSchedulerFactory.PROP_SCHED_SKIP_UPDATE_CHECK, "true");
        SCHEDULER_FACTORY_PROPERTIES.put(StdSchedulerFactory.PROP_THREAD_POOL_CLASS, SimpleThreadPool.class.getName());
        SCHEDULER_FACTORY_PROPERTIES.put("org.quartz.threadPool.threadCount", "1");
        SCHEDULER_FACTORY_PROPERTIES.put(StdSchedulerFactory.PROP_JOB_STORE_CLASS, RAMJobStore.class);
    }
    
    private static final String ITEMS_QUERY_MAX_ITEMS = "SELECT * FROM hippolog:item ORDER BY hippolog:timestamp ASC";
    private static final String ITEMS_QUERY_ITEM_TIMEOUT = "SELECT * FROM hippolog:item WHERE hippolog:timestamp < $timeoutTimestamp ORDER BY hippolog:timestamp ASC";
    private static final int EVENT_TYPES = Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED;
    private static final long ONE_WEEK = 1000 * 60 * 60 * 24 * 7;
    private static Boolean buzy = false;

    private Session session;
    private Scheduler scheduler;
    private String cronExpression = null;
    private long maxitems = -1;
    private long itemtimeout = -1;
    private JobListener jobListener;
    private EventListener configurationListener;
    private JobDetail job;
    private String quartzNamePostfix = "";

    public EventLogCleanupModule() {
    }

    /* Constructor for testing. */
    EventLogCleanupModule(String cronExpression, long maxitems, long itemstimeout, JobListener jobListener, Session session) throws RepositoryException, SchedulerException, ParseException {
        this.cronExpression = cronExpression;
        this.maxitems = maxitems;
        this.itemtimeout = itemstimeout;
        this.jobListener = jobListener;
        this.session = session;
        quartzNamePostfix = "Test";
        startScheduler();
        scheduleJob();
    }

    private void startScheduler() {
        if (cronExpression == null) {
            // no cron expression configured
            return;
        }
        try {
            SchedulerFactory factory = new StdSchedulerFactory(SCHEDULER_FACTORY_PROPERTIES);
            scheduler = factory.getScheduler();
            if (jobListener != null) {
                scheduler.addGlobalJobListener(jobListener);
            }
            scheduler.start();
        } catch (SchedulerException e) {
            log.error("Failed to initialize: cannot instantiate quartz scheduler", e);
        }
    }

    private void scheduleJob() {
        if (cronExpression == null) {
            // no cron expression configured
            return;
        }
        if (scheduler == null) {
            // starting scheduler failed
            return;
        }
        try {
            job = new JobDetail("EventLogCleanupJob" + quartzNamePostfix, EventLogCleanupJob.class);
            job.getJobDataMap().put(CONFIG_MAXITEMS_PROPERTY, maxitems);
            job.getJobDataMap().put(CONFIG_KEEP_ITEMS_FOR, itemtimeout);
            job.getJobDataMap().put("session", session);
            CronTrigger trigger = new CronTrigger("EventLogCleanupTrigger" + quartzNamePostfix);
            trigger.setCronExpression(cronExpression);
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            log.error("Failed to initialize: cannot schedule job", e);
        } catch (ParseException e) {
            log.error("Failed to initialize: " + cronExpression + " is not a cron expression", e);
        }
    }

    private void unscheduleJob() throws SchedulerException {
        scheduler.deleteJob(job.getName(), job.getGroup());
    }

    private void configure() throws RepositoryException {
        cronExpression = null;
        maxitems = -1;
        itemtimeout = -1;
        if (session.nodeExists(CONFIG_NODE_PATH)) {
            Node configNode = session.getNode(CONFIG_NODE_PATH);
            if (configNode.hasProperty(CONFIG_CRONEXPRESSION_PROPERTY)) {
                cronExpression = configNode.getProperty(CONFIG_CRONEXPRESSION_PROPERTY).getString();
            }
            if (configNode.hasProperty(CONFIG_MAXITEMS_PROPERTY)) {
                maxitems = configNode.getProperty(CONFIG_MAXITEMS_PROPERTY).getLong();
            }
            if (configNode.hasProperty(CONFIG_KEEP_ITEMS_FOR)) {
                itemtimeout = configNode.getProperty(CONFIG_KEEP_ITEMS_FOR).getLong();
            }
        }
    }

    @Override
    public void initialize(final Session session) throws RepositoryException {
        this.session = session;
        configure();
        startScheduler();
        scheduleJob();
        configurationListener = new ConfigurationListener();
        session.getWorkspace().getObservationManager().addEventListener(configurationListener, EVENT_TYPES, CONFIG_NODE_PATH, false, null, null, true);
    }

    @Override
    public void shutdown() {
        if (configurationListener != null) {
            try {
                session.getWorkspace().getObservationManager().removeEventListener(configurationListener);
            } catch (RepositoryException e) {
                log.error("Error removing configuration event listener: " + e);
            }
        }
        if (scheduler != null) {
            try {
                scheduler.shutdown();
            } catch (SchedulerException e) {
                log.error("Error shutting down quartz scheduler: " + e);
            }
        }
    }

    public static class EventLogCleanupJob implements Job {

        private static final String DEFAULT_CLUSTER_NODE_ID = "default";

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {

            synchronized (buzy) {
                if (buzy) {
                    return;
                }
                buzy = true;
            }

            Session session = (Session) context.getMergedJobDataMap().get("session");

            try {
                // make sure only one cluster node runs the scheduled cleanup job
                if (!lock(session)) {
                    return;
                }

                log.info("Running event log cleanup job");

                long maxitems = (Long) context.getMergedJobDataMap().get(CONFIG_MAXITEMS_PROPERTY);
                long itemtimeout = (Long) context.getMergedJobDataMap().get(CONFIG_KEEP_ITEMS_FOR);

                removeTooManyItems(maxitems, session);
                removeTimedOutItems(itemtimeout, session);
            } catch (RepositoryException e) {
                log.error("Error while cleaning up event log", e);
            } finally {
                unlock(session);
                buzy = false;
            }

        }
        
        private boolean lock(Session session) {
            try {
                Node lockable = session.getNode(CONFIG_NODE_PATH);
                if (!lockable.isNodeType("mix:lockable")) {
                    log.error("Node " + CONFIG_NODE_PATH + " must be lockable");
                    return false;
                }
                LockManager lockManager = session.getWorkspace().getLockManager();
                if (!lockManager.isLocked(CONFIG_NODE_PATH)) {
                    try {
                        lockManager.lock(CONFIG_NODE_PATH, false, false, ONE_WEEK, getClusterNodeId(session));
                        return true;
                    } catch (LockException e) {
                        log.warn("Failed to obtain lock: " + e.getMessage() + ". Event log cleanup will not run");
                    }
                }
            } catch (RepositoryException e) {
                log.error("Failed to obtain lock: event log cleanup will not run", e);
            }
            return false;
        }
        
        private void unlock(Session session) {
            try {
                LockManager lockManager = session.getWorkspace().getLockManager();
                Lock lock = lockManager.getLock(CONFIG_NODE_PATH);
                if (lock.isLockOwningSession()) {
                    lockManager.unlock(CONFIG_NODE_PATH);
                }
            } catch (RepositoryException e) {
                log.error("Error releasing lock", e);
            }
        }
        
        private void removeTooManyItems(long maxitems, Session session) throws RepositoryException {
            if (maxitems != -1) {
                QueryManager queryManager = session.getWorkspace().getQueryManager();
                Query query = queryManager.createQuery(ITEMS_QUERY_MAX_ITEMS, Query.SQL);
                NodeIterator nodes = query.execute().getNodes();
                long totalSize = ((HippoNodeIterator)nodes).getTotalSize();
                long cleanupSize = totalSize - maxitems;
                if (cleanupSize > 0) {
                    log.info("Event log total size is " + totalSize);
                    log.info("Number of items to clean up is " + cleanupSize);
                    for (int i = 0; i < cleanupSize; i++) {
                        try {
                            Node node = nodes.nextNode();
                            if (log.isDebugEnabled()) {
                                log.debug("Removing event log item at " + node.getPath());
                            }
                            node.remove();
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
                    log.info("Done cleaning " + cleanupSize + " items");
                } else {
                    log.info("Event log total size is less than maximum allowed items: no cleanup needed");
                }
            }
        }
        
        private void removeTimedOutItems(long itemtimeout, Session session) throws RepositoryException {
            if (itemtimeout != -1) {
                QueryManager queryManager = session.getWorkspace().getQueryManager();
                long timeoutTimestamp = System.currentTimeMillis() - itemtimeout;
                String queryString = ITEMS_QUERY_ITEM_TIMEOUT.replace("$timeoutTimestamp", String.valueOf(timeoutTimestamp));
                Query query = queryManager.createQuery(queryString, Query.SQL);
                NodeIterator nodes = query.execute().getNodes();
                long totalSize = ((HippoNodeIterator)nodes).getTotalSize();
                if (totalSize > 0) {
                    log.info("Number of items to clean is " + totalSize);
                    int i = 0;
                    while (nodes.hasNext()) {
                        try {
                            Node node = nodes.nextNode();
                            if (log.isDebugEnabled()) {
                                log.debug("Removing event log item at " + node.getPath());
                            }
                            node.remove();
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
                    log.info("Done cleaning " + totalSize + " items");
                } else {
                    log.info("No timed out items: no cleanup needed");
                }
            }
        }

        private String getClusterNodeId(Session session) {
            String clusteNodeId = session.getRepository().getDescriptor("jackrabbit.cluster.id");
            if (clusteNodeId == null) {
                clusteNodeId = DEFAULT_CLUSTER_NODE_ID;
            }
            return clusteNodeId;
        }
    }

    private class ConfigurationListener implements EventListener {

        @Override
        public void onEvent(EventIterator events) {
            boolean reconfigure = false;
            // only reconfigure if any configuration properties changed
            while(events.hasNext()) {
                try {
                    String eventPath = events.nextEvent().getPath();
                    if (!eventPath.endsWith(CONFIG_LOCK_ISDEEP_PROPERTY) && !eventPath.endsWith(CONFIG_LOCK_OWNER)) {
                        reconfigure = true;
                    }
                } catch (RepositoryException e) {
                    log.error("Failed to determine if event is a reconfigure event", e);
                }
            }
            if (reconfigure) {
                try {
                    synchronized (EventLogCleanupModule.this) {
                        unscheduleJob();
                        configure();
                        scheduleJob();
                    }
                } catch (RepositoryException e) {
                    log.error("Failed to reconfigure event log cleaner", e);
                } catch (SchedulerException e) {
                    log.error("Failed to reconfigure event log cleaner", e);
                }
            }
        }
    }
}
