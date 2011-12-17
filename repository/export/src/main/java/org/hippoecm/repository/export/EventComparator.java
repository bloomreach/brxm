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
package org.hippoecm.repository.export;

import java.util.Comparator;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sorts Events according to the following rules:
 * - removed events come before added events, added events come before changed events
 * - if both events are either removed, added or changed events, compare paths according to string compare
 * 
 * Note: this comparator imposes orderings that are inconsistent with equals depending on the Event implementation.
 */
class EventComparator implements Comparator<Event> {
    
    private static final Logger log = LoggerFactory.getLogger("org.hippoecm.repository.export");
    
    @Override
    public int compare(Event e1, Event e2) {
        int compareType = compareType(e1, e2);
        return compareType == 0 ? comparePath(e1, e2) : compareType;
    }

    private int compareType(Event e1, Event e2) {
        if (isRemoveEvent(e1)) {
            return isRemoveEvent(e2) ? 0 : -1;
        } 
        if (isRemoveEvent(e2)) {
            // e1 is added or changed event
            return 1;
        }
        if (isAddedEvent(e1)) {
            return isAddedEvent(e2) ? 0 : -1;
        }
        if (isAddedEvent(e2)) {
            // e1 is changed event
            return 1;
        }
        // e1 and e2 are changed events
        return 0;
    }

    private int comparePath(Event e1, Event e2) {
        try {
            return e1.getPath().compareTo(e2.getPath());
        } catch (RepositoryException e) {
            log.error(e.getClass().getName()+": "+e.getMessage(), e);
        }
        return 0;
    }
    
    private boolean isRemoveEvent(Event event) {
        return event.getType() == Event.NODE_REMOVED || event.getType() == Event.PROPERTY_REMOVED;
    }
    
    private boolean isAddedEvent(Event event) {
        return event.getType() == Event.NODE_ADDED || event.getType() == Event.PROPERTY_ADDED;
    }    
    
}