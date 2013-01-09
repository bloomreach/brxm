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

import org.hippoecm.hst.core.jcr.EventListenersContainerListener;
import org.hippoecm.hst.core.jcr.GenericEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstConfigurationEventListener extends GenericEventListener implements EventListenersContainerListener {

    protected HstManager hstManager;
    
    public void setHstManager(HstManager hstManager) {
        this.hstManager = hstManager;
    }

    @Override
    public void onEvent(EventIterator events) {
        hstManager.invalidate(events);
    }
    
    public void onEventListenersContainerStarted() {
        // do nothing
    }
    
    public void onEventListenersContainerRefreshed() {
        // event listener is reconnected: Because we might have missed changes, we need
        // to invalidate everything from the hstManager
        hstManager.invalidateAll();
    }
    
    public void onEventListenersContainerStopped() {
        // do nothing
    }
}
