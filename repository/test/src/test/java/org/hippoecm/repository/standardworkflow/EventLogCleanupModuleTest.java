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

import org.hippoecm.repository.TestCase;
import org.hippoecm.repository.api.HippoNodeIterator;
import org.junit.Before;
import org.junit.Test;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

import javax.jcr.NodeIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import static junit.framework.Assert.assertEquals;

public class EventLogCleanupModuleTest extends TestCase {

    private boolean jobExecuted;
    private final Object monitor = new Object();

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // make sure there are no log items in the repository
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        NodeIterator nodes = queryManager.createQuery("SELECT * FROM hippolog:item", Query.SQL).execute().getNodes();
        while(nodes.hasNext()) {
            nodes.nextNode().remove();
        }
        session.save();
    }

    @Test
    public void testEventLogCleanupMaxItems() throws Exception {
        WorkflowEventLoggerWorkflowImpl eventLogger = new WorkflowEventLoggerWorkflowImpl(null, session, null);
        eventLogger.logEvent("userName", "className", "methodName");
        eventLogger.logEvent("userName", "className", "methodName");
        eventLogger.logEvent("userName", "className", "methodName");

        // run cleanup module with maximum items of 1 and no item timeout
        EventLogCleanupModule module = new EventLogCleanupModule("0/2 * * * * ?", 1l, -1l, new TestJobListener(), session);

        synchronized (monitor) {
            while (!jobExecuted) {
                monitor.wait(3000);
            }
        }
        module.shutdown();
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        // it seems we need to specify an order by clause to get the total size...
        NodeIterator nodes = queryManager.createQuery("SELECT * FROM hippolog:item ORDER BY hippolog:timestamp ASC", Query.SQL).execute().getNodes();
        assertEquals(1l, ((HippoNodeIterator)nodes).getTotalSize());
    }

    @Test
    public void testEventLogCleanupTimeout() throws Exception {
        WorkflowEventLoggerWorkflowImpl eventLogger = new WorkflowEventLoggerWorkflowImpl(null, session, null);
        eventLogger.logEvent("userName", "className", "methodName");
        eventLogger.logEvent("userName", "className", "methodName");
        eventLogger.logEvent("userName", "className", "methodName");

        // run cleanup module with no maximum to the number of items and all items timed out
        EventLogCleanupModule module = new EventLogCleanupModule("0/2 * * * * ?", -1l, 0l, new TestJobListener(), session);

        synchronized (monitor) {
            while (!jobExecuted) {
                monitor.wait(3000);
            }
        }
        module.shutdown();
        
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        // it seems we need to specify an order by clause to get the total size...
        NodeIterator nodes = queryManager.createQuery("SELECT * FROM hippolog:item ORDER BY hippolog:timestamp ASC", Query.SQL).execute().getNodes();
        assertEquals(0l, ((HippoNodeIterator)nodes).getTotalSize());
    }

    private class TestJobListener implements JobListener {

        @Override
        public String getName() {
            return "testJobListener";
        }

        @Override
        public void jobToBeExecuted(JobExecutionContext context) {
        }

        @Override
        public void jobExecutionVetoed(JobExecutionContext context) {
        }

        @Override
        public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
            jobExecuted = true;
            synchronized (monitor) {
                monitor.notify();
            }
        }
    }
}
