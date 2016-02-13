/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.nodetypes;

import java.util.concurrent.atomic.AtomicInteger;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

public class NodeTypesChangeTracker {

    private final Session session;
    private final static AtomicInteger counter = new AtomicInteger(0);

    private final EventListener nodeTypesChangeListener = new EventListener() {
        @Override
        public void onEvent(final EventIterator events) {
            counter.incrementAndGet();
        }
    };

    public NodeTypesChangeTracker(Session session) {
        this.session = session;
    }

    public void start() throws RepositoryException {
        session.getWorkspace().getObservationManager().addEventListener(nodeTypesChangeListener,
                Event.NODE_ADDED | Event.NODE_REMOVED | Event.NODE_MOVED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED,
                "/jcr:system/jcr:nodeTypes", true, null, null, false);
    }

    public void stop() {
        // note session logout already removes the nodeTypesChangeListener
        session.logout();
    }

    public static int getChangesCounter() {
        return counter.get();
    }

}
