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
package org.hippoecm.hst.core.jcr;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <CODE>GenericEventListener</CODE> class provides a default implementation for
 * the {@link EventListener} interface.
 * This receives an event and dispatches each event to a specialized method.
 * The child class of this class can override some methods which are related to
 * its own interests.
 */
public class GenericEventListener implements EventListener {

    private static final Logger log = LoggerFactory.getLogger(GenericEventListener.class);
    // By default, not interested in events from the logging in the repository or the version environment
    protected String [] skipPaths = new String [] { "/jcr:system", "/hippo:log" };
    
    public String [] getSkipPaths() {
        if (skipPaths == null) {
            return null;
        }
        
        String [] cloned = new String[skipPaths.length];
        System.arraycopy(skipPaths, 0, cloned, 0, skipPaths.length);
        return cloned;
    }
    
    public void setSkipPaths(String [] skipPaths) {
        if (skipPaths == null) {
            this.skipPaths = null;
        } else {
            this.skipPaths = new String[skipPaths.length];
            System.arraycopy(skipPaths, 0, this.skipPaths, 0, skipPaths.length);
        }
    }

    protected boolean isEventOnSkippedPath(Event event) throws RepositoryException {
        if (skipPaths == null || skipPaths.length == 0) {
            return false;
        }

        String eventPath = event.getPath();

        for (String skipPath : skipPaths) {
            if (eventPath.startsWith(skipPath)) {
                return true;
            }
        }

        return false;
    }

    protected boolean eventIgnorable(Event event) throws RepositoryException {
        if (HippoNodeType.HIPPO_IGNORABLE.equals(event.getUserData())) {
            log.debug("Ignore event '{}' because user data is equal to {}",
                    event.getPath(), HippoNodeType.HIPPO_IGNORABLE);
            return true;
        }
        return false;
    }
    
    public void onEvent(EventIterator events) {
        while (events.hasNext()) {
            Event event = events.nextEvent();

            try {
                if (isEventOnSkippedPath(event)) {
                    continue;
                }
                if (eventIgnorable(event)) {
                    continue;
                }
            } catch (RepositoryException e) {
                continue;
            }
            
            int type = event.getType();
            
            switch (type) {
            case Event.NODE_ADDED:
                onNodeAdded(event);
                break;
            case Event.NODE_REMOVED:
                onNodeRemoved(event);
                break;
            case Event.PROPERTY_ADDED:
                onPropertyAdded(event);
                break;
            case Event.PROPERTY_CHANGED:
                onPropertyChanged(event);
                break;
            case Event.PROPERTY_REMOVED:
                onPropertyRemoved(event);
                break;
            }
        }
    }
    
    protected void onNodeAdded(Event event) {
    }

    protected void onNodeRemoved(Event event) {
    }
    
    protected void onPropertyAdded(Event event) {
    }
    
    protected void onPropertyChanged(Event event) {
    }
    
    protected void onPropertyRemoved(Event event) {
    }
    
}
