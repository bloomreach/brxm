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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.observation.Event;

import org.junit.Test;

import static org.junit.Assert.*;

public class EventComparatorTest {
    
    @Test
    public void testStringCompareOnIdenticalTypes() {
        List<ExportEvent> events = new ArrayList<ExportEvent>();
        events.add(new ExportEvent(Event.NODE_REMOVED, "/foo"));
        events.add(new ExportEvent(Event.NODE_REMOVED, "/foo/bar"));
        events.add(new ExportEvent(Event.PROPERTY_REMOVED, "/foo/baz"));
        events.add(new ExportEvent(Event.NODE_REMOVED, "/foo/quux"));
        events.add(new ExportEvent(Event.PROPERTY_REMOVED, "/foo/qux"));
        
        List<ExportEvent> expected = new ArrayList<ExportEvent>(events);
        Collections.sort(events, new EventComparator());
        
        assertEquals(expected, events);
    }
    
    public void testRemovedBeforeAddedBeforeChanged() {
        List<ExportEvent> events = new ArrayList<ExportEvent>();
        events.add(new ExportEvent(Event.NODE_REMOVED, "/foo"));
        events.add(new ExportEvent(Event.NODE_REMOVED, "/foo/bar"));
        events.add(new ExportEvent(Event.NODE_ADDED, "/baz"));
        events.add(new ExportEvent(Event.NODE_ADDED, "/baz/qux"));
        events.add(new ExportEvent(Event.PROPERTY_CHANGED, "/bar"));
        
        List<ExportEvent> expected = new ArrayList<ExportEvent>(events);
        Collections.sort(events, new EventComparator());
        
        assertEquals(expected, events);
    }
}
