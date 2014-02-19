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
package org.hippoecm.repository.quartz;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.util.ISO8601;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.repository.util.JcrConstants;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobPersistenceException;
import org.quartz.SchedulerConfigException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.core.SchedulingContext;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.spi.TriggerFiredBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_CRONEXPRESSION;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_CRON_TRIGGER;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_DATA;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_ENDTIME;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_NEXTFIRETIME;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_REPEATCOUNT;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_REPEATINTERVAL;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_SIMPLE_TRIGGER;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_STARTTIME;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_TRIGGERS;

public class JCRJobStore extends AbstractJobStore {

    private static final Logger log = LoggerFactory.getLogger(SchedulerModule.class);

    private static final long TWO_MINUTES = 60 * 2;

    private final long lockTimeout;

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private Map<String, Future<?>> keepAlives = Collections.synchronizedMap(new HashMap<String, Future<?>>());

    public JCRJobStore() {
        this(TWO_MINUTES);
    }

    JCRJobStore(long lockTimeout) {
        this.lockTimeout = lockTimeout;
    }

    @Override
    public void initialize(ClassLoadHelper loadHelper, SchedulerSignaler signaler) throws SchedulerConfigException {
        signaler.signalSchedulingChange(1000);
    }

    @Override
    public boolean isClustered() {
        return true;
    }

    @Override
    public void storeJobAndTrigger(SchedulingContext ctxt, JobDetail newJob, Trigger newTrigger) throws JobPersistenceException {
        if (!(newJob instanceof JCRJobDetail)) {
            throw new JobPersistenceException("JobDetail must be of type JCRJobDetail");
        }
        if (!(newTrigger instanceof SimpleTrigger) && !(newTrigger instanceof CronTrigger)) {
            throw new JobPersistenceException("Cannot store trigger of type " + newTrigger.getClass().getName());
        }
        final JCRJobDetail jobDetail = (JCRJobDetail) newJob;
        final Session session = getSession(ctxt);
        synchronized(session) {
            try {
                final Node jobNode = session.getNodeByIdentifier(jobDetail.getIdentifier());
                jobDetail.persist(jobNode);

                final Node triggersNode;
                if(jobNode.hasNode(HIPPOSCHED_TRIGGERS)) {
                    triggersNode = jobNode.getNode(HIPPOSCHED_TRIGGERS);
                } else {
                    triggersNode = jobNode.addNode(HIPPOSCHED_TRIGGERS, HIPPOSCHED_TRIGGERS);
                }

                final Node triggerNode;

                if (newTrigger instanceof SimpleTrigger) {
                    final SimpleTrigger trigger = (SimpleTrigger) newTrigger;
                    triggerNode = triggersNode.addNode(newTrigger.getName(), HIPPOSCHED_SIMPLE_TRIGGER);
                    final Calendar startTime = Calendar.getInstance();
                    startTime.setTime(trigger.getStartTime());
                    triggerNode.setProperty(HIPPOSCHED_STARTTIME, startTime);
                    if (trigger.getEndTime() != null) {
                        final Calendar endTime = Calendar.getInstance();
                        endTime.setTime(trigger.getEndTime());
                        triggerNode.setProperty(HIPPOSCHED_ENDTIME, endTime);
                    }
                    if (trigger.getRepeatCount() != 0) {
                        triggerNode.setProperty(HIPPOSCHED_REPEATCOUNT, trigger.getRepeatCount());
                    }
                    if (trigger.getRepeatInterval() != 0) {
                        triggerNode.setProperty(HIPPOSCHED_REPEATINTERVAL, trigger.getRepeatInterval());
                    }
                } else {
                    final CronTrigger trigger = (CronTrigger) newTrigger;
                    triggerNode = triggersNode.addNode(newTrigger.getName(), HIPPOSCHED_CRON_TRIGGER);
                    triggerNode.setProperty(HIPPOSCHED_CRONEXPRESSION, trigger.getCronExpression());
                }

                triggerNode.addMixin(JcrConstants.MIX_LOCKABLE);
                final Calendar fireTime = dateToCalendar(newTrigger.getNextFireTime());
                triggerNode.setProperty(HIPPOSCHED_NEXTFIRETIME, fireTime);

                session.save();
            } catch (RepositoryException e) {
                refreshSession(session);
                throw new JobPersistenceException("Failed to store job and trigger", e);
            }
        }
    }

