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
package org.onehippo.cm.impl.model.builder.eventutils;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import static org.junit.Assert.fail;

public class ExpectedEvents {

    private final List<EventPojo> expectedEvents = new ArrayList<>();

    public ExpectedEvents expectNodeAdded(final String path) throws RepositoryException {
        expectedEvents.add(new EventPojo(Event.NODE_ADDED, path));
        return this;
    }

    public ExpectedEvents expectNodeReordered(final String parent, final String srcChildRelPath, final String destChildRelPath) throws RepositoryException {
        expectedEvents.add(new EventPojo(Event.NODE_MOVED, parent, srcChildRelPath, destChildRelPath));
        return this;
    }

    public ExpectedEvents expectNodeRemoved(final String path) throws RepositoryException {
        expectedEvents.add(new EventPojo(Event.NODE_REMOVED, path));
        return this;
    }

    public ExpectedEvents expectPropertyAdded(final String path) throws RepositoryException {
        expectedEvents.add(new EventPojo(Event.PROPERTY_ADDED, path));
        return this;
    }

    public ExpectedEvents expectPropertyChanged(final String path) throws RepositoryException {
        expectedEvents.add(new EventPojo(Event.PROPERTY_CHANGED, path));
        return this;
    }

    public ExpectedEvents expectPropertyRemoved(final String path) throws RepositoryException {
        expectedEvents.add(new EventPojo(Event.PROPERTY_REMOVED, path));
        return this;
    }

    public void check(final List<EventPojo> actualEvents) throws RepositoryException {
        final StringBuilder message = new StringBuilder();
        final List<EventPojo> unseenEvents = new ArrayList<>(expectedEvents.size());
        unseenEvents.addAll(expectedEvents);

        for (EventPojo event : actualEvents) {
            if (unseenEvents.contains(event)) {
                unseenEvents.remove(event);
            } else {
                message.append("unexpected event: ").append(event.toString()).append("\n");
            }
        }

        for (EventPojo unseen : unseenEvents) {
            message.append("did not see the following event: ").append(unseen.toString()).append("\n");
        }

        if (message.length() > 0) {
            fail(message.toString());
        }
    }

}
