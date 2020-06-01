/*
 * Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.logging.RepositoryLogger;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.Test;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.repository.events.HippoWorkflowEvent;
import org.onehippo.repository.events.PersistedHippoEventListener;
import org.onehippo.repository.events.PersistedHippoEventListenerRegistry;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class BroadcastModuleTest extends RepositoryTestCase {

    private static class TestEventListener implements PersistedHippoEventListener {

        private volatile int seen = 0;
        protected volatile HippoEvent event;
        protected volatile List<HippoEvent> processed = new ArrayList<>();

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
            return true;
        }

        @Override
        public void onHippoEvent(final HippoEvent event) {
            this.event = event;
            processed.add(event);
            seen++;
        }

        private void clear() {
            seen = 0;
            event = null;
        }
    }

    private static class CategoryEventListener extends TestEventListener {


        private String category;

        private CategoryEventListener(final String category) {
            this.category = category;
        }

        @Override
        public String getEventCategory() {
            return category;
        }

        @Override
        public String getChannelName() {
            return "category";
        }

        @Override
        public boolean onlyNewEvents() {
            // this makes sure that the CategoryEventListener is always run the first time before
            // the TestEventListener and is to verify the fix in REPO-1934
            return false;
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

        PersistedHippoEventListenerRegistry.get().register(listener);
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
            PersistedHippoEventListenerRegistry.get().unregister(listener);
        }

    }

    @Test
    public void events_older_than_max_event_age_are_skipped() throws Exception {
        final TestEventListener listener = new TestEventListener();
        final RepositoryLogger logger = new RepositoryLogger();
        try {
            PersistedHippoEventListenerRegistry.get().register(listener);
            logger.initialize(session);

            HippoWorkflowEvent outdated = new HippoWorkflowEvent();
            outdated.category("bar");
            // override the timestamp a bit dirty
            final long toDaysAgoMs = 2 * 24 * 60 * 60 * 1000;
            outdated.set("timestamp", System.currentTimeMillis() - toDaysAgoMs);
            logger.logHippoEvent(outdated);

            try {
                waitForEvent(listener);
                fail("Expected no event because outdated");
            } catch (Exception e) {
                assertEquals("Event not received within 10 seconds", e.getMessage());
            }

            final Node moduleNode = session.getNode("/hippo:configuration/hippo:modules/broadcast/hippo:moduleconfig");

            assertEquals("No event yet has been processed so no channel node with last processed should yet been persisted",
                    0, moduleNode.getNodes().getSize());

            HippoWorkflowEvent next = new HippoWorkflowEvent();
            next.category("bar");
            logger.logHippoEvent(next);
            waitForEvent(listener);

            assertEquals(1, listener.seen);
            Node clusterNode = moduleNode.getNodes().nextNode();
            assertEquals(next.timestamp(), clusterNode.getNode("basic").getProperty(BroadcastConstants.LAST_PROCESSED).getLong());

        } finally {
            logger.shutdown();
            PersistedHippoEventListenerRegistry.get().unregister(listener);
        }
    }


    @Test
    public void multiple_events_are_processed_from_old_to_new() throws Exception {
        final TestEventListener listener = new TestEventListener();
        final RepositoryLogger logger = new RepositoryLogger();
        try {
            PersistedHippoEventListenerRegistry.get().register(listener);
            logger.initialize(session);

            HippoWorkflowEvent event1 = new HippoWorkflowEvent();
            event1.category("bar1");
            logger.logHippoEvent(event1);

            Thread.sleep(1);

            HippoWorkflowEvent event2 = new HippoWorkflowEvent();
            event2.category("bar2");
            logger.logHippoEvent(event2);

            Thread.sleep(3);

            HippoWorkflowEvent event3 = new HippoWorkflowEvent();
            event3.category("bar3");
            logger.logHippoEvent(event3);

            waitForEvent(listener);

            assertEquals(3, listener.processed.size());

            assertEquals(event1.category(), listener.processed.get(0).category());
            assertEquals(event2.category(), listener.processed.get(1).category());
            assertEquals(event3.category(), listener.processed.get(2).category());
        } finally {
            logger.shutdown();
            PersistedHippoEventListenerRegistry.get().unregister(listener);
        }
    }
    
    @Test
    public void multiple_persisted_listeners_including_specific_category_listeners() throws Exception {

        final TestEventListener nullCategoryListener = new TestEventListener();
        final CategoryEventListener fooCategoryListener = new CategoryEventListener("foo");
        final RepositoryLogger logger = new RepositoryLogger();
        try {
            PersistedHippoEventListenerRegistry.get().register(nullCategoryListener);
            PersistedHippoEventListenerRegistry.get().register(fooCategoryListener);
            logger.initialize(session);

            HippoWorkflowEvent bar = new HippoWorkflowEvent();
            // fooCategoryListener does not listen to 'bar' category
            bar.category("bar");
            logger.logHippoEvent(bar);

            waitForEvent(nullCategoryListener);

            assertEquals("bar", nullCategoryListener.event.category());
            assertNull("No event expected for the 'foo' category listener" , fooCategoryListener.event);

            final Node moduleNode = session.getNode("/hippo:configuration/hippo:modules/broadcast/hippo:moduleconfig");
            final Node clusterNode = moduleNode.getNodes().nextNode();

            {
                final Node basicNode = clusterNode.getNode("basic");
                assertEquals(bar.timestamp(), basicNode.getProperty(BroadcastConstants.LAST_PROCESSED).getLong());

                final Node categoryNode = clusterNode.getNode("category");
                assertEquals("Although the 'fooCategoryListener' did not receive any event, we still expect " +
                                "the events to be processed including the 'bar' timestamp.",
                        bar.timestamp(), categoryNode.getProperty(BroadcastConstants.LAST_PROCESSED).getLong());
            }

            nullCategoryListener.event = null;

            HippoWorkflowEvent foo = new HippoWorkflowEvent();
            // fooCategoryListener does listen to 'foo' category
            foo.category("foo");
            logger.logHippoEvent(foo);

            waitForEvent(nullCategoryListener);
            assertEquals("foo", nullCategoryListener.event.category());

            waitForEvent(fooCategoryListener);
            assertEquals("foo", nullCategoryListener.event.category());

            {
                final Node basicNode = clusterNode.getNode("basic");
                assertEquals(foo.timestamp(), basicNode.getProperty(BroadcastConstants.LAST_PROCESSED).getLong());

                final Node categoryNode = clusterNode.getNode("category");
                assertEquals(foo.timestamp(), categoryNode.getProperty(BroadcastConstants.LAST_PROCESSED).getLong());
            }

        } finally {
            logger.shutdown();
            PersistedHippoEventListenerRegistry.get().unregister(nullCategoryListener);
            PersistedHippoEventListenerRegistry.get().unregister(fooCategoryListener);
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
