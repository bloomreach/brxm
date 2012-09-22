/*
 *  Copyright 2008-2012 Hippo.
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
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionManager;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.commons.iterator.NodeIterable;
import org.apache.jackrabbit.util.ISO8601;
import org.hippoecm.repository.quartz.workflow.WorkflowJobDetail;
import org.hippoecm.repository.util.JcrUtils;
import org.quartz.JobDetail;
import org.quartz.JobPersistenceException;
import org.quartz.SchedulerConfigException;
import org.quartz.Trigger;
import org.quartz.core.SchedulingContext;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.spi.TriggerFiredBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JCRJobStore extends AbstractJobStore {

    private static final Logger log = LoggerFactory.getLogger(SchedulerModule.class);

    private static final String HIPPOSCHED_DATA = "hipposched:data";
    private static final String HIPPOSCHED_FIRETIME = "hipposched:fireTime";
    private static final String HIPPOSCHED_NEXTFIRETIME = "hipposched:nextFireTime";
    private static final String HIPPOSCHED_TRIGGERS = "hipposched:triggers";
    private static final String HIPPOSCHED_TRIGGER = "hipposched:trigger";

    private static final long TWO_MINUTES = 60 * 2;

    private boolean clustered = false;

    @Override
    public void initialize(ClassLoadHelper loadHelper, SchedulerSignaler signaler) throws SchedulerConfigException {
        signaler.signalSchedulingChange(1000);
    }

    @Override
    public boolean isClustered() {
        return clustered;
    }

    public void setIsClustered(boolean flag) {
        clustered = flag;
    }

    /**
     * Store a job as a JCR node.  Invoked with the scheduler context of the invoking session.
     */
    @Override
    public void storeJobAndTrigger(SchedulingContext ctxt, JobDetail newJob, Trigger newTrigger) throws JobPersistenceException {
        if (!(newJob instanceof JCRJobDetail)) {
            throw new JobPersistenceException("JobDetail must be of type JCRJobDetail");
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

                final Node triggerNode = triggersNode.addNode(newTrigger.getName(), HIPPOSCHED_TRIGGER);
                triggerNode.addMixin("mix:lockable");

                final Calendar fireTime = getCalendarInstance(newTrigger.getNextFireTime());
                triggerNode.setProperty(HIPPOSCHED_NEXTFIRETIME, fireTime);
                triggerNode.setProperty(HIPPOSCHED_FIRETIME, fireTime);
                triggerNode.setProperty(HIPPOSCHED_DATA, JcrUtils.createBinaryValueFromObject(session, newTrigger));

                session.save();
            } catch (RepositoryException ex) {
                refreshSession(session);
                final String message = "Failed to store job and trigger";
                log.error(message, ex);
                throw new JobPersistenceException(message, ex);
            }
        }
    }

    @Override
    public JobDetail retrieveJob(SchedulingContext ctxt, String jobIdentifier, String groupName) throws JobPersistenceException {
        final Session session = getSession(ctxt);
        synchronized (session) {
            try {
                final Node jobNode = session.getNodeByIdentifier(jobIdentifier);
                final Value jobDataValue = jobNode.getProperty(HIPPOSCHED_DATA).getValue();
                JobDetail jobDetail = (JobDetail) createObjectFromBinaryValue(jobDataValue);
                if (!(jobDetail instanceof JCRJobDetail)) {
                    // Pre 7.8 backwards compatibility
                    jobDetail = new WorkflowJobDetail(jobNode, jobDetail.getJobDataMap());
                }
                return jobDetail;
            } catch(RepositoryException e) {
                refreshSession(session);
                final String message = "Failed to retrieve job";
                log.error(message, e);
                throw new JobPersistenceException(message, e);
            } catch(IOException e) {
                throw new JobPersistenceException("Failed to read object while retrieving job", e);
            } catch(ClassNotFoundException e) {
                throw new JobPersistenceException("Failed to recreate job", e);
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
                                triggers.add(createTriggerFromNode(triggerNode));
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
                throw new JobPersistenceException("No such job: " + jobIdentifier);
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
                for(Node triggerNode : getPendingTriggers(session, noLaterThan)) {
                    if(triggerNode != null) {
                        if (lock(session, triggerNode.getPath())) {
                            ensureIsCheckedOut(triggerNode);
                            final Trigger trigger = createTriggerFromNode(triggerNode);
                            triggerNode.getProperty(HIPPOSCHED_NEXTFIRETIME).remove();
                            session.save();
                            return trigger;
                        }
                    }
                }
            } catch(RepositoryException ex) {
                refreshSession(session);
                final String message = "Failed to acquire next trigger";
                log.error(message, ex);
                throw new JobPersistenceException(message, ex);
            } catch(IOException ex) {
                log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
                throw new JobPersistenceException("data format exception while acquiring next trigger", ex);
            } catch(ClassNotFoundException ex) {
                log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
                throw new JobPersistenceException("cannot recreate trigger", ex);
            }
        }
        return null;
    }

    private Trigger createTriggerFromNode(final Node triggerNode) throws RepositoryException, ClassNotFoundException, IOException {
        final Trigger trigger = (Trigger) createObjectFromBinaryValue(triggerNode.getProperty(HIPPOSCHED_DATA).getValue());
        trigger.setName(triggerNode.getIdentifier());
        trigger.setJobName(triggerNode.getParent().getParent().getIdentifier());
        return trigger;
    }

    @Override
    public void releaseAcquiredTrigger(SchedulingContext ctxt, Trigger trigger) throws JobPersistenceException {
        final Session session = getSession(ctxt);
        synchronized (session) {
            try {
                final String triggerIdentifier = trigger.getName();
                final Node triggerNode = session.getNodeByIdentifier(triggerIdentifier);
                triggerNode.setProperty(HIPPOSCHED_NEXTFIRETIME, triggerNode.getProperty(HIPPOSCHED_FIRETIME).getValue());
                session.save();
                unlock(session, triggerNode.getPath());
            } catch (PathNotFoundException ex) {
                log.warn(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            } catch (ItemNotFoundException ex) {
                log.warn(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            } catch (RepositoryException ex) {
                refreshSession(session);
                final String message = "Failed to release acquired trigger";
                log.error(message, ex);
                throw new JobPersistenceException(message, ex);
            }
        }
    }

    @Override
    public TriggerFiredBundle triggerFired(SchedulingContext ctxt, Trigger trigger) throws JobPersistenceException {
        return new TriggerFiredBundle(retrieveJob(ctxt, trigger.getJobName(), trigger.getJobGroup()),
                                      trigger, null, false,
                                      trigger.getPreviousFireTime(), trigger.getPreviousFireTime(),
                                      trigger.getPreviousFireTime(), trigger.getNextFireTime());
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
                final Node triggerNode = session.getNodeByIdentifier(triggerIdentifier);
    
                final Date nextFire = trigger.getFireTimeAfter(new Date());
                if(nextFire != null) {
                    ensureIsCheckedOut(triggerNode);
                    final Calendar nextFireTime = getCalendarInstance(nextFire);
                    triggerNode.setProperty(HIPPOSCHED_FIRETIME, nextFireTime);
                    triggerNode.setProperty(HIPPOSCHED_NEXTFIRETIME, nextFireTime);
                    session.save();
                    unlock(session, triggerNode.getPath());
                } else {
                    final String jobIdentifier = ((JCRJobDetail) jobDetail).getIdentifier();
                    final Node jobNode = session.getNodeByIdentifier(jobIdentifier);
                    ensureIsCheckedOut(jobNode.getParent());
                    jobNode.remove();
                    session.save();
                }
            } catch (ItemNotFoundException ex) {
                log.warn("Unable to find job node to be completed for trigger uuid '" + trigger.getName() + "'");
            } catch (RepositoryException ex) {
                refreshSession(session);
                final String message = "Failed to complete triggered job";
                log.error(message, ex);
                throw new JobPersistenceException(message, ex);
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
            log.error(e.getClass().getName() + ": " + e.getMessage(), e);
        }
    }

    private static void ensureIsCheckedOut(Node node) throws RepositoryException {
        if (node.isNodeType("mix:versionable") && !node.isCheckedOut()) {
            final VersionManager versionManager = node.getSession().getWorkspace().getVersionManager();
            versionManager.checkout(node.getPath());
        }
    }

    private static Object createObjectFromBinaryValue(final Value value) throws RepositoryException, IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(value.getBinary().getStream());
        try {
            return ois.readObject();
        } finally {
            IOUtils.closeQuietly(ois);
        }
    }

    private static Calendar getCalendarInstance(Date date) {
        final Calendar result = Calendar.getInstance();
        result.setTime(date);
        return result;
    }

    private static NodeIterable getPendingTriggers(Session session, long noLaterThan) throws RepositoryException {
        final Calendar cal = getCalendarInstance(new Date(noLaterThan));
        final QueryManager qMgr = session.getWorkspace().getQueryManager();
        final Query query = qMgr.createQuery(
                "SELECT * FROM hipposched:trigger WHERE hipposched:nextFireTime <= TIMESTAMP '"
                        + ISO8601.format(cal) + "' ORDER BY hipposched:nextFireTime", Query.SQL);
        final QueryResult result = query.execute();
        return new NodeIterable(result.getNodes());
    }

    private static boolean lock(Session session, String nodePath) throws RepositoryException {
        log.debug("Trying to obtain lock on " + nodePath);
        final LockManager lockManager = session.getWorkspace().getLockManager();
        if (!lockManager.isLocked(nodePath)) {
            try {
                ensureIsLockable(session, nodePath);
                lockManager.lock(nodePath, false, false, TWO_MINUTES, getClusterNodeId(session));
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

    private static void unlock(Session session, String nodePath) throws RepositoryException {
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

    private static void ensureIsLockable(Session session, String nodePath) throws RepositoryException {
        final Node node = session.getNode(nodePath);
        if (!node.isNodeType("mix:lockable")) {
            node.addMixin("mix:lockable");
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
