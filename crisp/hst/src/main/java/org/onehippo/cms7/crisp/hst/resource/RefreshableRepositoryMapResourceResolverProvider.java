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

import org.hippoecm.hst.site.HstServices;
import org.onehippo.cms7.crisp.api.CrispConstants;
import org.onehippo.cms7.crisp.api.resource.ResourceResolver;
import org.onehippo.cms7.crisp.core.resource.RepositoryMapResourceResolverProvider;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.cms7.services.eventbus.Subscribe;

/**
 * Extending {@link RepositoryMapResourceResolverProvider} to be able to refresh the internal resource resolvers
 * on any changes from the CRISP ResourceResolver container configuration node which is determined by {@link #getResourceResolverContainerConfigPath()}.
 * <p>
 * Also, this class overrides {@link #getResourceResolverContainerConfigPath()} in order to allow an HST site webapp
 * to override the default CRISP ResourceResolver container configuration node path (e.g. "/hippo:configuration/hippo:modules/crispregistry/hippo:moduleconfig/crisp:resourceresolvercontainer")
 * by configuring a property in hst-config.properties.
 */
public class RefreshableRepositoryMapResourceResolverProvider extends RepositoryMapResourceResolverProvider {

    /**
     * {@link HippoEventBus} event listener instance to subscribe configuration changes in the repository.
     */
    private ConfigurationChangeEventListener configurationChangeEventListener;

    /**
     * Default constructor.
     */
    public RefreshableRepositoryMapResourceResolverProvider() {
        super();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        configurationChangeEventListener = new ConfigurationChangeEventListener();
        HippoServiceRegistry.registerService(configurationChangeEventListener, HippoEventBus.class);
    }

    @Override
    public void destroy() {
        HippoServiceRegistry.unregisterService(configurationChangeEventListener, HippoEventBus.class);
        super.destroy();
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

    /**
     * {@link HippoEventBus} event listener to subscribe configuration changes in the repository and initialize
     * the {@link ResourceResolver}s for each <strong>resource space</strong>s.
     */
    public class ConfigurationChangeEventListener {
        @Subscribe
        public void handleEvent(HippoEvent event) {
            if (CrispConstants.EVENT_APPLICATION_NAME.equals(event.application())
                    && CrispConstants.EVENT_CATEGORY_CONFIGURATION.equals(event.category())
                    && CrispConstants.EVENT_ACTION_UPDATE_CONFIGURATION.equals(event.action())) {
                refreshResourceResolvers();
            }
        }
    }
}
