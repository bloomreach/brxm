/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
 *
 */
package org.hippoecm.frontend.service.navappsettings;

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.INavAppSettingsService;
import org.hippoecm.frontend.service.NavConfigResource;
import org.hippoecm.frontend.service.ResourceType;

public class NavAppSettingsService extends Plugin implements INavAppSettingsService {

    public NavAppSettingsService(IPluginContext context, IPluginConfig config) {
        super(context, config);
        final String name = config.getString(SERVICE_ID, INavAppSettingsService.SERVICE_ID);
        context.registerService(this, name);
    }

    @Override
    public List<NavConfigResource> getNavConfigResources() {
        final IPluginConfig navConfigResources = getPluginConfig().getPluginConfig("navConfigResources");
        final List<NavConfigResource> resources = new ArrayList<>();
        for (IPluginConfig eachResource : navConfigResources.getPluginConfigSet()) {
            resources.add(readResource(eachResource));
        }
        return resources;
    }

    private NavConfigResource readResource(final IPluginConfig eachResource) {
        final String resourceUrl = eachResource.getString("resource.url");
        final ResourceType resourceType = ResourceType.valueOf(eachResource.getString("resource.type").toUpperCase());
        return new NavConfigResource() {
            @Override
            public String getUrl() {
                return resourceUrl;
            }

            @Override
            public ResourceType getResourceType() {
                return resourceType;
            }
        };
    }

}
