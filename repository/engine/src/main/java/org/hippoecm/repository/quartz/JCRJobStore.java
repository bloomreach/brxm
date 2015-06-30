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
package org.hippoecm.repository.quartz;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.util.ISO8601;
import org.hippoecm.repository.api.SynchronousEventListener;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.RepoUtils;
import org.onehippo.repository.locking.HippoLockManager;
import org.onehippo.repository.util.JcrConstants;
import org.quartz.Calendar;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.SchedulerConfigException;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.calendar.BaseCalendar;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.JobStore;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.spi.TriggerFiredBundle;
import org.quartz.spi.TriggerFiredResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_CRONEXPRESSION;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_CRON_TRIGGER;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_DATA;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_ENABLED;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_ENDTIME;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_NEXTFIRETIME;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_REPEATCOUNT;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_REPEATINTERVAL;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_SIMPLE_TRIGGER;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_STARTTIME;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_TRIGGERS;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_WORKFLOW_JOB;
import static org.hippoecm.repository.util.JcrUtils.ALL_EVENTS;
import static org.hippoecm.repository.util.RepoUtils.getClusterNodeId;
import static org.quartz.SimpleTrigger.REPEAT_INDEFINITELY;


public class JCRJobStore implements JobStore {

    private static final Logger log = LoggerFactory.getLogger(JCRJobStore.class);
    private static final long TWO_MINUTES = 60 * 2;

    private final long lockTimeout;
    private final Session session;
    private final String jobStorePath;

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private Map<String, Future<?>> keepAlives = Collections.synchronizedMap(new HashMap<String, Future<?>>());

    private EventListener listener;

    public JCRJobStore() {
        this(TWO_MINUTES, null);
    }

    JCRJobStore(final Session session) {
        this(TWO_MINUTES, session);
    }

    JCRJobStore(final long lockTimeout, final Session session) {
        this(lockTimeout, session, SchedulerModule.getModuleConfigPath());
    }

    JCRJobStore(final long lockTimeout, final Session session, final String jobStorePath) {
        this.lockTimeout = lockTimeout;
        this.session = session;
        this.jobStorePath = jobStorePath;
    }

    @Override
    public void initialize(final ClassLoadHelper loadHelper, final SchedulerSignaler signaler)
            throws SchedulerConfigException {
        if (signaler != null) {
            signaler.signalSchedulingChange(1000);
        }
        initializeTriggers();
        try {
            getSession().getWorkspace().getObservationManager()
                    .addEventListener(listener = new SynchronousEventListener() {
                @Override
                public void onEvent(final EventIterator events) {
                    if (hasTriggerUpdateEvents(events)) {
                        initializeTriggers();
                    }
                }

            }, ALL_EVENTS, jobStorePath, true, null, null, true);
        } catch (RepositoryException e) {
            log.error("Failed to register event listener for initializing triggers", e);
        }
    }

    /**
     * True if either a hipposched property was added, removed or changed,
     * or a node event came in.
     */
    static boolean hasTriggerUpdateEvents(final EventIterator events) {
        while (events.hasNext()) {
            final Event event = events.nextEvent();
            if (JcrUtils.isPropertyEvent(event)) {
                try {
                    final String propertyName = substringAfterLast(event.getPath(), "/");
                    if (isTriggerUpdateProperty(propertyName)) {
                        return true;
                    }
                } catch (RepositoryException ignore) {
                }
            }
        }
        return false;
    }

    private static boolean isTriggerUpdateProperty(final String propertyName) {
        switch (propertyName) {
            case HIPPOSCHED_ENABLED:
            case HIPPOSCHED_STARTTIME:
            case HIPPOSCHED_ENDTIME:
            case HIPPOSCHED_REPEATINTERVAL:
            case HIPPOSCHED_CRONEXPRESSION:
                return true;
        }
        return false;
    }

