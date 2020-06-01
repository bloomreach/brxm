/*
 * Copyright 2014-2019 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.platform.configuration.cache;

import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.is;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_DOMAINS;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_UPSTREAM;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;

@RunWith(EasyMockRunner.class)
public class HstEventsCollectorTest {

    private HstEventsCollector hstEventsCollector;

    @Mock
    private Event nodeAddedEvent;
    @Mock
    private Event nodeAddedEvent2;
    @Mock
    private Event nodeRemovedEvent;
    @Mock
    private Event propertyAddedEvent;
    @Mock
    private EventIterator events;

    @Before
    public void setUp() {
        this.hstEventsCollector = new HstEventsCollector("/hst:myproject");
    }

    @Test
    public void testCollect_adds_parent_if_a_node_is_moved() throws RepositoryException {

        final String parentPath = "/hst:myproject/a";
        final String childPath = parentPath + "/b";

        expect(nodeAddedEvent.getUserData()).andReturn("").anyTimes();
        expect(nodeAddedEvent.getPath()).andReturn(childPath).anyTimes();
        expect(nodeAddedEvent.getType()).andReturn(Event.NODE_ADDED).anyTimes();

        expect(nodeRemovedEvent.getUserData()).andReturn("").anyTimes();
        expect(nodeRemovedEvent.getPath()).andReturn(childPath).anyTimes();
        expect(nodeRemovedEvent.getType()).andReturn(Event.NODE_REMOVED).anyTimes();

        expect(events.hasNext()).andReturn(true).andReturn(true).andReturn(false);
        expect(events.nextEvent()).andReturn(nodeRemovedEvent).andReturn(nodeAddedEvent);

        replay(events, nodeAddedEvent, nodeRemovedEvent);

        hstEventsCollector.collect(events);

        final Set<HstEvent> result = hstEventsCollector.getAndClearEvents();
        assertThat(result.size(), is(2));
        assertThat(result, hasItems(new HstEvent(childPath, false), new HstEvent(parentPath, false)));
    }

    @Test
    public void testCollect_ignores_upstream_node_events() throws RepositoryException {

        expect(nodeAddedEvent.getUserData()).andStubReturn("some user data");
        expect(nodeAddedEvent.getType()).andStubReturn(Event.NODE_ADDED);
        expect(nodeAddedEvent.getPath()).andStubReturn("/hst:myproject/a/b/" + NODENAME_HST_UPSTREAM);

        expect(nodeRemovedEvent.getUserData()).andStubReturn("some user data");
        expect(nodeRemovedEvent.getType()).andStubReturn(Event.NODE_ADDED);
        expect(nodeRemovedEvent.getPath()).andStubReturn("/hst:myproject/a/b/" + NODENAME_HST_UPSTREAM + "/x");

        expect(propertyAddedEvent.getUserData()).andStubReturn("some user data");
        expect(propertyAddedEvent.getType()).andStubReturn(Event.PROPERTY_ADDED);
        expect(propertyAddedEvent.getPath()).andStubReturn("/hst:myproject/a/b/" + NODENAME_HST_UPSTREAM + "/x");

        expect(events.hasNext()).andReturn(true).times(3).andReturn(false);
        expect(events.nextEvent()).andReturn(nodeAddedEvent).andReturn(nodeRemovedEvent).andReturn(propertyAddedEvent);

        replay(events, nodeAddedEvent, nodeRemovedEvent, propertyAddedEvent);

        hstEventsCollector.collect(events);

        final Set<HstEvent> result = hstEventsCollector.getAndClearEvents();
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    public void hst_domain_changes_are_ignored_in_events_collector() throws Exception {
        expect(nodeAddedEvent.getPath()).andStubReturn("/hst:myproject/" + NODENAME_HST_DOMAINS);
        expect(nodeAddedEvent.getType()).andReturn(Event.NODE_ADDED).anyTimes();
        expect(nodeAddedEvent.getUserData()).andReturn(null).anyTimes();

        expect(nodeAddedEvent2.getPath()).andStubReturn("/hst:myproject/" + NODENAME_HST_DOMAINS + "/foo");
        expect(nodeAddedEvent2.getType()).andReturn(Event.NODE_ADDED).anyTimes();
        expect(nodeAddedEvent2.getUserData()).andReturn(null).anyTimes();

        expect(nodeRemovedEvent.getPath()).andStubReturn("/hst:myproject/" + NODENAME_HST_DOMAINS + "/x");
        expect(nodeRemovedEvent.getType()).andReturn(Event.NODE_REMOVED).anyTimes();
        expect(nodeRemovedEvent.getUserData()).andReturn(null).anyTimes();

        expect(propertyAddedEvent.getPath()).andStubReturn("/hst:myproject/" + NODENAME_HST_DOMAINS + "/foo");
        expect(propertyAddedEvent.getType()).andReturn(Event.NODE_MOVED).anyTimes();
        expect(propertyAddedEvent.getUserData()).andReturn(null).anyTimes();


        expect(events.hasNext()).andReturn(true).times(4).andReturn(false);
        expect(events.nextEvent())
                .andReturn(nodeAddedEvent)
                .andReturn(nodeAddedEvent2)
                .andReturn(nodeRemovedEvent)
                .andReturn(propertyAddedEvent);
        replay(events, nodeAddedEvent, nodeAddedEvent2, nodeRemovedEvent, propertyAddedEvent);
        hstEventsCollector.collect(events);

        final Set<HstEvent> result = hstEventsCollector.getAndClearEvents();
        assertThat(result.isEmpty(), is(true));
    }
}
