/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.core.jcr;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.junit.Before;
import org.junit.Test;

/**
 * TestRewindableEventIterator
 * 
 * @version $Id$
 */
public class TestRewindableEventIterator {
    
    private List<Event> events;
    
    @Before
    public void setUp() {
        events = new ArrayList<Event>();
        
        for (int i = 0; i < 10; i++) {
            Event event = createMock(Event.class);
            events.add(event);
        }
    }
    
    @Test
    public void testRewinding() throws Exception {
        EventIterator eventIterator = createMock(EventIterator.class);
        
        for (int i = 0; i < events.size(); i++) {
            expect(eventIterator.hasNext()).andReturn(true);
            expect(eventIterator.nextEvent()).andReturn(events.get(i));
        }
        expect(eventIterator.hasNext()).andReturn(false);
        
        replay(eventIterator);
        
        RewindableEventIterator rewindableEventIterator = new RewindableEventIterator(eventIterator);
        
        assertEquals(events.size(), rewindableEventIterator.getSize());
        
        int index = 0;
        while (rewindableEventIterator.hasNext()) {
            assertEquals((long) index, rewindableEventIterator.getPosition());
            Event event = rewindableEventIterator.nextEvent();
            assertEquals(events.get(index), event);
            ++index;
        }
        
        rewindableEventIterator.rewind();
        
        assertEquals(events.size(), rewindableEventIterator.getSize());
        
        index = 0;
        while (rewindableEventIterator.hasNext()) {
            assertEquals((long) index, rewindableEventIterator.getPosition());
            Event event = rewindableEventIterator.nextEvent();
            assertEquals(events.get(index), event);
            ++index;
        }

        rewindableEventIterator.rewind();
        rewindableEventIterator.skip(events.size() / 2);
        
        assertEquals(events.size(), rewindableEventIterator.getSize());
        
        index = events.size() / 2;
        while (rewindableEventIterator.hasNext()) {
            assertEquals((long) index, rewindableEventIterator.getPosition());
            Event event = rewindableEventIterator.nextEvent();
            assertEquals(events.get(index), event);
            ++index;
        }
    }
    
}
