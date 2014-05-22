/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.configuration.cache;

import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.contains;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.hasItems;

public class HstEventsCollectorTest {

    private HstEventsCollector hstEventsCollector;

    @Before
    public void setUp() {
        this.hstEventsCollector = new HstEventsCollector();
        this.hstEventsCollector.setRootPath("/");
    }
    @Test
    public void testCollect_adds_parent_if_a_node_is_moved() throws RepositoryException {

        final String parentPath = "/a";
        final String childPath = parentPath + "/b";

        final Event addNode = createNiceMock(Event.class);
        expect(addNode.getUserData()).andReturn("").anyTimes();
        expect(addNode.getPath()).andReturn(childPath).anyTimes();
        expect(addNode.getType()).andReturn(Event.NODE_ADDED).anyTimes();

        final Event removeNode = createNiceMock(Event.class);
        expect(removeNode.getUserData()).andReturn("").anyTimes();
        expect(removeNode.getPath()).andReturn(childPath).anyTimes();
        expect(removeNode.getType()).andReturn(Event.NODE_REMOVED).anyTimes();

        final EventIterator events = createNiceMock(EventIterator.class);
        expect(events.hasNext()).andReturn(true).andReturn(true).andReturn(false);
        expect(events.nextEvent()).andReturn(removeNode).andReturn(addNode);

        EasyMock.replay(events, addNode, removeNode);

        hstEventsCollector.collect(events);

        final Set<HstEvent> result = hstEventsCollector.getAndClearEvents();
        assertThat(result.size(), is(2));
        assertThat(result, hasItems(new HstEvent(childPath, false), new HstEvent(parentPath, false)));
    }
}
