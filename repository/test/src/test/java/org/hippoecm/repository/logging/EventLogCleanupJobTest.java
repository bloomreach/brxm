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
package org.hippoecm.repository.logging;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.easymock.EasyMock;
import org.hippoecm.repository.api.HippoNodeIterator;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;

public class EventLogCleanupJobTest extends RepositoryTestCase {

    private RepositoryLogger eventLogger;

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

        eventLogger = new RepositoryLogger();
        eventLogger.initialize(session);
    }

    @After
    public void tearDown() throws Exception {
        if (eventLogger != null) {
            eventLogger.shutdown();
            eventLogger = null;
        }
        super.tearDown();
    }

    private void logEvent(String userName, String className, String methodName) {
        HippoEvent event = new HippoEvent("repository");
        event.user(userName).category("workflow").result("resultValue");
        event.set("className", className).set("methodName", methodName);
        eventLogger.logHippoEvent(event);
    }

    @Test
    public void testEventLogCleanupMaxItems() throws Exception {
        logEvent("userName", "className", "methodName");
        logEvent("userName", "className", "methodName");
        logEvent("userName", "className", "methodName");

        // run cleanup module with maximum items of 1 and no item timeout
        EventLogCleanupJob cleanupJob = new EventLogCleanupJob();
        final RepositoryJobExecutionContext executionContext = createMock(RepositoryJobExecutionContext.class);
        expect(executionContext.createSystemSession()).andReturn(session.impersonate(new SimpleCredentials("admin", new char[] {})));
        expect(executionContext.getAttribute("maxitems")).andReturn("1");
        expect(executionContext.getAttribute("minutestolive")).andReturn("-1");
        EasyMock.replay(executionContext);

        cleanupJob.execute(executionContext);

        QueryManager queryManager = session.getWorkspace().getQueryManager();
        // it seems we need to specify an order by clause to get the total size...
        NodeIterator nodes = queryManager.createQuery("SELECT * FROM hippolog:item ORDER BY hippolog:timestamp ASC", Query.SQL).execute().getNodes();
        assertEquals(1l, ((HippoNodeIterator)nodes).getTotalSize());

        assertNoEmptyFolders();
    }

    @Test
    public void testEventLogCleanupTimeout() throws Exception {
        logEvent("userName", "className", "methodName");
        logEvent("userName", "className", "methodName");
        logEvent("userName", "className", "methodName");

        // run cleanup module with no maximum to the number of items and all items timed out
        EventLogCleanupJob cleanupJob = new EventLogCleanupJob();
        final RepositoryJobExecutionContext executionContext = createMock(RepositoryJobExecutionContext.class);
        expect(executionContext.createSystemSession()).andReturn(session.impersonate(new SimpleCredentials("admin", new char[] {})));
        expect(executionContext.getAttribute("maxitems")).andReturn("-1");
        expect(executionContext.getAttribute("minutestolive")).andReturn("0");
        EasyMock.replay(executionContext);

        cleanupJob.execute(executionContext);

        QueryManager queryManager = session.getWorkspace().getQueryManager();
        // it seems we need to specify an order by clause to get the total size...
        NodeIterator nodes = queryManager.createQuery("SELECT * FROM hippolog:item ORDER BY hippolog:timestamp ASC", Query.SQL).execute().getNodes();
        assertEquals(0l, ((HippoNodeIterator)nodes).getTotalSize());

        assertNoEmptyFolders();
    }

    private void assertNoEmptyFolders() throws RepositoryException {
        final Node root = session.getNode("/hippo:log");
        assertNoEmptyFolders(root);
    }

    private void assertNoEmptyFolders(final Node node) throws RepositoryException {
        if (node.getNodes().getSize() == 0) {
            fail("Empty hippolog:folder: " + node.getPath());
        }
        for (Node child : new NodeIterable(node.getNodes())) {
            if (child.isNodeType("hippolog:folder") && child.getName().length() == 1) {
                assertNoEmptyFolders(child);
            }
        }
    }

}
