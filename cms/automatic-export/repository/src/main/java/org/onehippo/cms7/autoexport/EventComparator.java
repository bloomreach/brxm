/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Comparator;

import javax.jcr.observation.Event;

/**
 * Sorts Events according to the following rules:
 * - removed events come before added events, added events come before changed events
 * - if both events are either removed, added or changed events, compare paths according to string compare
 * 
 * Note: this comparator imposes orderings that are inconsistent with equals depending on the Event implementation.
 */
class EventComparator implements Comparator<ExportEvent> {
    
    @Override
    public int compare(ExportEvent e1, ExportEvent e2) {
        int compareType = compareType(e1, e2);
        return compareType == 0 ? comparePath(e1, e2) : compareType;
    }

    private int compareType(ExportEvent e1, ExportEvent e2) {
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

    private int comparePath(ExportEvent e1, ExportEvent e2) {
        return e1.getPath().compareTo(e2.getPath());
    }
    
    private boolean isRemoveEvent(Event event) {
        return event.getType() == Event.NODE_REMOVED || event.getType() == Event.PROPERTY_REMOVED;
    }
    
    private boolean isAddedEvent(Event event) {
        return event.getType() == Event.NODE_ADDED || event.getType() == Event.PROPERTY_ADDED;
    }    
    
}
