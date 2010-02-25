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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

/**
 * RewindableEventIterator
 * <P>
 * An implementation of <CODE>javax.jcr.observation.EventIterator</CODE>,
 * which has the ability to rewind to the beginning.
 * </P>
 * <P>
 * <EM>Caution: Because this tries to copy all events initially, it can cause performance problems.</EM>
 * </P>
 * 
 * @version $Id$
 */
public class RewindableEventIterator implements EventIterator {
    
    private List<Event> eventList;
    private Iterator<Event> eventListIterator;
    private int position;
    private int size;
    
    public RewindableEventIterator(EventIterator events) {
        this(events, 0, null);
    }
    
    public RewindableEventIterator(EventIterator events, int max, String [] skipPaths) {
        eventList = new ArrayList<Event>();
        
        while (events.hasNext()) {
            if (max > 0 && eventList.size() == max) {
                break;
            }
            
            Event event = events.nextEvent();
            
            try {
                if (isEventOnSkippedPath(event, skipPaths)) {
                    continue;
                }
            } catch (RepositoryException e) {
                continue;
            }
            
            eventList.add(event);
        }
        
        rewind();
    }
    
    public void rewind() {
        eventListIterator = eventList.iterator();
        position = 0;
        size = eventList.size();
    }
    
    public Event nextEvent() {
        Event event = eventListIterator.next();
        ++position;
        return event;
    }
    
    public long getPosition() {
        return position;
    }
    
    public long getSize() {
        return size;
    }
    
    public void skip(long skipNum) {
        for (long l = 0; l < skipNum; l++) {
            nextEvent();
        }
    }
    
    public boolean hasNext() {
        return eventListIterator.hasNext();
    }
    
    public Object next() {
        return nextEvent();
    }
    
    public void remove() {
        eventListIterator.remove();
    }
    
    protected boolean isEventOnSkippedPath(Event event, String [] skipPaths) throws RepositoryException {
        if (skipPaths == null || skipPaths.length == 0) {
            return false;
        }
        
        String eventPath = event.getPath();
        
        for (String skipPath : skipPaths) {
            if (eventPath.startsWith(skipPath)) {
                return true;
            }
        }
        
        return false;
    }
}