    @Override
    public JobDetail retrieveJob(SchedulingContext ctxt, String jobIdentifier, String groupName) throws JobPersistenceException {
        final Session session = getSession(ctxt);
        synchronized (session) {
            String jobPath = null;
            try {
                final Node jobNode = session.getNodeByIdentifier(jobIdentifier);
                jobPath = jobNode.getPath();
                return new RepositoryJobDetail(jobNode);
            } catch (ItemNotFoundException e) {
                throw new JobPersistenceException("No such job: " + jobIdentifier);
            } catch (RepositoryException e) {
                refreshSession(session);
                throw new JobPersistenceException("Failed to retrieve job at " + jobPath, e);
            }
        }
    }

    @Override
    public Trigger[] getTriggersForJob(final SchedulingContext ctxt, final String jobIdentifier, final String groupName) throws JobPersistenceException {
        final Session session = getSession(ctxt);
        synchronized (session) {
            try {
                final Node jobNode = session.getNodeByIdentifier(jobIdentifier);
                final Node triggersNode = JcrUtils.getNodeIfExists(jobNode, HIPPOSCHED_TRIGGERS);
                if (triggersNode != null) {
                    final List<Trigger> triggers = new ArrayList<Trigger>();
                    for (Node triggerNode : new NodeIterable(triggersNode.getNodes())) {
                        if (triggerNode != null) {
                            try {
                                final Trigger trigger = createTriggerFromNode(triggerNode);
                                if (trigger != null) {
                                    triggers.add(trigger);
                                }
                            } catch (RepositoryException e) {
                                throw new JobPersistenceException("Failed to create trigger", e);
                            } catch (ClassNotFoundException e) {
                                throw new JobPersistenceException("Failed to create trigger", e);
                            } catch (IOException e) {
                                throw new JobPersistenceException("Failed to create trigger", e);
                            }
                        }
                    }
                    return triggers.toArray(new Trigger[triggers.size()]);
                }
            } catch (ItemNotFoundException e) {
                throw new JobPersistenceException("No such job " + jobIdentifier);
            } catch (RepositoryException e) {
                throw new JobPersistenceException("Failed to get triggers for job", e);
            }
        }
        return new Trigger[0];
    }

    @Override
    public Trigger acquireNextTrigger(SchedulingContext ctxt, long noLaterThan) throws JobPersistenceException {
        final Session session = getSession(ctxt);
        synchronized (session) {
            try {
                for (Node triggerNode : getPendingTriggers(session, noLaterThan)) {
                    if(triggerNode != null) {
                        final Node jobNode = triggerNode.getParent().getParent();
                        if (lock(session, triggerNode.getPath())) {
                            try {
                                startLockKeepAlive(session, triggerNode.getIdentifier());
                                return createTriggerFromNode(triggerNode);
                            } catch (IOException e) {
                                log.error("Failed to read trigger for job " + jobNode.getPath(), e);
                                stopLockKeepAlive(triggerNode.getIdentifier());
                                unlock(session, triggerNode.getPath());
                            } catch (ClassNotFoundException e) {
                                log.error("Failed to recreate trigger for job " + jobNode.getPath(), e);
                                stopLockKeepAlive(triggerNode.getIdentifier());
                                unlock(session, triggerNode.getPath());
                            }
                        }
                    }
                }
            } catch (RepositoryException e) {
                refreshSession(session);
                log.error("Failed to acquire next trigger", e);
            }
        }
        return null;
    }

