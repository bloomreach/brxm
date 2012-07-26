/*
 *  Copyright 2008 Hippo.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Calendar;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.util.ISO8601;
import org.quartz.JobDetail;
import org.quartz.JobPersistenceException;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.SchedulerConfigException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.core.SchedulingContext;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.JobStore;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.spi.TriggerFiredBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JCRJobStore implements JobStore {

    static final Logger log = LoggerFactory.getLogger(SchedulerModule.class);

    private boolean clustered = false;
    private String clusteredInstanceName = null;
    private String clusteredInstanceId = null;

    public void initialize(ClassLoadHelper loadHelper, SchedulerSignaler signaler) throws SchedulerConfigException {
        signaler.signalSchedulingChange(1000);
    }

    public void setUseProperties(boolean flag) {
        // FIXME
    }
    public void setClusterCheckinInterval(long interval) {
        // FIXME
    }
    public void setIsClustered(boolean flag) {
        this.clustered = flag;
    }
    public void setMisfireThreshold(long threshold) {
        // FIXME
    }

    public void schedulerStarted() throws SchedulerException {
    }

    public void shutdown() {
    }

    public boolean supportsPersistence() {
        return true;
    }

    public long getEstimatedTimeToReleaseAndAcquireTrigger() {
        return 0;  // FIXME
    }

    public boolean isClustered() {
        return this.clustered;
    }

    /**
     * Store a job as a JCR node.  Invoked with the scheduler context of the invoking session.
     */
    public void storeJobAndTrigger(SchedulingContext ctxt, JobDetail newJob, Trigger newTrigger)
      throws ObjectAlreadyExistsException, JobPersistenceException {
        if (log.isDebugEnabled()) {
            log.trace("trace");
        }
        Session session = ((JCRSchedulingContext)ctxt).getSession();
        synchronized(session) {
            try {
                Node jobNode = session.getRootNode().getNode(newJob.getName().substring(1));
                jobNode.setProperty("hipposched:data",new ByteArrayInputStream(objectToBytes(newJob)));
                jobNode.setProperty("hippo:document",(String) newJob.getJobDataMap().get("document"));
                if(!jobNode.hasNode("hipposched:triggers")) {
                    jobNode.addNode("hipposched:triggers","hipposched:triggers");
                }
                String triggerRelPath = newTrigger.getName().substring(newJob.getName().length()+1);
                Node triggerNode = jobNode.getNode("hipposched:triggers").addNode(triggerRelPath, "hipposched:trigger");
                Calendar cal = Calendar.getInstance();
                cal.setTime(newTrigger.getNextFireTime());
                triggerNode.setProperty("hipposched:nextFireTime", cal);
                triggerNode.setProperty("hipposched:fireTime", cal);
                triggerNode.setProperty("hipposched:data", new ByteArrayInputStream(objectToBytes(newTrigger)));
                jobNode.getParent().save();
            } catch (RepositoryException ex) {
                log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
                throw new JobPersistenceException("Failure storing job and trigger", ex);
            }
        }
    }

    private byte[] objectToBytes(Object o) throws RepositoryException {
        try {
            ByteArrayOutputStream store = new ByteArrayOutputStream();
            ObjectOutputStream ostream = new ObjectOutputStream(store);
            ostream.writeObject(o);
            ostream.flush();
            return store.toByteArray();
        } catch (IOException ex) {
            throw new ValueFormatException(ex);
        }
    }

    public void storeJob(SchedulingContext ctxt, JobDetail newJob, boolean replaceExisting)
      throws ObjectAlreadyExistsException, JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
    }

    public boolean removeJob(SchedulingContext ctxt, String jobName, String groupName) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
        return false;
    }

    public JobDetail retrieveJob(SchedulingContext ctxt, String jobName, String groupName) throws JobPersistenceException {
       if(log.isDebugEnabled()) {
            log.trace("trace");
        }
        try {
            Session session = getSession(ctxt);
            synchronized (session) {
                session.refresh(false);

                Node jobNode = null;
                if (jobName.startsWith("/")) {
                    jobNode = session.getRootNode().getNode(jobName.substring(1));
                } else {
                    jobNode = session.getNodeByUUID(jobName);
                }
                if(jobNode != null) {
                    Object o = new ObjectInputStream(jobNode.getProperty("hipposched:data").getStream()).readObject();
                    JobDetail job = (JobDetail) o;
                    job.setName(jobNode.getUUID());
                    return job;
                }
            }
        } catch(RepositoryException ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
            throw new JobPersistenceException("error while retrieving job", ex);
        } catch(IOException ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
            throw new JobPersistenceException("data format while retrieving job", ex);
        } catch(ClassNotFoundException ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
            throw new JobPersistenceException("cannot recreate job", ex);
        }
        return null;
    }

    public void storeTrigger(SchedulingContext ctxt, Trigger newTrigger, boolean replaceExisting)
      throws ObjectAlreadyExistsException, JobPersistenceException {
       if(log.isDebugEnabled()) {
            log.trace("trace");
        }
    }

    public boolean removeTrigger(SchedulingContext ctxt, String triggerName, String groupName) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
        return false;
    }

    public boolean replaceTrigger(SchedulingContext ctxt, String triggerName, String groupName, Trigger newTrigger)
      throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
        return false;
    }

    public Trigger retrieveTrigger(SchedulingContext ctxt, String triggerName, String groupName) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
        return null;
    }

    public void storeCalendar(SchedulingContext ctxt, String name, org.quartz.Calendar calendar, boolean replaceExisting,
                              boolean updateTriggers) throws ObjectAlreadyExistsException, JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
    }

    public boolean removeCalendar(SchedulingContext ctxt, String calName) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
        return false;
    }

    public org.quartz.Calendar retrieveCalendar(SchedulingContext ctxt, String calName) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
        return null;
    }

    public int getNumberOfJobs(SchedulingContext ctxt) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
        return 0;
    }

    public int getNumberOfTriggers(SchedulingContext ctxt) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
        return 0;
    }

    public int getNumberOfCalendars(SchedulingContext ctxt) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
        return 0;
    }

    public String[] getJobNames(SchedulingContext ctxt, String groupName) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
        return new String[0];
    }

    public String[] getTriggerNames(SchedulingContext ctxt, String groupName) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
        return new String[0];
    }

    public String[] getJobGroupNames(SchedulingContext ctxt) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
        return new String[0];
    }

    public String[] getTriggerGroupNames(SchedulingContext ctxt) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
        return new String[0];
    }

    public String[] getCalendarNames(SchedulingContext ctxt) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
        return new String[0];
    }

    public Trigger[] getTriggersForJob(SchedulingContext ctxt, String jobName, String groupName) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
        return new Trigger[0];
    }

    public int getTriggerState(SchedulingContext ctxt, String triggerName, String triggerGroup) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
        return 0;
    }

    public void pauseTrigger(SchedulingContext ctxt, String triggerName, String groupName) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
    }

    public void pauseTriggerGroup(SchedulingContext ctxt, String groupName) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
    }

    public void pauseJob(SchedulingContext ctxt, String jobName, String groupName) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
    }

    public void pauseJobGroup(SchedulingContext ctxt, String groupName) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
    }

    public void resumeTrigger(SchedulingContext ctxt, String triggerName, String groupName) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
    }

    public void resumeTriggerGroup(SchedulingContext ctxt, String groupName) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
    }

    public Set getPausedTriggerGroups(SchedulingContext ctxt) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
        return new HashSet();
    }

    public void resumeJob(SchedulingContext ctxt, String jobName, String groupName) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
    }

    public void resumeJobGroup(SchedulingContext ctxt, String groupName) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
    }

    public void pauseAll(SchedulingContext ctxt) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
    }

    public void resumeAll(SchedulingContext ctxt) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
    }

    public Trigger acquireNextTrigger(SchedulingContext ctxt, long noLaterThan) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("acquireNextTrigger({})",noLaterThan);
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(noLaterThan));
        try {
            Session session = getSession(ctxt);
            synchronized (session) {
                session.refresh(false);
                QueryManager qMgr = session.getWorkspace().getQueryManager();
                Query query = qMgr.createQuery(
                        "SELECT * FROM hipposched:trigger WHERE hipposched:nextFireTime <= TIMESTAMP '"
                                + ISO8601.format(cal) + "' ORDER BY hipposched:nextFireTime", Query.SQL);
                QueryResult result = query.execute();
                for(NodeIterator iter = result.getNodes(); iter.hasNext(); ) {
                    Node triggerNode = iter.nextNode();
                    if(triggerNode != null && triggerNode.hasProperty("hipposched:nextFireTime")) {
                        if (triggerNode.isNodeType("mix:versionable") && !triggerNode.isCheckedOut()) {
                            triggerNode.checkout();
                        }
                        Object o = new ObjectInputStream(triggerNode.getProperty("hipposched:data").getStream()).readObject();
                        Trigger trigger = (Trigger)o;
                        trigger.setName(triggerNode.getUUID());
                        trigger.setJobName(triggerNode.getParent().getParent().getUUID());
                        triggerNode.getProperty("hipposched:nextFireTime").remove();
                        /* If saving the trigger node fails, this is most likely due to another node in
                         * a clustered installation picking up the trigger.  This will render the nextFireTime
                         * to be already removed.  In such a case, it is proper to just proceed to the next
                         * possible trigger in the query.
                         */
                        try {
                            session.save();
                            return (Trigger)o;
                        } catch(RepositoryException ex) {
                            session.refresh(false);
                        }
                    } else {
                        triggerNode = null;
                    }
                }
            }
        } catch(RepositoryException ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
            throw new JobPersistenceException("error acquiring next trigger", ex);
        } catch(IOException ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
            throw new JobPersistenceException("data format exception while acquiring next trigger", ex);
        } catch(ClassNotFoundException ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
            throw new JobPersistenceException("cannot recreate trigger", ex);
        }
        return null;
    }

    public void releaseAcquiredTrigger(SchedulingContext ctxt, Trigger trigger) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
        String triggerName = trigger.getName();
        Node triggerNode;
        try {
            Session session = getSession(ctxt);
            synchronized (session) {
                session.refresh(false);
                try {
                    if (triggerName.startsWith("/")) {
                        triggerNode = session.getRootNode().getNode(triggerName.substring(1));
                    } else {
                        triggerNode = session.getNodeByUUID(triggerName);
                    }
                } catch (PathNotFoundException ex) {
                    log.warn(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                    return;
                } catch (ItemNotFoundException ex) {
                    log.warn(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                    return;
                }
                triggerNode.setProperty("hipposched:nextFireTime", triggerNode.getProperty("hipposched:fireTime").getValue());
                triggerNode.save();
            }
        } catch(RepositoryException ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
            throw new JobPersistenceException("error releasing acquired trigger", ex);
        }
    }

    public TriggerFiredBundle triggerFired(SchedulingContext ctxt, Trigger trigger) throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
        return new TriggerFiredBundle(retrieveJob(ctxt, trigger.getJobName(), trigger.getJobGroup()),
                                      trigger, null, false,
                                      trigger.getPreviousFireTime(), trigger.getPreviousFireTime(),
                                      trigger.getPreviousFireTime(), trigger.getNextFireTime());
    }

    public void triggeredJobComplete(SchedulingContext ctxt, Trigger trigger, JobDetail jobDetail, int triggerInstCode)
      throws JobPersistenceException {
        if(log.isDebugEnabled()) {
            log.trace("trace");
        }
        try {
            Session session = getSession(ctxt);
            synchronized (session) {
                session.refresh(false);

                String jobName = jobDetail.getName();
                String triggerName = trigger.getName();
                Node jobNode;
                Node triggerNode;
                if (jobName.startsWith("/")) {
                    jobNode = session.getRootNode().getNode(jobName.substring(1));
                } else {
                    jobNode = session.getNodeByUUID(jobName);
                }
                if (triggerName.startsWith("/")) {
                    triggerNode = session.getRootNode().getNode(triggerName.substring(1));
                } else {
                    triggerNode = session.getNodeByUUID(triggerName);
                }
    
                Date nextFire = trigger.getFireTimeAfter(new Date());
                if(nextFire != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(nextFire);
                    if(triggerNode.isNodeType("mix:versionable") && !triggerNode.isCheckedOut()) {
                        triggerNode.checkout();
                    }
                    triggerNode.setProperty("hipposched:nextFireTime", cal);
                    triggerNode.setProperty("hipposched:fireTime", cal);
                    triggerNode.save();
                } else {
                    if(jobNode != null) {
                        Node handle = jobNode.getParent();
                        if(handle.isNodeType("mix:versionable") && !handle.isCheckedOut()) {
                            handle.checkout();
                        }
                        jobNode.remove();
                        handle.save();
                    }
                }
            }
        } catch(PathNotFoundException ex) {
            // deliberate ignore
        } catch(RepositoryException ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
            throw new JobPersistenceException("error while marking job completed", ex);
        }
    }

    public void setInstanceId(String id) {
        clusteredInstanceId = id;
    }

    public void setInstanceName(String name) {
        clusteredInstanceName = name;
    }

    private Session getSession(SchedulingContext ctxt) throws RepositoryException {
        Session session;
        if(ctxt instanceof JCRSchedulingContext) {
            session = ((JCRSchedulingContext)ctxt).getSession();
        } else {
            session = SchedulerModule.session;
        }
        return session;
    }
}
