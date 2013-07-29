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
package org.hippoecm.repository.quartz;

import java.util.Date;

import javax.jcr.RepositoryException;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.scheduling.RepositoryJob;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.onehippo.repository.scheduling.RepositoryJobInfo;
import org.onehippo.repository.scheduling.RepositoryJobSimpleTrigger;
import org.onehippo.repository.scheduling.RepositoryJobTrigger;
import org.onehippo.repository.scheduling.RepositoryScheduler;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static junit.framework.Assert.fail;

public class SchedulerTest extends RepositoryTestCase {

    private static boolean repositoryJobExecuted;
    private static String failureMessage;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        repositoryJobExecuted = false;
        failureMessage = null;
    }

    @Test
    public void testScheduleRepositoryJobWithSimpleTrigger() throws Exception {
        final RepositoryScheduler scheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);
        final RepositoryJobInfo testJobInfo = new RepositoryJobInfo("test", TestRepositoryJob.class);
        testJobInfo.setAttribute("foo", "bar");
        final RepositoryJobTrigger testJobTrigger = new RepositoryJobSimpleTrigger("test", new Date());
        scheduler.scheduleJob(testJobInfo, testJobTrigger);
        if (!waitUntilExecuted()) {
            fail("RepositoryJob not executed within 5 seconds");
        }
        if (failureMessage != null) {
            fail(failureMessage);
        }
    }

    @Test
    public void testScheduleRepositoryJobWithSimpleTriggerIndefinitely() throws Exception {
        final RepositoryScheduler scheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);
        final RepositoryJobInfo testJobInfo = new RepositoryJobInfo("test", TestRepositoryJob.class);
        final RepositoryJobTrigger testJobTrigger = new RepositoryJobSimpleTrigger("test", new Date(), RepositoryJobSimpleTrigger.REPEAT_INDEFINITELY, 5000);
        scheduler.scheduleJob(testJobInfo, testJobTrigger);
        try {
            if (!waitUntilExecuted()) {
                fail("RepositoryJob not executed within 5 seconds");
            }
        } finally {
            scheduler.deleteJob("test", "default");
        }
    }

    @Test
    public void testScheduleAndDeleteRepositoryJob() throws Exception {
        final RepositoryScheduler scheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);
        final RepositoryJobInfo testJobInfo = new RepositoryJobInfo("test", TestRepositoryJob.class);
        final RepositoryJobTrigger testJobTrigger = new RepositoryJobSimpleTrigger("test", new Date(System.currentTimeMillis() + 2500));
        scheduler.scheduleJob(testJobInfo, testJobTrigger);
        scheduler.deleteJob(testJobInfo.getName(), testJobInfo.getGroup());
        if (waitUntilExecuted()) {
            fail("Deleted job was still executed.");
        }
    }

    private boolean waitUntilExecuted() throws Exception {
        int n = 50;
        while (n-- > 0) {
            Thread.sleep(100);
            if (repositoryJobExecuted) {
                return true;
            }
        }
        return false;
    }


    public static class TestRepositoryJob implements RepositoryJob {

        @Override
        public void execute(final RepositoryJobExecutionContext context) throws RepositoryException {
            repositoryJobExecuted = true;
            if (context.getAttribute("foo") == null) {
                failureMessage = "expected attribute not found";
            }
        }

    }
}
