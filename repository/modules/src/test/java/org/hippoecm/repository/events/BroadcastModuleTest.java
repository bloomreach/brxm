/*
 * Copyright (C) 2012 Hippo B.V.
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

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;

import org.apache.jackrabbit.commons.iterator.NodeIterable;
import org.hippoecm.repository.logging.RepositoryLogger;
import org.junit.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.cms7.services.eventbus.Subscribe;
import org.onehippo.repository.events.HippoWorkflowEvent;
import org.onehippo.repository.events.Persisted;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;

public class BroadcastModuleTest extends RepositoryTestCase {

    static class Listener {

        List<HippoWorkflowEvent> seenEvents = new LinkedList<HippoWorkflowEvent>();

        @Subscribe
        @Persisted(name = "basic")
        public void handleWorkflowEvent(HippoWorkflowEvent event) {
            seenEvents.add(event);
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        for (Node node : new NodeIterable(session.getNode("/hippo:configuration/hippo:modules/broadcast/hippo:moduleconfig").getNodes())) {
            node.remove();
        }

        if (session.nodeExists("/hippo:log")) {
            session.getNode("/hippo:log").remove();
        }
        session.getRootNode().addNode("hippo:log", "hippolog:folder");
        session.save();
    }

    @Test
    public void testPoller() throws Exception {
        Listener listener = new Listener();

        HippoWorkflowEvent event = new HippoWorkflowEvent();
        event.className("hoho");
        event.methodName("hihi");
        event.documentPath("/content/documents/test");

        HippoServiceRegistry.registerService(listener, HippoEventBus.class);
        try {
            RepositoryLogger logger = new RepositoryLogger();
            logger.initialize(session);

            logger.logHippoEvent(event);

            logger.shutdown();

            waitForEvent(listener);

            assertEquals(1, listener.seenEvents.size());

            HippoWorkflowEvent hippoEvent = listener.seenEvents.get(0);
            assertEquals(hippoEvent.className(), event.className());
            assertEquals(hippoEvent.methodName(), event.methodName());
            assertEquals(hippoEvent.documentPath(), event.documentPath());

            final Node moduleNode = session.getNode("/hippo:configuration/hippo:modules/broadcast/hippo:moduleconfig");
            Node clusterNode = moduleNode.getNodes().nextNode();
            Node basicNode = clusterNode.getNode("basic");
            assertEquals(hippoEvent.timestamp(), basicNode.getProperty(BroadcastConstants.LAST_PROCESSED).getLong());

            // new event
            HippoWorkflowEvent newEvent = new HippoWorkflowEvent(event);
            newEvent.timestamp(System.currentTimeMillis());
            listener.seenEvents.clear();
            logger.initialize(session);
            logger.logHippoEvent(newEvent);
            logger.shutdown();

            waitForEvent(listener);

            assertEquals(1, listener.seenEvents.size());
        } finally {
            HippoServiceRegistry.unregisterService(listener, HippoEventBus.class);
        }

    }

    private void waitForEvent(final Listener listener) throws Exception {
        int n = 50;
        while (n-- > 0) {
            Thread.sleep(100);
            if (listener.seenEvents.size() == 1) {
                Thread.sleep(100);
                return;
            }
        }
        throw new Exception("Event not received within 5 seconds");
    }
}
