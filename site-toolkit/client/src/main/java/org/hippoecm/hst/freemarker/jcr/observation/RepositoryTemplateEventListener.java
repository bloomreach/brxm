/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.freemarker.jcr.observation;

import java.util.Set;

import org.hippoecm.hst.core.jcr.EventListenersContainerListener;
import org.hippoecm.hst.core.jcr.GenericEventListener;
import org.hippoecm.hst.freemarker.jcr.TemplateLoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RepositoryTemplateEventListener extends GenericEventListener implements EventListenersContainerListener {

    private static final Logger log = LoggerFactory.getLogger(RepositoryTemplateEventListener.class);

    protected TemplateLoadingCache cache;

    public void setTemplateLoadingCache(TemplateLoadingCache cache) {
        this.cache = cache;
    }

    protected void removeFromCache(final Set<String> paths) {
        for (String path : paths) {
            removeFromCache(path);
        }
    }

    protected void removeFromCache(final String path) {
        log.info("Removing '{}' from template cache.", path);
        cache.remove(path);
    }

    @Override
    public void onEventListenersContainerStarted() {
        // nothing
    }

    @Override
    public void onEventListenersContainerRefreshed() {
        log.info("HST Container refreshed, clear all cached templates.");
        cache.clear();
    }

    @Override
    public void onEventListenersContainerStopped() {
        // nothing
    }
}
