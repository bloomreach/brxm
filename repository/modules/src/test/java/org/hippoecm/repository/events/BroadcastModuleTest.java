/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.events;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.logging.RepositoryLogger;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.Test;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.events.HippoWorkflowEvent;
import org.onehippo.repository.events.PersistedHippoEventListener;
import org.onehippo.repository.events.PersistedHippoEventsService;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;

public class BroadcastModuleTest extends RepositoryTestCase {

    private static class TestEventListener implements PersistedHippoEventListener {

        private volatile int seen = 0;
        private volatile HippoEvent event;

        @Override
        public String getEventCategory() {
            return null;
        }

        @Override
        public String getChannelName() {
            return "basic";
        }

        @Override
        public boolean onlyNewEvents() {
            return false;
        }

        @Override
        public void onHippoEvent(final HippoEvent event) {
            this.event = event;
            seen++;
        }

        private void clear() {
            seen = 0;
            event = null;
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        clearLog();
    }

    private void clearLog() throws RepositoryException {
        for (Node clusterLog : new NodeIterable(session.getNode("/hippo:log").getNodes())) {
            for (Node nodeLog : new NodeIterable(clusterLog.getNodes())) {
                nodeLog.remove();
            }
        }
        session.save();
    }

    @Override
    public void tearDown() throws Exception {
        for (Node node : new NodeIterable(session.getNode("/hippo:configuration/hippo:modules/broadcast/hippo:moduleconfig").getNodes())) {
            node.remove();
        }
        session.save();
        super.tearDown();
    }

    @Test
    public void testPoller() throws Exception {
        TestEventListener listener = new TestEventListener();

        HippoWorkflowEvent in = new HippoWorkflowEvent();
        in.className("hoho");
        in.action("hihi");
        in.subjectPath("/content/documents/test");
        in.workflowCategory("haha");
        in.interaction("hehe");

        HippoServiceRegistry.registerService(listener, PersistedHippoEventsService.class);
        try {
            RepositoryLogger logger = new RepositoryLogger();
            logger.initialize(session);
            logger.logHippoEvent(in);
            logger.shutdown();

            waitForEvent(listener);

            assertEquals(1, listener.seen);

            HippoWorkflowEvent out = new HippoWorkflowEvent(listener.event);
            assertEquals(in.className(), out.className());
            assertEquals(in.action(), out.action());
            assertEquals(in.subjectPath(), out.subjectPath());
            assertEquals(in.workflowCategory(), out.workflowCategory());
            assertEquals(in.interaction(), out.interaction());

            final Node moduleNode = session.getNode("/hippo:configuration/hippo:modules/broadcast/hippo:moduleconfig");
            Node clusterNode = moduleNode.getNodes().nextNode();
            Node basicNode = clusterNode.getNode("basic");
            assertEquals(out.timestamp(), basicNode.getProperty(BroadcastConstants.LAST_PROCESSED).getLong());

            // new event
            HippoWorkflowEvent newEvent = new HippoWorkflowEvent(in);
            newEvent.timestamp(System.currentTimeMillis());
            listener.clear();
            logger.initialize(session);
            logger.logHippoEvent(newEvent);
            logger.shutdown();

            waitForEvent(listener);

            assertEquals(1, listener.seen);
        } finally {
            HippoServiceRegistry.unregisterService(listener, PersistedHippoEventsService.class);
        }

    }

    private void waitForEvent(final TestEventListener listener) throws Exception {
        int n = 101;
        while (n-- > 0) {
            Thread.sleep(100);
            if (listener.event != null) {
                Thread.sleep(100);
                return;
            }
        }
        throw new Exception("Event not received within 10 seconds");
    }
}
