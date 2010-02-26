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
package org.hippoecm.hst.configuration;

import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.hippoecm.hst.core.container.HstComponentRegistry;
import org.hippoecm.hst.core.jcr.GenericEventListener;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstSitesConfigurationEventListener extends GenericEventListener {
    
    static Logger log = LoggerFactory.getLogger(HstSitesConfigurationEventListener.class);
    
    protected List<HstSitesManager> sitesManagers;
    protected HstComponentRegistry componentRegistry;
    protected HstSiteMapMatcher hstSiteMapMatcher;
    
    public void setSitesManagers(List<HstSitesManager> sitesManagers) {
        this.sitesManagers = sitesManagers;
    }
    
    public void setSiteMapMatcher(HstSiteMapMatcher hstSiteMapMatcher) {
        this.hstSiteMapMatcher = hstSiteMapMatcher;
    }
    
    public void setComponentRegistry(HstComponentRegistry componentRegistry) {
        this.componentRegistry = componentRegistry;
    }

    public void onEvent(EventIterator events) {
        Event invaliationEvent = null;
        
        while (events.hasNext()) {
            Event event = events.nextEvent();

            try {
                if (isEventOnSkippedPath(event)) {
                    continue;
                }
            } catch (RepositoryException e) {
                continue;
            }
            
            invaliationEvent = event;
            break;
        }
        
        if (invaliationEvent != null) {
            try {
                if (log.isDebugEnabled()) log.debug("Event received on {}.", invaliationEvent.getPath());
                doInvalidation();
            } catch (RepositoryException e) {
                if (log.isWarnEnabled()) log.warn("Cannot retreive the path of the event: {}", e.getMessage());
            }
        }
    }
    
    private void doInvalidation() {
        this.componentRegistry.unregisterAllComponents();
        for(HstSitesManager s: sitesManagers) {
            s.invalidate();
        }
        this.hstSiteMapMatcher.invalidate();
    }
}
