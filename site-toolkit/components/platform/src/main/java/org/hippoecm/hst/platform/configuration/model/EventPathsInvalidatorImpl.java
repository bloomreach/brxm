/*
 *  Copyright 2013-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration.model;

import org.hippoecm.hst.platform.configuration.cache.HstEventsCollector;
import org.hippoecm.hst.platform.api.model.EventPathsInvalidator;
import org.hippoecm.hst.platform.model.HstModelImpl;

/**
 * To dispatch events (for example from a HippoSession) directly, for example without having persisted the changes yet. This is important
 * in case the changes MUST be reflected in the model, and can't wait for the *asynchronous* jcr events to arrive
 */
public class EventPathsInvalidatorImpl implements EventPathsInvalidator {

    private final HstModelImpl hstModelImpl;
    private final HstEventsCollector hstEventsCollector;

    public EventPathsInvalidatorImpl(final HstModelImpl hstModelImpl, final HstEventsCollector hstEventsCollector) {
        this.hstModelImpl = hstModelImpl;
        this.hstEventsCollector = hstEventsCollector;
    }

    @Override
    public void eventPaths(final String... absEventPaths) {
        synchronized(hstModelImpl) {
            hstEventsCollector.collect(absEventPaths);
            if (hstEventsCollector.hasEvents()) {
                hstModelImpl.invalidate();
            }
        }
    }

    /**
     * used by downstream project for testing purposes
     */
    @SuppressWarnings("unused")
    public HstEventsCollector getHstEventsCollector() {
        return hstEventsCollector;
    }
}
