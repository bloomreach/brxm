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
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_JOBGROUP;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_REPOSITORY_JOB;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_REPOSITORY_JOB_CLASS;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_SIMPLE_TRIGGER;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_STARTTIME;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_TRIGGERS;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

public class JCRJobStoreTest extends RepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        session.getRootNode().addNode("test");
        session.save();
    }

    @Test
    public void testStoreJobAndTrigger() throws Exception {
        final JCRJobStore store = new JCRJobStore(session);
        final Node jobNode = createAndStoreJobAndSimpleTrigger(store);

        assertTrue(jobNode.hasNode("hipposched:triggers"));
        assertTrue(jobNode.hasNode("hipposched:triggers/trigger"));
        assertTrue(jobNode.hasProperty("hipposched:triggers/trigger/hipposched:nextFireTime"));
    }

    @Test
    public void testAcquireNextTrigger() throws Exception {
        final JCRJobStore store = new JCRJobStore(session);
        final Node jobNode = createAndStoreJobAndSimpleTrigger(store);
        final List<OperableTrigger> triggers = store.acquireNextTriggers(System.currentTimeMillis(), 1, -1l);
        assertNotNull(triggers);
        assertFalse(triggers.isEmpty());
        assertEquals(1, triggers.size());
        assertTrue(jobNode.getNode("hipposched:triggers/trigger").isLocked());
    }

    @Test
    public void testAcquireNextTriggerAndRelease() throws Exception {
        final JCRJobStore store = new JCRJobStore(session);
        final Node jobNode = createAndStoreJobAndSimpleTrigger(store);
        final List<OperableTrigger> triggers = store.acquireNextTriggers(System.currentTimeMillis(), 1, -1l);
        assumeNotNull(triggers);
        assumeTrue(!triggers.isEmpty());
        store.releaseAcquiredTrigger(triggers.get(0));
        assertFalse(jobNode.getNode("hipposched:triggers/trigger").isLocked());
    }

    @Test
    public void testTriggeredJobCompleteSimple() throws Exception {
        final JCRJobStore store = new JCRJobStore(session);
        final Node jobNode = createAndStoreJobAndSimpleTrigger(store);
        final String jobNodePath = jobNode.getPath();
        final JobDetail jobDetail = store.retrieveJob(new JobKey(jobNode.getIdentifier()));
        final List<OperableTrigger> triggers = store.acquireNextTriggers(System.currentTimeMillis(), 1, -1l);
        assumeNotNull(triggers);
        assumeTrue(!triggers.isEmpty());
        store.triggeredJobComplete(triggers.get(0), jobDetail, null);
        // when the job was completed and the trigger doesn't have a next fire time, the job should be deleted
        assertFalse(session.nodeExists(jobNodePath));
    }

    @Test
    public void testTriggeredJobCompleteRepeated() throws Exception {
        final JCRJobStore store = new JCRJobStore(session);
        final Node jobNode = createAndStoreJobAndRepeatedTrigger(store);
        final String jobNodePath = jobNode.getPath();
        final JobDetail jobDetail = store.retrieveJob(new JobKey(jobNode.getIdentifier()));
        final List<OperableTrigger> triggers = store.acquireNextTriggers(System.currentTimeMillis(), 1, -1l);
        assumeNotNull(triggers);
        assumeTrue(!triggers.isEmpty());
        store.triggeredJobComplete(triggers.get(0), jobDetail, null);
        // when the job was completed and the trigger has a next fire time, the job should not be deleted
        assertTrue(session.nodeExists(jobNodePath));
        assertTrue(jobNode.hasProperty("hipposched:triggers/trigger/hipposched:nextFireTime"));
    }

    @Test
    public void testGetTriggersForJob() throws Exception {
        final JCRJobStore store = new JCRJobStore(session);
        final Node jobNode = createAndStoreJobAndSimpleTrigger(store);
        final List<OperableTrigger> triggersForJob = store.getTriggersForJob(new JobKey(jobNode.getIdentifier()));
        assertNotNull(triggersForJob);
        assertEquals(1, triggersForJob.size());
    }

    @Test
    public void testTriggerLockKeepAlive() throws Exception {
        final JCRJobStore store = new JCRJobStore(10, session);
        final Node jobNode = createAndStoreJobAndSimpleTrigger(store);
        final List<OperableTrigger> triggers = store.acquireNextTriggers(System.currentTimeMillis(), 1, -1l);
        Thread.sleep(1000*12); // sleep longer than lock timeout
        assertTrue(jobNode.getNode("hipposched:triggers/trigger").isLocked());
        store.releaseAcquiredTrigger(triggers.get(0));
        assertFalse(jobNode.getNode("hipposched:triggers/trigger").isLocked());
    }

    @Test
    public void testConfigureJobInRepository() throws Exception {
        final Node test = session.getNode("/test");
        final Node groupNode = test.addNode("group", HIPPOSCHED_JOBGROUP);
        final Node jobNode = groupNode.addNode("job", HIPPOSCHED_REPOSITORY_JOB);
        jobNode.setProperty(HIPPOSCHED_REPOSITORY_JOB_CLASS, "<class>");
        final Node triggerNode = jobNode.addNode(HIPPOSCHED_TRIGGERS, HIPPOSCHED_TRIGGERS).addNode("trigger", HIPPOSCHED_SIMPLE_TRIGGER);
        triggerNode.setProperty(HIPPOSCHED_STARTTIME, Calendar.getInstance());
        session.save();
        final JCRJobStore store = new JCRJobStore(10, session, "/test");
        store.initialize(null, null);
        // wait until initialization of triggers is saved on separate thread
        Thread.sleep(1000l);
        final List<OperableTrigger> triggers = store.acquireNextTriggers(System.currentTimeMillis(), 1, -1l);
        assertEquals(1, triggers.size());
        final OperableTrigger trigger = triggers.get(0);
        assertTrue(trigger instanceof SimpleTrigger);
        SimpleTrigger simpleTrigger = (SimpleTrigger) trigger;
        assertTrue(simpleTrigger.getJobKey().getName().equals(jobNode.getIdentifier()));
        final JobDetail jobDetail = store.retrieveJob(simpleTrigger.getJobKey());
        assertTrue(jobDetail instanceof RepositoryJobDetail);
        final RepositoryJobDetail repositoryJobDetail = (RepositoryJobDetail) jobDetail;
        assertEquals("<class>", repositoryJobDetail.getRepositoryJobClassName());
    }

    private Node createAndStoreJobAndRepeatedTrigger(final JCRJobStore store) throws RepositoryException, JobPersistenceException {
        final Node jobNode = session.getNode("/test").addNode("job", HIPPOSCHED_REPOSITORY_JOB);
        jobNode.setProperty(HIPPOSCHED_REPOSITORY_JOB_CLASS, Job.class.getName());
        final JobDetail jobDetail = new RepositoryJobDetail(jobNode);
        final SimpleTriggerImpl trigger = new SimpleTriggerImpl("trigger", 2, 1000);
        trigger.setNextFireTime(new Date());
        store.storeJobAndTrigger(jobDetail, trigger);
        return jobNode;
    }

    private Node createAndStoreJobAndSimpleTrigger(final JCRJobStore store) throws RepositoryException, JobPersistenceException {
        final Node jobNode = session.getNode("/test").addNode("job", HIPPOSCHED_REPOSITORY_JOB);
        jobNode.setProperty(HIPPOSCHED_REPOSITORY_JOB_CLASS, Job.class.getName());
        final JobDetail jobDetail = new RepositoryJobDetail(jobNode);
        final SimpleTriggerImpl trigger = new SimpleTriggerImpl("trigger");
        trigger.setNextFireTime(new Date());
        store.storeJobAndTrigger(jobDetail, trigger);
        return jobNode;
    }

}
