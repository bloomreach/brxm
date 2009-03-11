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

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.hippoecm.hst.core.container.HstComponentRegistry;
import org.hippoecm.hst.core.jcr.GenericEventListener;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstSitesConfigurationEventListener extends GenericEventListener {
    
    static Logger log = LoggerFactory.getLogger(HstSitesConfigurationEventListener.class);
    
    protected HstSitesManager sitesManager;
    protected HstComponentRegistry componentRegistry;
    protected HstSiteMapMatcher hstSiteMapMatcher;
    
    public void setSitesManager(HstSitesManager sitesManager) {
        this.sitesManager = sitesManager;
    }
    
    public void setSiteMapMatcher(HstSiteMapMatcher hstSiteMapMatcher) {
        this.hstSiteMapMatcher = hstSiteMapMatcher;
    }
    
    public void setComponentRegistry(HstComponentRegistry componentRegistry) {
        this.componentRegistry = componentRegistry;
    }

    protected void onNodeAdded(Event event) {
        try {
            if (log.isDebugEnabled()) log.debug("Event received on {} by {}.", event.getPath(), event.getUserID());
            doInvalidation();
        } catch (RepositoryException e) {
            if (log.isWarnEnabled()) log.warn("Cannot retreive the path of the event: {}", e.getMessage());
        }
    }

    protected void onNodeRemoved(Event event) {
        try {
            if (log.isDebugEnabled()) log.debug("Event received on {} by {}.", event.getPath(), event.getUserID());
            doInvalidation();
        } catch (RepositoryException e) {
            if (log.isWarnEnabled()) log.warn("Cannot retreive the path of the event: {}", e.getMessage());
        }
    }
    
    protected void onPropertyAdded(Event event) {
        try {
            if (log.isDebugEnabled()) log.debug("Event received on {} by {}.", event.getPath(), event.getUserID());
            doInvalidation();
        } catch (RepositoryException e) {
            if (log.isWarnEnabled()) log.warn("Cannot retreive the path of the event: {}", e.getMessage());
        }
    }
    
    protected void onPropertyChanged(Event event) {
        try {
            if (log.isDebugEnabled()) log.debug("Event received on {} by {}.", event.getPath(), event.getUserID());
            doInvalidation();
        } catch (RepositoryException e) {
            if (log.isWarnEnabled()) log.warn("Cannot retreive the path of the event: {}", e.getMessage());
        }
    }
    
    protected void onPropertyRemoved(Event event) {
        try {
            if (log.isDebugEnabled()) log.debug("Event received on {} by {}.", event.getPath(), event.getUserID());
            doInvalidation();
        } catch (RepositoryException e) {
            if (log.isWarnEnabled()) log.warn("Cannot retreive the path of the event: {}", e.getMessage());
        }
    }

    private void doInvalidation() {
        this.componentRegistry.unregisterAllComponents();
        this.sitesManager.invalidate();
        this.hstSiteMapMatcher.invalidate();
    }
}
