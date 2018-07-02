/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.model;

import java.util.Arrays;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.model.EventPathsInvalidator;
import org.hippoecm.hst.platform.configuration.cache.HstConfigurationLoadingCache;
import org.hippoecm.hst.platform.configuration.cache.HstEventConsumer;
import org.hippoecm.hst.platform.configuration.cache.HstEventsCollector;
import org.hippoecm.hst.platform.configuration.cache.HstEventsDispatcher;
import org.hippoecm.hst.platform.configuration.cache.HstNodeLoadingCache;
import org.hippoecm.hst.platform.configuration.model.EventPathsInvalidatorImpl;
import org.hippoecm.hst.platform.configuration.model.HstConfigurationEventListener;

import static javax.jcr.observation.Event.NODE_ADDED;
import static javax.jcr.observation.Event.NODE_MOVED;
import static javax.jcr.observation.Event.NODE_REMOVED;
import static javax.jcr.observation.Event.PERSIST;
import static javax.jcr.observation.Event.PROPERTY_ADDED;
import static javax.jcr.observation.Event.PROPERTY_CHANGED;
import static javax.jcr.observation.Event.PROPERTY_REMOVED;

public class InvalidationMonitor {

    private final Session session;
    private final HstConfigurationEventListener hstConfigurationEventListener;
    private final EventPathsInvalidator eventPathsInvalidator;
    private final HstEventsDispatcher hstEventsDispatcher;


    public InvalidationMonitor(final Session session,
                               final HstNodeLoadingCache hstNodeLoadingCache,
                               final HstConfigurationLoadingCache hstConfigurationLoadingCache,
                               final HstModelImpl hstModelImpl) throws RepositoryException {

        this.session = session;

        final HstEventsCollector hstEventsCollector = new HstEventsCollector(hstNodeLoadingCache.getRootPath());

        // jcr listener to collect hst events in 'hstEventsCollector'
        hstConfigurationEventListener = new HstConfigurationEventListener(hstModelImpl, hstEventsCollector);

        // EventPathsInvalidatorImpl to be able to push events manually and to collect in the hstEventsCollector
        eventPathsInvalidator = new EventPathsInvalidatorImpl(hstModelImpl, hstEventsCollector);

        session.getWorkspace().getObservationManager().addEventListener(hstConfigurationEventListener,
                getAnyEventType(), hstNodeLoadingCache.getRootPath(), true, null, null, false);

        hstEventsDispatcher = new HstEventsDispatcher(hstEventsCollector, Arrays.asList(new HstEventConsumer[]{hstNodeLoadingCache, hstConfigurationLoadingCache}));
    }

    protected void dispatchHstEvents() {
        hstEventsDispatcher.dispatchHstEvents();
    }

    protected void destroy() throws RepositoryException {
        session.getWorkspace().getObservationManager().removeEventListener(hstConfigurationEventListener);

    }

    protected EventPathsInvalidator getEventPathsInvalidator() {
        return eventPathsInvalidator;
    }

    private int getAnyEventType() {
        return NODE_ADDED | NODE_MOVED | NODE_REMOVED | PROPERTY_ADDED | PROPERTY_CHANGED | PROPERTY_REMOVED | PERSIST;
    }
}
