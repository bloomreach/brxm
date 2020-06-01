/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.testutils.jcr.event;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventListener;

import static org.junit.Assert.fail;

public class EventCollector {

    private final String MAGIC_NODE_NAME = "magic-node-name";

    private Session session;
    private Node root;
    private EventListener eventListener;
    private List<EventPojo> collectedEvents = new ArrayList<>();
    private boolean stopped = false;

    public EventCollector(final Session session, final Node root) {
        this.session = session;
        this.root = root;
    }

    public void start() throws RepositoryException {
        eventListener = (events) -> {
            try {
                while (events.hasNext()) {
                    final Event event = events.nextEvent();
                    if (event.getPath().contains(MAGIC_NODE_NAME)) {
                        stopped = true;
                    } else {
                        collectedEvents.add(EventPojo.from(event));
                    }
                }
            } catch (RepositoryException e) {
                fail("Unexpected exception: " + e);
            }
        };
        session.getWorkspace().getObservationManager().addEventListener(
                eventListener,
                EventUtils.ALL_EVENTS,
                root.getPath(),
                true,
                null,
                null,
                false);
    }

    public List<EventPojo> stop() throws RepositoryException {
        root.addNode(MAGIC_NODE_NAME);
        session.save();

        int waitMs = 10;
        while (!stopped && waitMs < 1000) {
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException e) {
                fail("Unexpected exception: " + e);
            }
            waitMs *= 2;
        }
        if (!stopped) {
            fail("Event listener did not receive magic event");
        }

        root.getNode(MAGIC_NODE_NAME).remove();
        session.save();
        session.getWorkspace().getObservationManager().removeEventListener(eventListener);

        return collectedEvents;
    }

}
