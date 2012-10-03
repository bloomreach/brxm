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
package org.onehippo.cms7.hst.ga.config;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.hippoecm.hst.core.jcr.EventListenersContainerListener;
import org.hippoecm.hst.core.jcr.GenericEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleAnalyticsConfigurationEventListener extends GenericEventListener implements EventListenersContainerListener {

    private static Logger log = LoggerFactory.getLogger(GoogleAnalyticsConfigurationEventListener.class);
    
    private GoogleAnalyticsConfigurationImpl config;
    
    public void setConfiguration(GoogleAnalyticsConfigurationImpl config) {
        this.config = config;
    }
    
    public void onEvent(EventIterator events) {
        Event invalidationEvent = null;

        while (events.hasNext()) {
            Event event = events.nextEvent();

            try {
                if (isEventOnSkippedPath(event)) {
                    continue;
                }
            } catch (RepositoryException e) {
                continue;
            }

            invalidationEvent = event;
            break;
        }

        if (invalidationEvent != null) {
            log.debug("Invalidating Google Analytics configuration");
            config.invalidate();
        }
    }
    
    @Override
    public void onEventListenersContainerStarted() {}

    @Override
    public void onEventListenersContainerRefreshed() {
        config.invalidate();
    }

    @Override
    public void onEventListenersContainerStopped() {}

}
