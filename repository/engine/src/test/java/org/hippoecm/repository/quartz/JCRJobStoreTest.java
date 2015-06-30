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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.EventIterator;

import org.hippoecm.repository.api.SynchronousEventListener;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.SimpleTrigger;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.quartz.spi.OperableTrigger;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_JOBGROUP;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_NEXTFIRETIME;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_REPOSITORY_JOB;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_REPOSITORY_JOB_CLASS;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_SIMPLE_TRIGGER;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_STARTTIME;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_TRIGGERS;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_WORKFLOW_JOB;
import static org.hippoecm.repository.util.JcrUtils.ALL_EVENTS;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

public class JCRJobStoreTest extends RepositoryTestCase {

    private final TriggerUpdateListener listener = new TriggerUpdateListener();
    private JCRJobStore store;
    private Session storeSession;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        session.getRootNode().addNode("test");
        session.save();
        session.getWorkspace().getObservationManager().addEventListener(listener, ALL_EVENTS, "/test", true, null, null, false);
        storeSession = session.impersonate(new SimpleCredentials("admin", new char[]{}));
        store = new JCRJobStore(10, storeSession, "/test");
        store.initialize(null, null);
    }

    @Override
    public void tearDown() throws Exception {
        store.shutdown();
        storeSession.logout();
        session.refresh(false);
        super.tearDown();
    }

    @Test
    public void testStoreJobAndTrigger() throws Exception {
        final Node jobNode = createAndStoreJobAndSimpleTrigger(store);
        assertTrue(jobNode.hasNode("hipposched:triggers"));
        assertTrue(jobNode.hasNode("hipposched:triggers/trigger"));
        assertTrue(jobNode.hasProperty("hipposched:triggers/trigger/hipposched:nextFireTime"));
    }

    @Test
    public void testAcquireNextTrigger() throws Exception {
        final Node jobNode = createAndStoreJobAndSimpleTrigger(store);
        listener.reset();
        final List<OperableTrigger> triggers = store.acquireNextTriggers(System.currentTimeMillis(), 1, -1l);
        assertNotNull(triggers);
        assertFalse(triggers.isEmpty());
        assertEquals(1, triggers.size());
        assertTrue(jobNode.getNode("hipposched:triggers/trigger").isLocked());
        assertFalse(listener.hasTriggerUpdateEvents);
    }

    @Test
    public void testAcquireNextTriggerAndRelease() throws Exception {
        final Node jobNode = createAndStoreJobAndSimpleTrigger(store);
        listener.reset();
        final List<OperableTrigger> triggers = store.acquireNextTriggers(System.currentTimeMillis(), 1, -1l);
        assumeNotNull(triggers);
        assumeTrue(!triggers.isEmpty());
        store.releaseAcquiredTrigger(triggers.get(0));
        assertFalse(jobNode.getNode("hipposched:triggers/trigger").isLocked());
        assertFalse(listener.hasTriggerUpdateEvents);
    }

    @Test
    public void testCompletedWorkflowJobIsRemoved() throws Exception {
        final String jobNodePath = testTriggeredJobCompleteSimple(HIPPOSCHED_WORKFLOW_JOB);
        assertFalse(session.nodeExists(jobNodePath));
    }

    @Test
    public void testCompletedRepositoryJobIsNotRemoved() throws Exception {
        final String jobNodePath = testTriggeredJobCompleteSimple(HIPPOSCHED_REPOSITORY_JOB);
        assertTrue(session.nodeExists(jobNodePath));
    }

    private String testTriggeredJobCompleteSimple(final String type) throws RepositoryException, JobPersistenceException {
        final Node jobNode = createAndStoreJobAndSimpleTrigger(store, type);
        final String jobNodePath = jobNode.getPath();
        final JobDetail jobDetail = store.retrieveJob(new JobKey(jobNode.getIdentifier()));
        final List<OperableTrigger> triggers = store.acquireNextTriggers(System.currentTimeMillis(), 1, -1l);
        assumeNotNull(triggers);
        assumeTrue(!triggers.isEmpty());
        store.triggeredJobComplete(triggers.get(0), jobDetail, null);
        return jobNodePath;
    }

    @Test
    public void testTriggeredJobCompleteRepeated() throws Exception {
        final Node jobNode = createAndStoreJobAndRepeatedTrigger(store);
        final String jobNodePath = jobNode.getPath();
        final JobDetail jobDetail = store.retrieveJob(new JobKey(jobNode.getIdentifier()));
        final List<OperableTrigger> triggers = store.acquireNextTriggers(System.currentTimeMillis(), 1, -1l);
        assumeNotNull(triggers);
        assumeTrue(!triggers.isEmpty());
        final OperableTrigger trigger = triggers.get(0);
        final SimpleTrigger simpleTrigger = (SimpleTrigger) trigger;
        final int repeatCount = simpleTrigger.getRepeatCount();
        store.triggeredJobComplete(trigger, jobDetail, null);
        // when the job was completed and the trigger has a next fire time, the job should not be deleted
        assertTrue(session.nodeExists(jobNodePath));
        Node triggerNode = jobNode.getNode("hipposched:triggers/trigger");
        assertTrue(triggerNode.hasProperty("hipposched:nextFireTime"));
        assertTrue(triggerNode.hasProperty("hipposched:repeatCount"));
        assertEquals(repeatCount-1, triggerNode.getProperty("hipposched:repeatCount").getLong());
    }

    @Test
    public void testGetTriggersForJob() throws Exception {
        final Node jobNode = createAndStoreJobAndSimpleTrigger(store);
        final List<OperableTrigger> triggersForJob = store.getTriggersForJob(new JobKey(jobNode.getIdentifier()));
        assertNotNull(triggersForJob);
        assertEquals(1, triggersForJob.size());
    }

    @Test
    public void testTriggerLockKeepAlive() throws Exception {
        final Node jobNode = createAndStoreJobAndSimpleTrigger(store);
        final List<OperableTrigger> triggers = store.acquireNextTriggers(System.currentTimeMillis(), 1, -1l);
        Thread.sleep(1000*12); // sleep longer than lock timeout
        assertTrue(jobNode.getNode("hipposched:triggers/trigger").isLocked());
        store.releaseAcquiredTrigger(triggers.get(0));
        assertFalse(jobNode.getNode("hipposched:triggers/trigger").isLocked());
    }

    @Test
    public void testTriggerLockKeepAliveIsCancelledWhenRefreshingLockFails() throws Exception {
        final Node jobNode = createAndStoreJobAndSimpleTrigger(store);
        final List<OperableTrigger> triggers = store.acquireNextTriggers(System.currentTimeMillis(), 1, -1l);
        final Node triggerNode = jobNode.getNode("hipposched:triggers/trigger");
        triggerNode.unlock();
        Thread.sleep(1000*12); // sleep longer than twice the refresh interval
        for (int i = 0; i < 10; i++) {
            if (!store.getLockKeepAlives().containsKey(triggerNode.getIdentifier())) {
                return;
            }
            Thread.sleep(1000l);
        }
        fail("Keep alive was not cancelled");
    }

    @Test
    public void testConfigureJobInRepository() throws Exception {
        final Node test = session.getNode("/test");
        final Node groupNode = test.addNode("group", HIPPOSCHED_JOBGROUP);
        final Node jobNode = groupNode.addNode("job", HIPPOSCHED_REPOSITORY_JOB);
        jobNode.setProperty(HIPPOSCHED_REPOSITORY_JOB_CLASS, "<class>");
        final Node triggerNode = jobNode.addNode(HIPPOSCHED_TRIGGERS, HIPPOSCHED_TRIGGERS).addNode("trigger", HIPPOSCHED_SIMPLE_TRIGGER);
        final Calendar startTime = Calendar.getInstance();
        startTime.setTime(new Date(System.currentTimeMillis()+30*60*1000));
        triggerNode.setProperty(HIPPOSCHED_STARTTIME, startTime);
        session.save();
        assertTrue(listener.hasTriggerUpdateEvents);
        listener.waitForEvents();
        listener.reset();
        // added trigger should have gotten a next fire time property
        assertTrue(triggerNode.hasProperty(HIPPOSCHED_NEXTFIRETIME));
        Calendar nextFireTime = triggerNode.getProperty(HIPPOSCHED_NEXTFIRETIME).getDate();

        // changing the start time updates the trigger
        startTime.setTime(new Date(System.currentTimeMillis()+60*60*1000));
        triggerNode.setProperty(HIPPOSCHED_STARTTIME, startTime);
        session.save();
        assertTrue(listener.hasTriggerUpdateEvents);
        listener.waitForEvents();
        listener.reset();
        assertFalse(nextFireTime.equals(triggerNode.getProperty(HIPPOSCHED_NEXTFIRETIME).getDate()));

        // setting the next fire time does not update the trigger
        nextFireTime.setTime(new Date(System.currentTimeMillis()+90*60*1000));
        triggerNode.setProperty(HIPPOSCHED_NEXTFIRETIME, nextFireTime);
        assertFalse(listener.hasTriggerUpdateEvents);
    }

    private Node createAndStoreJobAndRepeatedTrigger(final JCRJobStore store) throws RepositoryException, JobPersistenceException {
        final Node jobNode = session.getNode("/test").addNode("job", HIPPOSCHED_REPOSITORY_JOB);
        jobNode.setProperty(HIPPOSCHED_REPOSITORY_JOB_CLASS, Job.class.getName());
        session.save();
        final JobDetail jobDetail = new RepositoryJobDetail(jobNode);
        final SimpleTriggerImpl trigger = new SimpleTriggerImpl("trigger", 2, 1000);
        trigger.setNextFireTime(new Date());
        store.storeJobAndTrigger(jobDetail, trigger);
        return jobNode;
    }

    private Node createAndStoreJobAndSimpleTrigger(final JCRJobStore store) throws RepositoryException, JobPersistenceException {
        return createAndStoreJobAndSimpleTrigger(store, HIPPOSCHED_REPOSITORY_JOB);
    }

    private Node createAndStoreJobAndSimpleTrigger(final JCRJobStore store, final String type) throws RepositoryException, JobPersistenceException {
        final Node jobNode = session.getNode("/test").addNode("job", type);
        jobNode.setProperty(HIPPOSCHED_REPOSITORY_JOB_CLASS, Job.class.getName());
        session.save();
        final RepositoryJobDetail jobDetail = new RepositoryJobDetail(jobNode);
        final SimpleTriggerImpl trigger = new SimpleTriggerImpl("trigger");
        trigger.setNextFireTime(new Date());
        store.storeJobAndTrigger(jobDetail, trigger);
        return jobNode;
    }

    private static final class TriggerUpdateListener implements SynchronousEventListener {

        private volatile boolean hasTriggerUpdateEvents;
        private volatile boolean eventsArrived;

        @Override
        public void onEvent(final EventIterator events) {
            eventsArrived = true;
            hasTriggerUpdateEvents |= JCRJobStore.hasTriggerUpdateEvents(events);
        }

        private void waitForEvents() {
            eventsArrived = false;
            int count = 0;
            while (!eventsArrived && count++ < 50) {
                try {
                    Thread.sleep(10l);
                } catch (InterruptedException ignore) {
                }
            }
            if (count == 50) {
                throw new AssertionError("Expected events did not arrive");
            }
        }

        private void reset() {
            hasTriggerUpdateEvents = false;
        }
    }
}
