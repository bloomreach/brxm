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

import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LazySessionDelegatingRepositorySessionsRefreshEventListener extends GenericEventListener {
    
    static Logger log = LoggerFactory.getLogger(LazySessionDelegatingRepositorySessionsRefreshEventListener.class);
    
    protected List<LazySessionDelegatingRepository> lazySessionDelegatingRepositories;
    
    public void setLazySessionDelegatingRepositories(List<LazySessionDelegatingRepository> lazySessionDelegatingRepositories) {
        this.lazySessionDelegatingRepositories = lazySessionDelegatingRepositories;
    }
    
    public void onEvent(EventIterator events) {
        boolean refreshSessions = false;
        
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
            
            refreshSessions = true;
            break;
        }
        
        if (refreshSessions) {
            log.debug("Event received. Refreshing sessions.");
            doRefreshing();
        }
    }
    
    private void doRefreshing() {
        long currentTimeMillis = System.currentTimeMillis();
        
        if (lazySessionDelegatingRepositories != null && !lazySessionDelegatingRepositories.isEmpty()) {
            for (LazySessionDelegatingRepository lazySessionDelegatingRepository : lazySessionDelegatingRepositories) {
                lazySessionDelegatingRepository.setSessionsRefreshPendingAfter(currentTimeMillis);
            }
        }
    }
}
