/*
 *  Copyright 2011 Hippo.
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
package org.onehippo.cms7.autoexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

public class EventPreProcessorTest extends RepositoryTestCase {

    private static final String[] content = new String[] {
        "/test", "nt:unstructured",
        "/test/foo", "nt:unstructured",
        "/test/foo/foo", "nt:unstructured",
        "foo", "foo",
        "/test/bar", "nt:unstructured",
        "bar", "bar"
    };

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        build(session, content);
        session.save();
    }
    
    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }
    
    @Test
    public void testAddMissingNodeAddedEvents() throws RepositoryException {
        EventPreProcessor processor = new EventPreProcessor(super.session);
        Collection<ExportEvent> events = new ArrayList<ExportEvent>();
        events.add(new ExportEvent(Event.NODE_ADDED, "/test"));
        events = processor.preProcessEvents(events);
        assertEquals(4, events.size());
        assertTrue(events.contains(new ExportEvent(Event.NODE_ADDED, "/test")));
        assertTrue(events.contains(new ExportEvent(Event.NODE_ADDED, "/test/foo")));
        assertTrue(events.contains(new ExportEvent(Event.NODE_ADDED, "/test/foo/foo")));
        assertTrue(events.contains(new ExportEvent(Event.NODE_ADDED, "/test/bar")));
    }
    
    @Test
    public void testRemoveRedundantNodeRemovedEvents() {
        EventPreProcessor processor = new EventPreProcessor(super.session);
        Collection<ExportEvent> events = new ArrayList<ExportEvent>();
        events.add(new ExportEvent(Event.NODE_REMOVED, "/test"));
        events.add(new ExportEvent(Event.NODE_REMOVED, "/test/foo"));
        events = processor.preProcessEvents(events);
        assertEquals(1, events.size());
        assertTrue(events.contains(new ExportEvent(Event.NODE_REMOVED, "/test")));
    }
    
    @Test
    public void testRemoveRedundantPropertyAddedEvents() {
        EventPreProcessor processor = new EventPreProcessor(super.session);
        Collection<ExportEvent> events = new ArrayList<ExportEvent>();
        events.add(new ExportEvent(Event.NODE_ADDED, "/test/bar"));
        events.add(new ExportEvent(Event.PROPERTY_ADDED, "/test/bar/bar"));
        events = processor.preProcessEvents(events);
        assertEquals(1, events.size());
        assertTrue(events.contains(new ExportEvent(Event.NODE_ADDED, "/test/bar")));
    }
    
    @Test
    public void testRemoveInvalidEvents() {
        EventPreProcessor processor = new EventPreProcessor(super.session);
        Collection<ExportEvent> events = new ArrayList<ExportEvent>();
        events.add(new ExportEvent(Event.NODE_ADDED, "/test/foo/foo"));
        events.add(new ExportEvent(Event.NODE_ADDED, "/test/foo/foo/bar"));
        events.add(new ExportEvent(Event.PROPERTY_ADDED, "/test/foo/foo/bar"));
        events.add(new ExportEvent(Event.PROPERTY_CHANGED, "/test/foo/foo/bar/baz"));
        events = processor.preProcessEvents(events);
        assertEquals(1, events.size());
        assertTrue(events.contains(new ExportEvent(Event.NODE_ADDED, "/test/foo/foo")));
    }
}
