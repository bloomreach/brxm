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

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobPersistenceException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.core.SchedulingContext;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

public class JCRJobStoreTest extends RepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        session.getRootNode().addNode("test");
        session.save();
    }

    @Test
    public void testStoreJobAndTrigger() throws Exception {
        final JCRJobStore store = new JCRJobStore();
        final SchedulingContext context = new JCRSchedulingContext(session);
        final Node jobNode = createAndStoreJobAndSimpleTrigger(store, context);

        assertTrue(jobNode.hasNode("hipposched:triggers"));
        assertTrue(jobNode.hasNode("hipposched:triggers/trigger"));
        assertTrue(jobNode.hasProperty("hipposched:triggers/trigger/hipposched:nextFireTime"));
    }

    @Test
    public void testAcquireNextTrigger() throws Exception {
        final JCRJobStore store = new JCRJobStore();
        final SchedulingContext context = new JCRSchedulingContext(session);
        final Node jobNode = createAndStoreJobAndSimpleTrigger(store, context);
        final Trigger trigger = store.acquireNextTrigger(context, System.currentTimeMillis());
        assertNotNull(trigger);
        assertTrue(jobNode.getNode("hipposched:triggers/trigger").isLocked());
    }

    @Test
    public void testAcquireNextTriggerAndRelease() throws Exception {
        final JCRJobStore store = new JCRJobStore();
        final SchedulingContext context = new JCRSchedulingContext(session);
        final Node jobNode = createAndStoreJobAndSimpleTrigger(store, context);
        final Trigger trigger = store.acquireNextTrigger(context, System.currentTimeMillis());
        store.releaseAcquiredTrigger(context, trigger);
        assertFalse(jobNode.getNode("hipposched:triggers/trigger").isLocked());
    }

    @Test
    public void testAcquireNextTriggerForInvalidJob() throws Exception {
        createAndStoreInvalidJobAndTrigger();
        final JCRJobStore store = new JCRJobStore();
        final SchedulingContext context = new JCRSchedulingContext(session);
        final Trigger trigger = store.acquireNextTrigger(context, System.currentTimeMillis());
        assertNull(trigger);
    }

    @Test
    public void testTriggeredJobCompleteSimple() throws Exception {
        final JCRJobStore store = new JCRJobStore();
        final SchedulingContext context = new JCRSchedulingContext(session);
        final Node jobNode = createAndStoreJobAndSimpleTrigger(store, context);
        final String jobNodePath = jobNode.getPath();
        final JobDetail jobDetail = store.retrieveJob(context, jobNode.getIdentifier(), null);
        final Trigger trigger = store.acquireNextTrigger(context, System.currentTimeMillis());
        store.triggeredJobComplete(context, trigger, jobDetail, 0);
        // when the job was completed and the trigger doesn't have a next fire time, the job should be deleted
        assertFalse(session.nodeExists(jobNodePath));
    }

    @Test
    public void testTriggeredJobCompleteRepeated() throws Exception {
        final JCRJobStore store = new JCRJobStore();
        final SchedulingContext context = new JCRSchedulingContext(session);
        final Node jobNode = createAndStoreJobAndRepeatedTrigger(store, context);
        final String jobNodePath = jobNode.getPath();
        final JobDetail jobDetail = store.retrieveJob(context, jobNode.getIdentifier(), null);
        final Trigger trigger = store.acquireNextTrigger(context, System.currentTimeMillis());
        store.triggeredJobComplete(context, trigger, jobDetail, 0);
        // when the job was completed and the trigger has a next fire time, the job should not be deleted
        assertTrue(session.nodeExists(jobNodePath));
        assertTrue(jobNode.hasProperty("hipposched:triggers/trigger/hipposched:nextFireTime"));
    }

    @Test
    public void testGetTriggersForJob() throws Exception {
        final JCRJobStore store = new JCRJobStore();
        final SchedulingContext context = new JCRSchedulingContext(session);
        final Node jobNode = createAndStoreJobAndSimpleTrigger(store, context);
        final Trigger[] triggersForJob = store.getTriggersForJob(context, jobNode.getIdentifier(), null);
        assertNotNull(triggersForJob);
        assertEquals(1, triggersForJob.length);
    }

    @Test
    public void testTriggerLockKeepAlive() throws Exception {
        final JCRJobStore store = new JCRJobStore(10);
        final SchedulingContext context = new JCRSchedulingContext(session);
        final Node jobNode = createAndStoreJobAndSimpleTrigger(store, context);
        final Trigger trigger = store.acquireNextTrigger(context, System.currentTimeMillis());
        Thread.sleep(1000*12); // sleep longer than lock timeout
        assertTrue(jobNode.getNode("hipposched:triggers/trigger").isLocked());
        store.releaseAcquiredTrigger(context, trigger);
        assertFalse(jobNode.getNode("hipposched:triggers/trigger").isLocked());
    }

    private Node createAndStoreJobAndRepeatedTrigger(final JCRJobStore store, final SchedulingContext context) throws RepositoryException, JobPersistenceException {
        final Node jobNode = session.getNode("/test").addNode("job", HippoSchedJcrConstants.HIPPOSCHED_REPOSITORY_JOB);
        jobNode.setProperty(HippoSchedJcrConstants.HIPPOSCHED_REPOSITORY_JOB_CLASS, Job.class.getName());
        final JobDetail jobDetail = new RepositoryJobDetail(jobNode);
        final SimpleTrigger trigger = new SimpleTrigger("trigger", 2, 1000);
        trigger.setNextFireTime(new Date());
        store.storeJobAndTrigger(context, jobDetail, trigger);
        return jobNode;
    }

    private Node createAndStoreJobAndSimpleTrigger(final JCRJobStore store, final SchedulingContext context) throws RepositoryException, JobPersistenceException {
        final Node jobNode = session.getNode("/test").addNode("job", HippoSchedJcrConstants.HIPPOSCHED_REPOSITORY_JOB);
        jobNode.setProperty(HippoSchedJcrConstants.HIPPOSCHED_REPOSITORY_JOB_CLASS, Job.class.getName());
        final JobDetail jobDetail = new RepositoryJobDetail(jobNode);
        final SimpleTrigger trigger = new SimpleTrigger("trigger");
        trigger.setNextFireTime(new Date());
        store.storeJobAndTrigger(context, jobDetail, trigger);
        return jobNode;
    }

    private void createAndStoreInvalidJobAndTrigger() throws RepositoryException, IOException {
        final Node jobNode = session.getNode("/test").addNode("job", "hipposched:job");
        final Binary invalidJobDetail = session.getValueFactory().createBinary(getClass().getResource("/InvalidJobDetail.ser").openStream());
        jobNode.setProperty("hipposched:data", invalidJobDetail);
        final Node triggersNode = jobNode.addNode("hipposched:triggers", "hipposched:triggers");
        final Node triggerNode = triggersNode.addNode("trigger", "hipposched:trigger");
        final SimpleTrigger trigger = new SimpleTrigger("test", new Date());
        trigger.setNextFireTime(new Date());
        final Calendar nextFireTime = Calendar.getInstance();
        nextFireTime.setTime(new Date());
        triggerNode.setProperty("hipposched:nextFireTime", nextFireTime);
        triggerNode.setProperty("hipposched:fireTime", nextFireTime);
        triggerNode.setProperty("hipposched:data", JcrUtils.createBinaryValueFromObject(session, trigger));
        session.save();
    }

}