    private Trigger createTriggerFromNode(final Node triggerNode) throws RepositoryException, ClassNotFoundException, IOException {
        Trigger trigger = null;
        if (triggerNode.hasProperty(HIPPOSCHED_DATA)) {
            log.warn("Cannot deserialize obsolete trigger definition at " + triggerNode.getPath());
        } else {
            final String triggerType = triggerNode.getPrimaryNodeType().getName();
            final Calendar nextFireTime = JcrUtils.getDateProperty(triggerNode, HIPPOSCHED_NEXTFIRETIME, Calendar.getInstance());
            if (HIPPOSCHED_SIMPLE_TRIGGER.equals(triggerType)) {
                final Calendar startTime = JcrUtils.getDateProperty(triggerNode, HIPPOSCHED_STARTTIME, null);
                final Calendar endTime = JcrUtils.getDateProperty(triggerNode, HIPPOSCHED_ENDTIME, null);
                final long repeatCount = JcrUtils.getLongProperty(triggerNode, HIPPOSCHED_REPEATCOUNT, 0);
                final long repeatInterval = JcrUtils.getLongProperty(triggerNode, HIPPOSCHED_REPEATINTERVAL, 0);
                if (startTime == null) {
                    log.warn("Cannot create simple trigger from node {}: mandatory property {} is missing",
                            triggerNode.getPath(), HIPPOSCHED_STARTTIME);
                } else {
                    final SimpleTrigger simpleTrigger = new SimpleTrigger(triggerNode.getIdentifier(), startTime.getTime());
                    if (endTime != null) {
                        simpleTrigger.setEndTime(endTime.getTime());
                    }
                    if (repeatCount != 0) {
                        simpleTrigger.setRepeatCount((int) repeatCount);
                    }
                    if (repeatInterval != 0) {
                        simpleTrigger.setRepeatInterval(repeatInterval);
                    }


                    simpleTrigger.setNextFireTime(nextFireTime.getTime());
                    trigger = simpleTrigger;
                }
            } else if (HIPPOSCHED_CRON_TRIGGER.equals(triggerType)) {
                final String cronExpression = JcrUtils.getStringProperty(triggerNode, HIPPOSCHED_CRONEXPRESSION, null);
                if (cronExpression == null) {
                    log.warn("Cannot create cron trigger from node {}: mandatory property {} is missing",
                            triggerNode.getPath(), HIPPOSCHED_CRONEXPRESSION);
                } else {
                    try {
                        CronTrigger cronTrigger = new CronTrigger(triggerNode.getIdentifier(), null, cronExpression);
                        cronTrigger.setNextFireTime(nextFireTime.getTime());
                        trigger = cronTrigger;
                    } catch (ParseException e) {
                        log.warn("Failed to create cron trigger from node {}: invalid cron expression {}",
                                triggerNode.getPath(), cronExpression);
                    }
                }
            } else {
                log.warn("Cannot create trigger of unknown type {}", triggerType);
            }
        }
        if (trigger != null) {
            trigger.setJobName(triggerNode.getParent().getParent().getIdentifier());
        }
        return trigger;
    }

    @Override
    public void releaseAcquiredTrigger(SchedulingContext ctxt, Trigger trigger) throws JobPersistenceException {
        final Session session = getSession(ctxt);
        synchronized (session) {
            try {
                final String triggerIdentifier = trigger.getName();
                stopLockKeepAlive(triggerIdentifier);
                final Node triggerNode = session.getNodeByIdentifier(triggerIdentifier);
                unlock(session, triggerNode.getPath());
            } catch (ItemNotFoundException e) {
                log.info("Trigger no longer exists: " + trigger.getName());
            } catch (RepositoryException e) {
                refreshSession(session);
                final String message = "Failed to release acquired trigger";
                log.error(message, e);
                throw new JobPersistenceException(message, e);
            }
        }
    }

    @Override
    public TriggerFiredBundle triggerFired(SchedulingContext ctxt, Trigger trigger) {
        try {
            return new TriggerFiredBundle(retrieveJob(ctxt, trigger.getJobName(), trigger.getJobGroup()),
                    trigger, null, false,
                    trigger.getPreviousFireTime(), trigger.getPreviousFireTime(),
                    trigger.getPreviousFireTime(), trigger.getNextFireTime());
        } catch (JobPersistenceException e) {
            log.error("Failed to verify job", e);
            return null;
        }
    }

    @Override
    public void triggeredJobComplete(SchedulingContext ctxt, Trigger trigger, JobDetail jobDetail, int triggerInstCode) throws JobPersistenceException {
        if (!(jobDetail instanceof JCRJobDetail)) {
            throw new JobPersistenceException("JobDetail must be of type JCRJobDetail");
        }
        final Session session = getSession(ctxt);
        synchronized (session) {
            try {
                final String triggerIdentifier = trigger.getName();
                stopLockKeepAlive(triggerIdentifier);
                final Node triggerNode = session.getNodeByIdentifier(triggerIdentifier);
                final Date nextFire = trigger.getFireTimeAfter(new Date());
                if(nextFire != null) {
                    final Calendar nextFireTime = dateToCalendar(nextFire);
                    triggerNode.setProperty(HIPPOSCHED_NEXTFIRETIME, nextFireTime);
                    session.save();
                    unlock(session, triggerNode.getPath());
                } else {
                    final String jobIdentifier = ((JCRJobDetail) jobDetail).getIdentifier();
                    final Node jobNode = session.getNodeByIdentifier(jobIdentifier);
                    JcrUtils.ensureIsCheckedOut(jobNode.getParent());
                    jobNode.remove();
                    session.save();
                }
            } catch (ItemNotFoundException e) {
                log.info("Trigger no longer exists: " + trigger.getName());
            } catch (RepositoryException e) {
                refreshSession(session);
                final String message = "Failed to finalize job: " + ((JCRJobDetail) jobDetail).getIdentifier();
                log.error(message, e);
            }
        }
    }

