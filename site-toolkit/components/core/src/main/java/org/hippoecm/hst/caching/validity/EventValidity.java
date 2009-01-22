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
package org.hippoecm.hst.caching.validity;

import org.hippoecm.hst.caching.Event;

public class EventValidity implements SourceValidity {

    private static final long serialVersionUID = 1L;
    
    private Event m_event;

    public EventValidity(Event ev) {
        m_event = ev;
    }

   
    public Event getEvent() {
        return m_event;
    }

   
    public int isValid() {
        return VALID;
    }

    public int isValid(SourceValidity sv) {
        if (sv instanceof EventValidity) {
            return VALID;
        }
        return INVALID;
    }

    public boolean equals(Object o) {
        if (o instanceof EventValidity) {
            return m_event.equals(((EventValidity) o).getEvent());
        }
        return false;
    }

    public int hashCode() {
        return m_event.hashCode();
    }

    public String toString() {
        return "EventValidity[" + m_event + "]";
    }
}
