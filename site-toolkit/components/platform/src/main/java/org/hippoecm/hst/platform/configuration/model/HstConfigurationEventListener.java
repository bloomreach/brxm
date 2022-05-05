/*
 *  Copyright 2008-2022 Hippo B.V. (http://www.onehippo.com)
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

import java.util.concurrent.atomic.AtomicLong;

import javax.jcr.observation.EventIterator;

import org.hippoecm.hst.core.jcr.GenericEventListener;
import org.hippoecm.hst.platform.configuration.cache.HstEventsCollector;
import org.hippoecm.hst.platform.model.HstModelImpl;

public class HstConfigurationEventListener extends GenericEventListener {

    private HstModelImpl hstModelImpl;
    private HstEventsCollector hstEventsCollector;

    private final AtomicLong counter = new AtomicLong();

    public HstConfigurationEventListener(final HstModelImpl hstModelImpl, final HstEventsCollector hstEventsCollector) {
        this.hstModelImpl = hstModelImpl;
        this.hstEventsCollector = hstEventsCollector;
    }

    @Override
    public void onEvent(EventIterator events) {
        synchronized(hstModelImpl) {
            counter.incrementAndGet();
            hstEventsCollector.collect(events);
            if (hstEventsCollector.hasEvents()) {
                hstModelImpl.invalidate();
            }
        }
    }

    public long getOnEventCount() {
        return counter.get();
    }
}