    private static Session getSession(SchedulingContext ctxt) {
        if (ctxt instanceof JCRSchedulingContext) {
            return ((JCRSchedulingContext) ctxt).getSession();
        }
        return SchedulerModule.getSession();
    }

    private static void refreshSession(Session session) {
        try {
            session.refresh(false);
        } catch (RepositoryException e) {
            log.error("Failed to refresh session", e);
        }
    }

    private static Calendar dateToCalendar(Date date) {
        final Calendar result = Calendar.getInstance();
        result.setTime(date);
        return result;
    }

    private static NodeIterable getPendingTriggers(Session session, long noLaterThan) {
        try {
            final Calendar cal = dateToCalendar(new Date(noLaterThan));
            final QueryManager qMgr = session.getWorkspace().getQueryManager();
            final Query query = qMgr.createQuery(
                    "SELECT * FROM hipposched:trigger WHERE hipposched:nextFireTime <= TIMESTAMP '"
                            + ISO8601.format(cal) + "' ORDER BY hipposched:nextFireTime", Query.SQL);
            final QueryResult result = query.execute();
            return new NodeIterable(result.getNodes());
        } catch (RepositoryException e) {
            log.error("Failed to query for pending triggers", e);
            return JcrUtils.emptyNodeIterable();
        }
    }

    private boolean lock(Session session, String nodePath) throws RepositoryException {
        log.debug("Trying to obtain lock on " + nodePath);
        final LockManager lockManager = session.getWorkspace().getLockManager();
        if (!lockManager.isLocked(nodePath)) {
            try {
                ensureIsLockable(session, nodePath);
                lockManager.lock(nodePath, false, false, lockTimeout, getClusterNodeId(session));
                log.debug("Lock successfully obtained on " + nodePath);
                return true;
            } catch (LockException e) {
                // happens when other cluster node beat us to it
                log.debug("Failed to set lock on "  + nodePath +  ": " + e.getMessage());
            }
        } else {
            log.debug("Already locked " + nodePath);
        }
        return false;
    }

    private void unlock(Session session, String nodePath) throws RepositoryException {
        log.debug("Trying to release lock on " + nodePath);
        try {
            final LockManager lockManager = session.getWorkspace().getLockManager();
            if (lockManager.isLocked(nodePath)) {
                final Lock lock = lockManager.getLock(nodePath);
                if (lock.isLockOwningSession()) {
                    lockManager.unlock(nodePath);
                    log.debug("Lock successfully released on " + nodePath);
                } else {
                    log.debug("We don't own the lock on " + nodePath);
                }
            } else {
                log.debug("Not locked " + nodePath);
            }
        } catch (RepositoryException e) {
            log.error("Failed to release lock on " + nodePath, e);
        }
    }

    private void refreshLock(final Session session, String identifier) {
        synchronized (session) {
            try {
                final Node node = session.getNodeByIdentifier(identifier);
                final LockManager lockManager = session.getWorkspace().getLockManager();
                final Lock lock = lockManager.getLock(node.getPath());
                lock.refresh();
                log.debug("Lock successfully refreshed");
            } catch (RepositoryException e) {
                log.error("Failed to refresh lock", e);
            }
        }
    }

    private void startLockKeepAlive(final Session session, final String identifier) {
        final long refreshInterval = lockTimeout / 2;
        final Future<?> future = executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                refreshLock(session, identifier);
            }
        }, refreshInterval, refreshInterval, TimeUnit.SECONDS);
        keepAlives.put(identifier, future);
    }

    private void stopLockKeepAlive(final String identifier) {
        final Future<?> future = keepAlives.remove(identifier);
        if (future != null) {
            future.cancel(true);
        }
    }

    private static void ensureIsLockable(Session session, String nodePath) throws RepositoryException {
        final Node node = session.getNode(nodePath);
        if (!node.isNodeType(JcrConstants.MIX_LOCKABLE)) {
            node.addMixin(JcrConstants.MIX_LOCKABLE);
        }
        session.save();
    }

    private static String getClusterNodeId(Session session) {
        String clusteNodeId = session.getRepository().getDescriptor("jackrabbit.cluster.id");
        if (clusteNodeId == null) {
            clusteNodeId = "default";
        }
        return clusteNodeId;
    }

}
