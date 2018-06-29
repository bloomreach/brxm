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
package org.hippoecm.hst.platform.configuration.model;

import javax.jcr.observation.EventIterator;

import org.hippoecm.hst.platform.configuration.cache.HstEventsCollector;
import org.hippoecm.hst.core.jcr.EventListenersContainerListener;
import org.hippoecm.hst.core.jcr.GenericEventListener;
import org.hippoecm.hst.platform.model.HstModelImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstConfigurationEventListener extends GenericEventListener {

    private HstModelImpl hstModelImpl;
    private HstEventsCollector hstEventsCollector;

    public HstConfigurationEventListener(final HstModelImpl hstModelImpl, final HstEventsCollector hstEventsCollector) {
        this.hstModelImpl = hstModelImpl;
        this.hstEventsCollector = hstEventsCollector;
    }

    @Override
    public void onEvent(EventIterator events) {
        synchronized(hstModelImpl) {
            hstEventsCollector.collect(events);
            if (hstEventsCollector.hasEvents()) {
                hstModelImpl.invalidate();
            }
        }
    }

}
