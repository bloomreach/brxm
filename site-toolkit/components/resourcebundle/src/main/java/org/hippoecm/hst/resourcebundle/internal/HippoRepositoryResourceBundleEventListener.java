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
package org.hippoecm.hst.resourcebundle.internal;

import javax.jcr.observation.EventIterator;

import org.hippoecm.hst.core.jcr.EventListenersContainerListener;
import org.hippoecm.hst.core.jcr.GenericEventListener;
import org.hippoecm.hst.resourcebundle.ResourceBundleRegistry;

/**
 * HippoRepositoryResourceBundleEventListener
 * <P>
 * This event listener listens to the specified JCR events (as configured in spring configuration, for instance),
 * and simply unregisters all the resource bundle families from the resource bundle registry.
 * </P>
 */
public class HippoRepositoryResourceBundleEventListener extends GenericEventListener implements EventListenersContainerListener {

    private ResourceBundleRegistry resourceBundleRegistry;

    public ResourceBundleRegistry getResourceBundleRegistry() {
        return resourceBundleRegistry;
    }

    public void setResourceBundleRegistry(ResourceBundleRegistry resourceBundleRegistry) {
        this.resourceBundleRegistry = resourceBundleRegistry;
    }

    @Override
    public void onEvent(EventIterator events) {
        if (resourceBundleRegistry instanceof MutableResourceBundleRegistry) {
            ((MutableResourceBundleRegistry) resourceBundleRegistry).unregisterAllBundleFamilies();
        }
    }

    public void onEventListenersContainerStarted() {
        // do nothing
    }

    public void onEventListenersContainerRefreshed() {
        // event listener is reconnected: Because we might have missed changes, we need
        // to unregister everything from the resourceBundleRegistry
        if (resourceBundleRegistry instanceof MutableResourceBundleRegistry) {
            ((MutableResourceBundleRegistry) resourceBundleRegistry).unregisterAllBundleFamilies();
        }
    }

    public void onEventListenersContainerStopped() {
        // do nothing
    }
}
