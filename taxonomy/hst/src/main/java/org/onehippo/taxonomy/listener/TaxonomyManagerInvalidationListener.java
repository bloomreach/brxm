/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.listener;

import org.hippoecm.hst.core.jcr.GenericEventListener;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.onehippo.taxonomy.api.TaxonomyManager;

public class TaxonomyManagerInvalidationListener extends GenericEventListener {
    private Logger log = LoggerFactory.getLogger(TaxonomyManagerInvalidationListener.class);
    
    private TaxonomyManager taxonomyManager;
    
    public void setTaxonomyManager(TaxonomyManager taxonomyManager) {
        this.taxonomyManager = taxonomyManager;
    }
    
    protected void onNodeAdded(Event event) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("onNodeAdded received on {} by {}.", event.getPath(), event.getUserID());
            }    
            doInvalidation(event.getPath());
        }
        catch (RepositoryException e) {
            log.warn("Cannot retrieve the path of the event", e);
        }
    }

    protected void onNodeRemoved(Event event) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("onNodeRemoved received on {} by {}.", event.getPath(), event.getUserID());
            }
            doInvalidation(event.getPath());
        }
        catch (RepositoryException e) {
            log.warn("Cannot retrieve the path of the event", e);
        }
    }

    protected void onPropertyAdded(Event event) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("onPropertyAdded received on {} by {}.", event.getPath(), event.getUserID());
            }    
            doInvalidation(event.getPath());
        }
        catch (RepositoryException e) {
            log.warn("Cannot retrieve the path of the event", e);
        }
    }

    protected void onPropertyChanged(Event event) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("onPropertyChanged received on {} by {}.", event.getPath(), event.getUserID());
            }
            doInvalidation(event.getPath());
        }
        catch (RepositoryException e) {
            log.warn("Cannot retrieve the path of the event", e);
        }
    }

    protected void onPropertyRemoved(Event event) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("onPropertyRemoved received on {} by {}.", event.getPath(), event.getUserID());
            }    
            doInvalidation(event.getPath());
        }
        catch (RepositoryException e) {
            log.warn("Cannot retrieve the path of the event", e);
        }
    }

    private void doInvalidation(final String path) {
        taxonomyManager.invalidate(path);
    }

}
