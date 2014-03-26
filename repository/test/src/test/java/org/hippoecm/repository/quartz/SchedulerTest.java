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

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.scheduling.RepositoryJob;
import org.onehippo.repository.scheduling.RepositoryJobCronTrigger;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.onehippo.repository.scheduling.RepositoryJobInfo;
import org.onehippo.repository.scheduling.RepositoryJobSimpleTrigger;
import org.onehippo.repository.scheduling.RepositoryJobTrigger;
import org.onehippo.repository.scheduling.RepositoryScheduler;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class SchedulerTest extends RepositoryTestCase {

    private static final Logger log = LoggerFactory.getLogger(SchedulerTest.class);

    private static boolean repositoryJobExecuted;
    private static String failureMessage;

    private RepositoryScheduler scheduler;
    private RepositoryJobInfo testJobInfo;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        scheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);
        testJobInfo = new RepositoryJobInfo("test", TestRepositoryJob.class);
        repositoryJobExecuted = false;
        failureMessage = null;
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        scheduler.deleteJob("test", "default");
    }

    @Test
    public void testScheduleRepositoryJobWithSimpleTrigger() throws Exception {
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
        final RepositoryJobTrigger testJobTrigger = new RepositoryJobSimpleTrigger("test", new Date(System.currentTimeMillis() + 2500));
        scheduler.scheduleJob(testJobInfo, testJobTrigger);
        scheduler.deleteJob(testJobInfo.getName(), testJobInfo.getGroup());
        if (waitUntilExecuted()) {
            fail("Deleted job was still executed.");
        }
    }

    @Test
    public void testTriggerRepositoryJobNow() throws Exception {
        final RepositoryJobTrigger testJobTrigger = new RepositoryJobSimpleTrigger("test", new Date(System.currentTimeMillis()+60000));
        scheduler.scheduleJob(testJobInfo, testJobTrigger);
        scheduler.executeJob("test", "default");
        try {
            if (!repositoryJobExecuted) {
                fail("RepositoryJob not executed within 5 seconds");
            }
        } finally {
            scheduler.deleteJob("test", "default");
        }
    }

    /**
     * REPO-813 Scheduling a repeating job with a repeat interval of 5 minutes
     * fires repeating triggers every 30 seconds more or less
     */
    @Test
    @Ignore
    public void testScheduleCronJob() throws Exception {
        final RepositoryScheduler scheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);
        final RepositoryJobInfo testJobInfo = new RepositoryJobInfo("test", TestRepositoryJob.class);
        final RepositoryJobTrigger testJobTrigger = new RepositoryJobCronTrigger("test", "0 0/1 * * * ?");
        scheduler.scheduleJob(testJobInfo, testJobTrigger);
        long start = System.currentTimeMillis();
        int count = 0;
        outer: while (count++ < 3) {
            while (true) {
                if (repositoryJobExecuted) {
                    repositoryJobExecuted = false;
                    break;
                } else {
                    long totalTime = System.currentTimeMillis() - start;
                    if (totalTime > 1000*60*5) {
                        break outer;
                    }
                }
                Thread.sleep(1000);
            }
        }
        long totalTime = System.currentTimeMillis() - start;
        assertTrue("Execution of jobs took less time than expected", totalTime > 1000*60*2);
        assertTrue("Execution of jobs took more time than expected", totalTime < 1000*60*3);
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
            log.debug("Executing TestRepositoryJob");
            repositoryJobExecuted = true;
            if (context.getAttribute("foo") == null) {
                failureMessage = "expected attribute not found";
            }
        }

    }
}
