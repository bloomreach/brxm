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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class WorkflowEventLoggerWorkflowImpTest extends TestCase {

    private static String cluster_id;
    
    @BeforeClass
    public static void setUpClass() {
        cluster_id = System.getProperty("org.apache.jackrabbit.core.cluster.node_id");
        System.setProperty("org.apache.jackrabbit.core.cluster.node_id", "test");
    }
    
    @AfterClass
    public static void tearDownClass() {
        if (cluster_id != null) {
            System.setProperty("org.apache.jackrabbit.core.cluster.node_id", cluster_id);
        }
    }
    
    @After
    public void tearDown() throws Exception {
        NodeIterator nodes = session.getNode("/hippo:log").getNodes();
        while (nodes.hasNext()) {
            nodes.nextNode().remove();
        }
        session.save();
        super.tearDown();
    }

    @Test
    public void testCreateWorkflowEventLoggerImpl() throws Exception {
        System.out.println(session);
        new WorkflowEventLoggerWorkflowImpl(null, session, null);
        assertTrue(session.itemExists("/hippo:log/test"));
    }

    @Test
    public void testLogEvent() throws Exception {
        WorkflowEventLoggerWorkflowImpl eventLogger = new WorkflowEventLoggerWorkflowImpl(null, session, null);
        eventLogger.logEvent("userName", "className", "methodName");
        Node logFolder = session.getNode("/hippo:log/test");
        Node currentNode = logFolder;
        for (int i = 0; i < 4; i++) {
            NodeIterator nodes = currentNode.getNodes();
            assertTrue(nodes.hasNext());
            currentNode = nodes.nextNode();
        }
        Node logEvent = currentNode;
        assertEquals("userName", logEvent.getProperty("hippolog:eventUser").getString());
        assertEquals("className", logEvent.getProperty("hippolog:eventClass").getString());
        assertEquals("methodName", logEvent.getProperty("hippolog:eventMethod").getString());
    }

}
