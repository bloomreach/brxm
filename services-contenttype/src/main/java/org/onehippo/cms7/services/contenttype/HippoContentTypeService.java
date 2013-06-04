/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.services.contenttype;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HippoContentTypeService implements ContentTypeService {

    static final Logger log = LoggerFactory.getLogger(HippoContentTypeService.class);

    private Session serviceSession;
    private EffectiveNodeTypesCache entCache;
    private ContentTypesCache ctCache;

    private EventListener nodeTypesChangeListener = new EventListener() {
        @Override
        public void onEvent(final EventIterator events) {
            synchronized (HippoContentTypeService.this) {
                // delete caches to be rebuild again on next invocation
                entCache = null;
                ctCache = null;
            }
        }
    };

    private EventListener contentTypesChangeListener = new EventListener() {
        @Override
        public void onEvent(final EventIterator events) {
            // TODO: make it more finegrained by only reacting to changes of 'committed' document types?
            synchronized (HippoContentTypeService.this) {
                // delete caches to be rebuild again on next invocation
                ctCache = null;
            }
        }
    };

    public HippoContentTypeService(Session serviceSession) throws RepositoryException {
        this.serviceSession = serviceSession;

        // register our nodeTypesChangeListener
        serviceSession.getWorkspace().getObservationManager().addEventListener(nodeTypesChangeListener,
                Event.NODE_ADDED|Event.NODE_REMOVED|Event.NODE_MOVED|Event.PROPERTY_ADDED|Event.PROPERTY_CHANGED|Event.PROPERTY_REMOVED,
                "/jcr:system/jcr:nodeTypes", true, null, null, false);

        // register our contentTypesChangeListener
        serviceSession.getWorkspace().getObservationManager().addEventListener(contentTypesChangeListener,
                Event.NODE_ADDED|Event.NODE_REMOVED|Event.NODE_MOVED|Event.PROPERTY_ADDED|Event.PROPERTY_CHANGED|Event.PROPERTY_REMOVED,
                "/hippo:namespaces", true, null, null, false);
    }

    public synchronized void shutdown() {
        try {
            serviceSession.getWorkspace().getObservationManager().removeEventListener(contentTypesChangeListener);
            serviceSession.getWorkspace().getObservationManager().removeEventListener(nodeTypesChangeListener);
        } catch (RepositoryException e) {
            // ignore
        }
        ctCache = null;
        entCache = null;
        serviceSession = null;
    }

    @Override
    public synchronized EffectiveNodeTypesCache getEffectiveNodeTypes() throws RepositoryException {
        if (entCache == null) {
            entCache = new EffectiveNodeTypesCache(serviceSession);
            // TODO: check if not already changed again
        }
        return entCache;
    }

    @Override
    public synchronized ContentTypesCache getContentTypes() throws RepositoryException {
        if (ctCache == null) {
            ctCache = new ContentTypesCache(serviceSession, getEffectiveNodeTypes());
            // TODO: check if not already changed again
        }
        return ctCache;
    }
}
