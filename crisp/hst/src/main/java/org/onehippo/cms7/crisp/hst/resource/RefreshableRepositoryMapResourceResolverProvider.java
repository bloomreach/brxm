/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.hst.resource;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.hippoecm.hst.core.jcr.EventListenerItemImpl;
import org.hippoecm.hst.core.jcr.EventListenersContainer;
import org.hippoecm.hst.site.HstServices;
import org.onehippo.cms7.crisp.api.CrispConstants;
import org.onehippo.cms7.crisp.core.resource.RepositoryMapResourceResolverProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extending {@link RepositoryMapResourceResolverProvider} to be able to refresh the internal resource resolvers
 * on any changes from the CRISP ResourceResolver container configuration node which is determined by {@link #getResourceResolverContainerConfigPath()}.
 * <p>
 * Also, this class overrides {@link #getResourceResolverContainerConfigPath()} in order to allow an HST site webapp
 * to override the default CRISP ResourceResolver container configuration node path (e.g. "/hippo:configuration/hippo:modules/crispregistry/hippo:moduleconfig/crisp:resourceresolvercontainer")
 * by configuring a property named {@link CrispConstants#CRISP_MODULE_CONFIG_PATH_PROP_NAME} in hst-config.properties.
 */
public class RefreshableRepositoryMapResourceResolverProvider extends RepositoryMapResourceResolverProvider {

    private static Logger log = LoggerFactory.getLogger(RefreshableRepositoryMapResourceResolverProvider.class);

    private EventListenersContainer moduleConfigChangeListenersContainer;

    /**
     * Default constructor.
     */
    public RefreshableRepositoryMapResourceResolverProvider() {
        super();
    }

    public void setModuleConfigChangeListenersContainer(EventListenersContainer moduleConfigChangeListenersContainer) {
        this.moduleConfigChangeListenersContainer = moduleConfigChangeListenersContainer;
    }

    @Override
    protected String getModuleConfigPath() {
        if (!HstServices.isAvailable()) {
            return null;
        }

        final String defaultModulePath = super.getModuleConfigPath();
        return HstServices.getComponentManager().getContainerConfiguration()
                .getString(CrispConstants.CRISP_MODULE_CONFIG_PATH_PROP_NAME, defaultModulePath);
    }

    @Override
    protected void onResourceResolversRefreshed() {
        if (moduleConfigChangeListenersContainer != null) {
            disposeModuleConfigChangeListenersContainer();
            initializeModuleConfigChangeListenersContainer();
        }
    }

    @Override
    public void destroy() {
        super.destroy();

        if (moduleConfigChangeListenersContainer != null) {
            disposeModuleConfigChangeListenersContainer();
        }
    }

    /**
     * Called by the module configuration listener on event. As an optimization, may be overridden
     * to return false for events that are actually not a configuration change, so that they don't trigger
     * a reconfiguration.
     * <p>
     * This is the same pattern as <code>org.onehippo.repository.modules.AbstractReconfigurableDaemonModule</code>
     * @param event event returned by the EventIterator
     * @return true if this event requires reloading of the configuration
     * @throws RepositoryException if repository exception occurs
     */
    protected boolean isRefreshableEvent(Event event) throws RepositoryException {
        return true;
    }

    private void initializeModuleConfigChangeListenersContainer() {
        final EventListenerItemImpl listenerItem = new EventListenerItemImpl();
        listenerItem.setAbsolutePath(getModuleConfigPath());
        listenerItem.setDeep(true);
        listenerItem.setEventTypes(Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_ADDED
                | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED);
        listenerItem.setNoLocal(false);
        listenerItem.setEventListener(new ModuleConfigurationChangeListener());

        moduleConfigChangeListenersContainer.addEventListenerItem(listenerItem);
        moduleConfigChangeListenersContainer.start();
    }

    private void disposeModuleConfigChangeListenersContainer() {
        moduleConfigChangeListenersContainer.stop();

        moduleConfigChangeListenersContainer.getEventListenerItems().forEach(listenerItem -> {
            moduleConfigChangeListenersContainer.removeEventListenerItem(listenerItem);
        });
    }

    /**
     * Applying the same pattern as <code>org.onehippo.repository.modules.AbstractReconfigurableDaemonModule</code>.
     */
    private class ModuleConfigurationChangeListener implements EventListener {

        @Override
        public void onEvent(EventIterator events) {
            boolean updated = false;

            while (events.hasNext()) {
                final Event event = events.nextEvent();

                try {
                    if (isRefreshableEvent(event)) {
                        updated = true;
                        break;
                    }
                } catch (RepositoryException e) {
                    log.error("Failed to determine if event is a refreshable event", e);
                }
            }

            if (updated) {
                refreshResourceResolvers();
            }
        }
    }
}
