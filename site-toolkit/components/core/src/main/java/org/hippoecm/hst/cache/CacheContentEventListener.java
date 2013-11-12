/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.cache;

import javax.jcr.RepositoryException;
import javax.jcr.observation.EventIterator;

import org.hippoecm.hst.cache.ehcache.HstCacheEhCacheImpl;
import org.hippoecm.hst.core.jcr.EventListenersContainerListener;
import org.hippoecm.hst.core.jcr.GenericEventListener;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheContentEventListener extends GenericEventListener implements EventListenersContainerListener {

    private static final Logger log = LoggerFactory.getLogger(CacheContentEventListener.class);

    private HstCache pageCache;

    public void setPageCache(HstCacheEhCacheImpl pageCache) {
        this.pageCache = pageCache;
    }

    @Override
    public void onEvent(EventIterator events) {
        // we cannot do better than flush entire cache on content changes.
        while (events.hasNext()) {
            try {
                if (!HippoNodeType.HIPPO_IGNORABLE.equals(events.nextEvent().getUserData())) {
                    pageCache.clear();
                    return;
                }
            } catch (RepositoryException e) {
               log.error("Error processing event");
            }
        }
    }

    @Override
    public void onEventListenersContainerStarted() {
        // do nothing
    }

    @Override
    public void onEventListenersContainerRefreshed() {
        pageCache.clear();
    }
    @Override
    public void onEventListenersContainerStopped() {
        // do nothing
    }

}
