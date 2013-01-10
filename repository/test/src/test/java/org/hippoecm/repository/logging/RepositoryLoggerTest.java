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

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.WorkflowException;
import org.junit.After;
import org.junit.Test;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class RepositoryLoggerTest extends RepositoryTestCase {

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
    public void testCreateRepositoryLogger() throws Exception {
        final RepositoryLogger repositoryLogger = new RepositoryLogger();
        repositoryLogger.initialize(session);
        assertTrue(session.itemExists("/hippo:log/default"));
    }

    @Test
    public void testCreateLogNode() throws RemoteException, RepositoryException, WorkflowException {
        final RepositoryLogger repositoryLogger = new RepositoryLogger();
        repositoryLogger.initialize(session);

        HippoEvent event = new HippoEvent("repository");
        event.user("userName").category("workflow").result("resultValue");
        event.set("className", "className").set("methodName", "methodName");
        repositoryLogger.logHippoEvent(event);

        Node logFolder = session.getNode("/hippo:log/default");
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
