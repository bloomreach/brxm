/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.cache.webfiles;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.hippoecm.hst.cache.CompositeHstCache;
import org.hippoecm.hst.cache.HstCache;
import org.hippoecm.hst.cache.PageCacheEventListener;
import org.hippoecm.hst.core.container.RequestInfoCacheKeyFragmentCreator;
import org.hippoecm.hst.core.container.WebFileValve;
import org.hippoecm.hst.core.jcr.GenericEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener that on change makes sure the page cache is flushed *and* the web files cache evicts possibly a web file
 * if the web file was cached but got changed
 */
public class WebFilesEventListener extends GenericEventListener {

    private static final Logger log = LoggerFactory.getLogger(PageCacheEventListener.class);

    private HstCache pageCache;
    private WebFileValve webFileValve;
    private RequestInfoCacheKeyFragmentCreator requestInfoCacheKeyFragmentCreator;

    public void setPageCache(CompositeHstCache pageCache) {
        this.pageCache = pageCache;
    }

    public void setWebFileValve(final WebFileValve webFileValve) {
        this.webFileValve = webFileValve;
    }

    public void setRequestInfoCacheKeyFragmentCreator(final RequestInfoCacheKeyFragmentCreator requestInfoCacheKeyFragmentCreator) {
        this.requestInfoCacheKeyFragmentCreator = requestInfoCacheKeyFragmentCreator;
    }
    @Override
    public void onEvent(EventIterator events) {
        boolean pageCacheCleared = false;
        while (events.hasNext()) {
            try {
                final Event event = events.nextEvent();
                if (eventIgnorable(event)) {
                    continue;
                }
                if (!pageCacheCleared) {
                    pageCacheCleared = true;
                    pageCache.clear();
                }
                if (requestInfoCacheKeyFragmentCreator != null) {
                    requestInfoCacheKeyFragmentCreator.reset();
                }
                webFileValve.onEvent(event);
            } catch (RepositoryException e) {
                log.error("Error processing event");
            }
        }
    }

}
