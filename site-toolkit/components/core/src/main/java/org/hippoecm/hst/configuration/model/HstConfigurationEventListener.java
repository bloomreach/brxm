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
package org.hippoecm.hst.configuration.model;

import javax.jcr.observation.EventIterator;

import org.hippoecm.hst.configuration.cache.HstEventsCollector;
import org.hippoecm.hst.core.jcr.EventListenersContainerListener;
import org.hippoecm.hst.core.jcr.GenericEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstConfigurationEventListener extends GenericEventListener implements EventListenersContainerListener {

    private static final Logger log = LoggerFactory.getLogger(HstConfigurationEventListener.class);

    private Object hstModelMutex;
    private HstEventsCollector hstEventsCollector;

    private HstManager hstManager;

    // TODO HSTTWO-4355 get rid of the hstManager : This should live in the platform webapp!
    public void setHstManager(HstManager hstManager) {
        this.hstManager = hstManager;
    }

    public void setHstModelMutex(Object hstModelMutex) {
        this.hstModelMutex = hstModelMutex;
    }

    public void setHstEventsCollector(HstEventsCollector hstEventsCollector) {
        this.hstEventsCollector = hstEventsCollector;
    }

    @Override
    public void onEvent(EventIterator events) {
        synchronized(hstModelMutex) {
            hstEventsCollector.collect(events);
            if (hstEventsCollector.hasEvents()) {
                // TODO HSTTWO-4355 get rid of the hstManager : This should live in the platform webapp!
                hstManager.markStale();
            }
        }
    }
    
    public void onEventListenersContainerStarted() {
        // do nothing
    }
    
    public void onEventListenersContainerRefreshed() {
        // do nothing
    }
    
    public void onEventListenersContainerStopped() {
        // do nothing
    }
}