    /**
     * Find triggers without a nextFireTime property (i.e. that were manually added in the repository)
     * and compute and set the nextFireTime property.
     */
    private void initializeTriggers() {
        log.debug("Initializing triggers");
        try {
            boolean changes = false;
            final Session session = getSession();
            synchronized (session) {
                final Node moduleConfig = session.getNode(jobStorePath);
                for (Node groupNode : new NodeIterable(moduleConfig.getNodes())) {
                    for (Node jobNode : new NodeIterable(groupNode.getNodes())) {
                        changes |= initializeTriggersOfJob(jobNode);
                    }
                }
            }
            if (changes) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        final Session session = getSession();
                        synchronized (session) {
                            try {
                                session.save();
                            } catch (RepositoryException e) {
                                log.error("Failed to save ");
                                try {
                                    session.refresh(false);
                                } catch (RepositoryException ignore) {
                                }
                            }
                        }
                    }
                });
            }
        } catch (RepositoryException e) {
            log.error("Failed to initialize triggers", e);
        }
    }

    private boolean initializeTriggersOfJob(final Node jobNode) throws RepositoryException {
        boolean changes = false;

        final boolean jobEnabled = JcrUtils.getBooleanProperty(jobNode, HIPPOSCHED_ENABLED, true);
        if (jobNode.hasNode(HIPPOSCHED_TRIGGERS)) {
            for (Node triggerNode : new NodeIterable(jobNode.getNode(HIPPOSCHED_TRIGGERS).getNodes())) {
                final boolean triggerEnabled = JcrUtils.getBooleanProperty(triggerNode, HIPPOSCHED_ENABLED, true);
                if (!triggerNode.isLocked()) {
                    if (jobEnabled && triggerEnabled) {
                        changes |= initializeTrigger(triggerNode);
                    }
                    if ((!jobEnabled || !triggerEnabled) && triggerNode.hasProperty(HIPPOSCHED_NEXTFIRETIME)) {
                        log.info("Disabling trigger {}", triggerNode.getPath());
                        triggerNode.getProperty(HIPPOSCHED_NEXTFIRETIME).remove();
                        changes = true;
                    }
                }
            }
        }
        return changes;
    }

    private boolean initializeTrigger(final Node triggerNode) throws RepositoryException {
        boolean changes = false;

        final OperableTrigger trigger = getOperableTrigger(triggerNode, null);
        if (trigger != null) {
            final Date nextFireTime = trigger.computeFirstFireTime(new BaseCalendar());
            if (nextFireTime != null) {
                final java.util.Calendar currentFireTime = JcrUtils.getDateProperty(triggerNode, HIPPOSCHED_NEXTFIRETIME, null);
                if (currentFireTime == null || nextFireTime.getTime() != currentFireTime.getTime().getTime()) {
                    log.info("Initializing trigger {}", triggerNode.getPath());
                    triggerNode.setProperty(HIPPOSCHED_NEXTFIRETIME, dateToCalendar(nextFireTime));
                    changes = true;
                }
            } else {
                log.warn("No fire time for manually added trigger {}", triggerNode.getPath());
            }
        }
        return changes;
    }

    @Override
    public void schedulerStarted() throws SchedulerException {
    }

    @Override
    public void schedulerPaused() {
    }

    @Override
    public void schedulerResumed() {
    }

    @Override
    public void shutdown() {
        if (listener != null) {
            try {
                getSession().getWorkspace().getObservationManager().removeEventListener(listener);
            } catch (RepositoryException ignore) {
            }
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @Override
    public boolean supportsPersistence() {
        return true;
    }

    @Override
    public long getEstimatedTimeToReleaseAndAcquireTrigger() {
        return 0;
    }

    @Override
    public boolean isClustered() {
        return true;
    }

    @Override
    public void storeJobAndTrigger(final JobDetail newJob, final OperableTrigger newTrigger)
            throws ObjectAlreadyExistsException, JobPersistenceException {
        if (!(newJob instanceof RepositoryJobDetail)) {
            throw new JobPersistenceException("JobDetail must be of type RepositoryJobDetail");
        }
        if (!(newTrigger instanceof SimpleTrigger) && !(newTrigger instanceof CronTrigger)) {
            throw new JobPersistenceException("Cannot store trigger of type " + newTrigger.getClass().getName());
        }
        final RepositoryJobDetail jobDetail = (RepositoryJobDetail) newJob;
        final Session session = getSession();
        synchronized(session) {
            try {
                final Node jobNode = session.getNodeByIdentifier(jobDetail.getIdentifier());

                final Node triggersNode;
                if(jobNode.hasNode(HIPPOSCHED_TRIGGERS)) {
                    triggersNode = jobNode.getNode(HIPPOSCHED_TRIGGERS);
                } else {
                    triggersNode = jobNode.addNode(HIPPOSCHED_TRIGGERS, HIPPOSCHED_TRIGGERS);
                }

                final Node triggerNode;

                if (newTrigger instanceof SimpleTrigger) {
                    final SimpleTrigger trigger = (SimpleTrigger) newTrigger;
                    triggerNode = triggersNode.addNode(newTrigger.getKey().getName(), HIPPOSCHED_SIMPLE_TRIGGER);
                    final java.util.Calendar startTime = java.util.Calendar.getInstance();
                    startTime.setTime(trigger.getStartTime());
                    triggerNode.setProperty(HIPPOSCHED_STARTTIME, startTime);
                    if (trigger.getEndTime() != null) {
                        final java.util.Calendar endTime = java.util.Calendar.getInstance();
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
                    triggerNode = triggersNode.addNode(newTrigger.getKey().getName(), HIPPOSCHED_CRON_TRIGGER);
                    triggerNode.setProperty(HIPPOSCHED_CRONEXPRESSION, trigger.getCronExpression());
                }

                triggerNode.addMixin(JcrConstants.MIX_LOCKABLE);
                final java.util.Calendar fireTime = dateToCalendar(newTrigger.getNextFireTime());
                triggerNode.setProperty(HIPPOSCHED_NEXTFIRETIME, fireTime);

                session.save();
            } catch (RepositoryException e) {
                refreshSession(session);
                throw new JobPersistenceException("Failed to store job and trigger", e);
            }
        }
    }

    @Override
    public void storeJob(final JobDetail newJob, final boolean replaceExisting) throws ObjectAlreadyExistsException, JobPersistenceException {
    }

    @Override
    public void storeJobsAndTriggers(final Map<JobDetail, Set<? extends Trigger>> triggersAndJobs, final boolean replace) throws ObjectAlreadyExistsException, JobPersistenceException {
    }

    @Override
    public boolean removeJob(final JobKey jobKey) throws JobPersistenceException {
        return false;
    }

    @Override
    public boolean removeJobs(final List<JobKey> jobKeys) throws JobPersistenceException {
        return false;
    }

    @Override
    public JobDetail retrieveJob(final JobKey jobKey) throws JobPersistenceException {
        final Session session = getSession();
        synchronized (session) {
            String jobPath = null;
            try {
                final Node jobNode = session.getNodeByIdentifier(jobKey.getName());
                jobPath = jobNode.getPath();
                return new RepositoryJobDetail(jobNode);
            } catch (ItemNotFoundException e) {
                throw new JobPersistenceException("No such job: " + jobKey.getName());
            } catch (RepositoryException e) {
                refreshSession(session);
                throw new JobPersistenceException("Failed to retrieve job at " + jobPath, e);
            }
        }

    }

    @Override
    public void storeTrigger(final OperableTrigger newTrigger, final boolean replaceExisting) throws ObjectAlreadyExistsException, JobPersistenceException {
    }

    @Override
    public boolean removeTrigger(final TriggerKey triggerKey) throws JobPersistenceException {
        return false;
    }

    @Override
    public boolean removeTriggers(final List<TriggerKey> triggerKeys) throws JobPersistenceException {
        return false;
    }

    @Override
    public boolean replaceTrigger(final TriggerKey triggerKey, final OperableTrigger newTrigger) throws JobPersistenceException {
        return false;
    }

    @Override
    public OperableTrigger retrieveTrigger(final TriggerKey triggerKey) throws JobPersistenceException {
        return null;
    }

    @Override
    public boolean checkExists(final JobKey jobKey) throws JobPersistenceException {
        return false;
    }

    @Override
    public boolean checkExists(final TriggerKey triggerKey) throws JobPersistenceException {
        return false;
    }

    @Override
    public void clearAllSchedulingData() throws JobPersistenceException {
    }

    @Override
    public void storeCalendar(final String name, final Calendar calendar, final boolean replaceExisting, final boolean updateTriggers) throws ObjectAlreadyExistsException, JobPersistenceException {
    }

    @Override
    public boolean removeCalendar(final String calName) throws JobPersistenceException {
        return false;
    }

    @Override
    public Calendar retrieveCalendar(final String calName) throws JobPersistenceException {
        return null;
    }

    @Override
    public int getNumberOfJobs() throws JobPersistenceException {
        return 0;
    }

    @Override
    public int getNumberOfTriggers() throws JobPersistenceException {
        return 0;
    }

    @Override
    public int getNumberOfCalendars() throws JobPersistenceException {
        return 0;
    }

    @Override
    public Set<JobKey> getJobKeys(final GroupMatcher<JobKey> matcher) throws JobPersistenceException {
        return null;
    }

    @Override
    public Set<TriggerKey> getTriggerKeys(final GroupMatcher<TriggerKey> matcher) throws JobPersistenceException {
        return null;
    }

    @Override
    public List<String> getJobGroupNames() throws JobPersistenceException {
        return null;
    }

    @Override
    public List<String> getTriggerGroupNames() throws JobPersistenceException {
        return null;
    }

    @Override
    public List<String> getCalendarNames() throws JobPersistenceException {
        return null;
    }

    @Override
    public List<OperableTrigger> getTriggersForJob(final JobKey jobKey) throws JobPersistenceException {
        final String jobIdentifier = jobKey.getName();
        final Session session = getSession();
        synchronized (session) {
            try {
                final Node jobNode = session.getNodeByIdentifier(jobIdentifier);
                final Node triggersNode = JcrUtils.getNodeIfExists(jobNode, HIPPOSCHED_TRIGGERS);
                if (triggersNode != null) {
                    final List<OperableTrigger> triggers = new ArrayList<>();
                    for (Node triggerNode : new NodeIterable(triggersNode.getNodes())) {
                        if (triggerNode != null) {
                            try {
                                final OperableTrigger trigger = createTriggerFromNode(triggerNode);
                                if (trigger != null) {
                                    triggers.add(trigger);
                                }
                            } catch (RepositoryException e) {
                                throw new JobPersistenceException("Failed to create trigger", e);
                            }
                        }
                    }
                    return triggers;
                }
            } catch (ItemNotFoundException e) {
                throw new JobPersistenceException("No such job " + jobIdentifier);
            } catch (RepositoryException e) {
                throw new JobPersistenceException("Failed to get triggers for job", e);
            }
        }
        return Collections.emptyList();

    }

    @Override
    public Trigger.TriggerState getTriggerState(final TriggerKey triggerKey) throws JobPersistenceException {
        return null;
    }

    @Override
    public void pauseTrigger(final TriggerKey triggerKey) throws JobPersistenceException {
    }

    @Override
    public Collection<String> pauseTriggers(final GroupMatcher<TriggerKey> matcher) throws JobPersistenceException {
        return null;
    }

    @Override
    public void pauseJob(final JobKey jobKey) throws JobPersistenceException {
    }

    @Override
    public Collection<String> pauseJobs(final GroupMatcher<JobKey> groupMatcher) throws JobPersistenceException {
        return null;
    }

    @Override
    public void resumeTrigger(final TriggerKey triggerKey) throws JobPersistenceException {
    }

    @Override
    public Collection<String> resumeTriggers(final GroupMatcher<TriggerKey> matcher) throws JobPersistenceException {
        return null;
    }

    @Override
    public Set<String> getPausedTriggerGroups() throws JobPersistenceException {
        return null;
    }

    @Override
    public void resumeJob(final JobKey jobKey) throws JobPersistenceException {
    }

    @Override
    public Collection<String> resumeJobs(final GroupMatcher<JobKey> matcher) throws JobPersistenceException {
        return null;
    }

    @Override
    public void pauseAll() throws JobPersistenceException {
    }

    @Override
    public void resumeAll() throws JobPersistenceException {
    }

    @Override
    public List<OperableTrigger> acquireNextTriggers(final long noLaterThan, int maxCount, final long timeWindow) throws JobPersistenceException {
        final Session session = getSession();
        List<OperableTrigger> triggers = null;
        synchronized (session) {
            try {
                for (Node triggerNode : getPendingTriggers(session, noLaterThan)) {
                    if (!JcrUtils.getBooleanProperty(triggerNode, HIPPOSCHED_ENABLED, true)) {
                        continue;
                    }
                    final Node jobNode = triggerNode.getParent().getParent();
                    if (!JcrUtils.getBooleanProperty(jobNode, HIPPOSCHED_ENABLED, true)) {
                        continue;
                    }
                    if (lock(session, triggerNode.getPath())) {
                        try {
                            // double check nextFireTime now that we have a lock
                            if (isPendingTrigger(triggerNode, noLaterThan)) {
                                startLockKeepAlive(session, triggerNode.getIdentifier());
                                if (triggers == null) {
                                    triggers = new ArrayList<>();
                                }
                                triggers.add(createTriggerFromNode(triggerNode));
                                if (--maxCount <= 0) {
                                    break;
                                }
                            } else {
                                unlock(session, triggerNode.getPath());
                            }
                        } catch (RepositoryException e) {
                            log.error("Failed to recreate trigger for job {}", jobNode.getPath(), e);
                            stopLockKeepAlive(triggerNode.getIdentifier());
                            unlock(session, triggerNode.getPath());
                        }
                    }
                }
            } catch (RepositoryException e) {
                refreshSession(session);
                log.error("Failed to acquire next trigger", e);
            }
        }
        return triggers == null ? Collections.<OperableTrigger>emptyList() : triggers;

    }

    private boolean isPendingTrigger(final Node triggerNode, final long noLaterThan) throws RepositoryException {
        final java.util.Calendar nextFireTime = JcrUtils.getDateProperty(triggerNode, HIPPOSCHED_NEXTFIRETIME, null);
        return nextFireTime != null && nextFireTime.compareTo(dateToCalendar(new Date(noLaterThan))) <= 0;
    }

    @Override
    public void releaseAcquiredTrigger(final OperableTrigger trigger) {
        final Session session = getSession();
        synchronized (session) {
            try {
                final String triggerIdentifier = trigger.getKey().getName();
                stopLockKeepAlive(triggerIdentifier);
                final Node triggerNode = session.getNodeByIdentifier(triggerIdentifier);
                unlock(session, triggerNode.getPath());
            } catch (ItemNotFoundException e) {
                log.info("Trigger no longer exists: {}", trigger.getKey().getName());
            } catch (RepositoryException e) {
                refreshSession(session);
                log.error("Failed to release acquired trigger", e);
            }
        }
    }

    @Override
    public List<TriggerFiredResult> triggersFired(final List<OperableTrigger> triggers) throws JobPersistenceException {
        final List<TriggerFiredResult> results = new ArrayList<>(triggers.size());
        for (OperableTrigger trigger : triggers) {
            try {
                results.add(new TriggerFiredResult(
                        new TriggerFiredBundle(retrieveJob(trigger.getJobKey()),
                                trigger, null, false,
                                trigger.getPreviousFireTime(), trigger.getPreviousFireTime(),
                                trigger.getPreviousFireTime(), trigger.getNextFireTime())));
            } catch (JobPersistenceException e) {
                log.error("Failed to verify job", e);
                results.add(new TriggerFiredResult(e));
            }
        }
        return results;
    }

    @Override
    public void triggeredJobComplete(final OperableTrigger trigger, final JobDetail jobDetail, final Trigger.CompletedExecutionInstruction triggerInstCode) {
        if (!(jobDetail instanceof RepositoryJobDetail)) {
            log.warn("JobDetail must be of type RepositoryJobDetail");
            return;
        }
        RepositoryJobDetail repositoryJobDetail = (RepositoryJobDetail) jobDetail;
        final Session session = getSession();
        synchronized (session) {
            try {
                final String triggerIdentifier = trigger.getKey().getName();
                stopLockKeepAlive(triggerIdentifier);
                final Node triggerNode = session.getNodeByIdentifier(triggerIdentifier);
                final Date nextFire = trigger.getFireTimeAfter(new Date());
                if(nextFire != null) {
                    final java.util.Calendar nextFireTime = dateToCalendar(nextFire);
                    triggerNode.setProperty(HIPPOSCHED_NEXTFIRETIME, nextFireTime);
                    if (trigger instanceof SimpleTrigger) {
                        updateRepeatCount((SimpleTrigger) trigger, triggerNode);
                    }
                    session.save();
                    unlock(session, triggerNode.getPath());
                } else {
                    final String jobIdentifier = repositoryJobDetail.getIdentifier();
                    final Node jobNode = session.getNodeByIdentifier(jobIdentifier);
                    if (removeAfterLastFireTime(jobNode)) {
                        JcrUtils.ensureIsCheckedOut(jobNode.getParent());
                        jobNode.remove();
                        session.save();
                    }
                }
            } catch (ItemNotFoundException e) {
                log.info("Trigger no longer exists: " + trigger.getKey().getName());
            } catch (RepositoryException e) {
                refreshSession(session);
                log.error("Failed to finalize job: " + repositoryJobDetail.getIdentifier(), e);
            }
        }

    }

    private void updateRepeatCount(final SimpleTrigger trigger, final Node triggerNode) throws RepositoryException {
        final int repeatCount = trigger.getRepeatCount();
        if (repeatCount != REPEAT_INDEFINITELY) {
            final int newRepeatCount = repeatCount -1;
            triggerNode.setProperty(HIPPOSCHED_REPEATCOUNT, newRepeatCount);
        }
    }

    private boolean removeAfterLastFireTime(final Node jobNode) throws RepositoryException {
        return jobNode.isNodeType(HIPPOSCHED_WORKFLOW_JOB);
    }

    @Override
    public void setInstanceId(final String schedInstId) {
    }

    @Override
    public void setInstanceName(final String schedName) {
    }

    @Override
    public void setThreadPoolSize(final int poolSize) {
    }

    private OperableTrigger createTriggerFromNode(final Node triggerNode) throws RepositoryException {
        OperableTrigger trigger = null;
        if (triggerNode.hasProperty(HIPPOSCHED_DATA)) {
            log.warn("Cannot deserialize obsolete trigger definition at " + triggerNode.getPath());
        } else {
            final java.util.Calendar nextFireTime = JcrUtils.getDateProperty(triggerNode, HIPPOSCHED_NEXTFIRETIME, java.util.Calendar.getInstance());
            trigger = getOperableTrigger(triggerNode, nextFireTime);
        }
        return trigger;
    }

    private OperableTrigger getOperableTrigger(final Node triggerNode, final java.util.Calendar nextFireTime) throws RepositoryException {
        OperableTrigger trigger = null;
        final String triggerType = triggerNode.getPrimaryNodeType().getName();
        if (HIPPOSCHED_SIMPLE_TRIGGER.equals(triggerType)) {
            final java.util.Calendar startTime = JcrUtils.getDateProperty(triggerNode, HIPPOSCHED_STARTTIME, null);
            final java.util.Calendar endTime = JcrUtils.getDateProperty(triggerNode, HIPPOSCHED_ENDTIME, null);
            final long repeatCount = JcrUtils.getLongProperty(triggerNode, HIPPOSCHED_REPEATCOUNT, 0L);
            final long repeatInterval = JcrUtils.getLongProperty(triggerNode, HIPPOSCHED_REPEATINTERVAL, 0L);
            if (startTime == null) {
                log.warn("Cannot create simple trigger from node {}: mandatory property {} is missing",
                        triggerNode.getPath(), HIPPOSCHED_STARTTIME);
            } else {
                final SimpleTriggerImpl simpleTrigger = new SimpleTriggerImpl(triggerNode.getIdentifier(), startTime.getTime());
                if (endTime != null) {
                    simpleTrigger.setEndTime(endTime.getTime());
                }
                if (repeatCount != 0) {
                    simpleTrigger.setRepeatCount((int) repeatCount);
                }
                if (repeatInterval != 0) {
                    simpleTrigger.setRepeatInterval(repeatInterval);
                }
                if (nextFireTime != null) {
                    simpleTrigger.setNextFireTime(nextFireTime.getTime());
                }
                simpleTrigger.setJobName(triggerNode.getParent().getParent().getIdentifier());
                trigger = simpleTrigger;
            }
        } else if (HIPPOSCHED_CRON_TRIGGER.equals(triggerType)) {
            final String cronExpression = JcrUtils.getStringProperty(triggerNode, HIPPOSCHED_CRONEXPRESSION, null);
            if (cronExpression == null) {
                log.warn("Cannot create cron trigger from node {}: mandatory property {} is missing",
                        triggerNode.getPath(), HIPPOSCHED_CRONEXPRESSION);
            } else {
                try {
                    CronTriggerImpl cronTrigger = new CronTriggerImpl(triggerNode.getIdentifier(), null, cronExpression);
                    if (nextFireTime != null) {
                        cronTrigger.setNextFireTime(nextFireTime.getTime());
                    }
                    cronTrigger.setJobName(triggerNode.getParent().getParent().getIdentifier());
                    trigger = cronTrigger;
                } catch (ParseException e) {
                    log.warn("Failed to create cron trigger from node {}: invalid cron expression {}",
                            triggerNode.getPath(), cronExpression);
                }
            }
        } else {
            log.warn("Cannot create trigger of unknown type {}", triggerType);
        }
        return trigger;
    }

    private NodeIterable getPendingTriggers(final Session session, long noLaterThan) {
        try {
            session.refresh(true);
            // make sure the index is up to date
            final java.util.Calendar cal = dateToCalendar(new Date(noLaterThan));
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
        log.debug("Trying to obtain lock on {}", nodePath);
        final HippoLockManager lockManager = (HippoLockManager) session.getWorkspace().getLockManager();
        if (!lockManager.isLocked(nodePath) || lockManager.expireLock(nodePath)) {
            try {
                ensureIsLockable(session, nodePath);
                lockManager.lock(nodePath, false, false, lockTimeout, getClusterNodeId(session));
                log.debug("Lock successfully obtained on {}", nodePath);
                return true;
            } catch (LockException e) {
                // happens when other cluster node beat us to it
                log.debug("Failed to set lock on {}: {}", nodePath, e.getMessage());
            }
        } else {
            log.debug("Already locked: {}", nodePath);
        }
        return false;
    }

    private void unlock(Session session, String nodePath) throws RepositoryException {
        log.debug("Trying to release lock on {}", nodePath);
        try {
            final LockManager lockManager = session.getWorkspace().getLockManager();
            if (lockManager.isLocked(nodePath)) {
                final Lock lock = lockManager.getLock(nodePath);
                if (lock.isLockOwningSession()) {
                    lockManager.unlock(nodePath);
                    log.debug("Lock successfully released on {}", nodePath);
                } else {
                    log.debug("We don't own the lock on {}", nodePath);
                }
            } else {
                log.debug("Not locked {}", nodePath);
            }
        } catch (RepositoryException e) {
            log.error("Failed to release lock on {}", nodePath, e);
        }
    }

    private void refreshLock(final Session session, String identifier) throws RepositoryException {
        synchronized (session) {
            final Node node = session.getNodeByIdentifier(identifier);
            final LockManager lockManager = session.getWorkspace().getLockManager();
            final Lock lock = lockManager.getLock(node.getPath());
            lock.refresh();
            log.debug("Lock successfully refreshed");
        }
    }

    private void startLockKeepAlive(final Session session, final String identifier) {
        final long refreshInterval = lockTimeout / 2;
        final Future<?> future = executorService.scheduleAtFixedRate(new Runnable() {
            private int failedAttempts = 0;
            private boolean cancelled = false; // REPO-1268 additional check in case cancellation of future somehow did not work
            @Override
            public void run() {
                if (cancelled) {
                    return;
                }
                try {
                    refreshLock(session, identifier);
                    failedAttempts = 0;
                } catch (RepositoryException e) {
                    log.warn("Failed to refresh lock: {}", e.getMessage());
                    failedAttempts++;
                    if (failedAttempts > 1) {
                        log.warn("Cancelling keep alive after {} attempts to refresh lock", failedAttempts);
                        if (!stopLockKeepAlive(identifier)) {
                            log.warn("Cancelling keep alive job failed");
                        }
                        cancelled = true;
                    }
                }
            }
        }, refreshInterval, refreshInterval, TimeUnit.SECONDS);
        keepAlives.put(identifier, future);
    }

    private boolean stopLockKeepAlive(final String identifier) {
        final Future<?> future = keepAlives.remove(identifier);
        if (future != null) {
            return future.cancel(true);
        }
        return false;
    }

    Map<String, Future<?>> getLockKeepAlives() {
        return keepAlives;
    }

    private static void ensureIsLockable(Session session, String nodePath) throws RepositoryException {
        final Node node = session.getNode(nodePath);
        if (!node.isNodeType(JcrConstants.MIX_LOCKABLE)) {
            node.addMixin(JcrConstants.MIX_LOCKABLE);
        }
        session.save();
    }


    private static java.util.Calendar dateToCalendar(Date date) {
        final java.util.Calendar result = java.util.Calendar.getInstance();
        result.setTime(date);
        return result;
    }

    private static void refreshSession(Session session) {
        try {
            session.refresh(false);
        } catch (RepositoryException e) {
            log.error("Failed to refresh session", e);
        }
    }

    private Session getSession() {
        if (session != null) {
            return session;
        }
        return SchedulerModule.getSession();
    }
}
