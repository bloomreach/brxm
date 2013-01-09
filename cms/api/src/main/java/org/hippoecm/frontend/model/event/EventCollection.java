/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.model.event;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EventCollection<T extends IEvent> implements Iterable<T>, Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> events;

    public EventCollection() {
        this.events = new ArrayList<T>();
    }

    public EventCollection(Iterator<? extends T> eventIter) {
        this.events = new ArrayList<T>();
        while (eventIter.hasNext()) {
            events.add(eventIter.next());
        }
    }

    public void add(T event) {
        events.add(event);
    }

    public int size() {
        return events.size();
    }

    public Iterator<T> iterator() {
        return events.iterator();
    }

}
