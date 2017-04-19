/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.resourcebundle.internal;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.hippoecm.hst.core.jcr.EventListenersContainerListener;
import org.hippoecm.hst.core.jcr.GenericEventListener;
import org.hippoecm.hst.resourcebundle.ResourceBundleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HippoRepositoryResourceBundleEventListener
 * <P>
 * This event listener listens to the specified JCR events (as configured in spring configuration, for instance),
 * and unregisters changed resource bundle families from the resource bundle registry.
 * </P>
 */
public class HippoRepositoryResourceBundleEventListener extends GenericEventListener implements EventListenersContainerListener {

    private static Logger log = LoggerFactory.getLogger(HippoRepositoryResourceBundleEventListener.class);

    private final ResourceBundleRegistry resourceBundleRegistry;

    public HippoRepositoryResourceBundleEventListener(final ResourceBundleRegistry resourceBundleRegistry) {
        this.resourceBundleRegistry = resourceBundleRegistry;
    }

    @Override
    public void onEvent(EventIterator events) {
        if (resourceBundleRegistry instanceof MutableResourceBundleRegistry) {
            final MutableResourceBundleRegistry registry = (MutableResourceBundleRegistry) resourceBundleRegistry;

            while (events.hasNext()) {
                final Event event = events.nextEvent();
                try {
                    final String identifier = event.getIdentifier();

                    // figuring out to which variant (live, preview, draft) the event pertains to is too complex/costly.
                    // instead, we (try to) evict the identifier from both the live and the preview cache.
                    registry.unregisterBundleFamily(identifier, true);
                    registry.unregisterBundleFamily(identifier, false);
                } catch (RepositoryException e) {
                    log.warn("Failed to retrieve path for JCR event '{}'.", event, e);
                }
            }
        }
    }

    @Override
    public void onEventListenersContainerStarted() {
        // do nothing
    }

    @Override
    public void onEventListenersContainerRefreshed() {
        // event listener is reconnected: Because we might have missed changes, we need
        // to unregister everything from the resourceBundleRegistry
        if (resourceBundleRegistry instanceof MutableResourceBundleRegistry) {
            ((MutableResourceBundleRegistry) resourceBundleRegistry).unregisterAllBundleFamilies();
        }
    }

    @Override
    public void onEventListenersContainerStopped() {
        // do nothing
    }
}
