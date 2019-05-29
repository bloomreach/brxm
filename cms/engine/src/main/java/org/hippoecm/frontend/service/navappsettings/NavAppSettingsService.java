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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.function.Supplier;

import org.apache.wicket.request.Request;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.AppSettings;
import org.hippoecm.frontend.service.INavAppSettingsService;
import org.hippoecm.frontend.service.NavAppSettings;
import org.hippoecm.frontend.service.NavConfigResource;
import org.hippoecm.frontend.service.ResourceType;
import org.hippoecm.frontend.service.UserSettings;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.frontend.util.RequestUtils;

public class NavAppSettingsService extends Plugin implements INavAppSettingsService {

    static final String NAVAPP_LOCATION_SYSTEM_PROPERTY = "navapp.location";

    static final String NAV_CONFIG_RESOURCES = "navConfigResources";
    static final String RESOURCE_URL = "resource.url";
    static final String RESOURCE_TYPE = "resource.type";

    static final String NAVIGATIONITEMS_ENDPOINT = "/ws/navigationitems";

    // To make unit testing easier
    private final transient Supplier<PluginUserSession> pluginUserSessionSupplier;

    NavAppSettingsService(IPluginContext context, IPluginConfig config, Supplier<PluginUserSession> pluginUserSessionSupplier) {
        super(context, config);
        this.pluginUserSessionSupplier = pluginUserSessionSupplier;
        final String name = config.getString(SERVICE_ID, SERVICE_ID);
        context.registerService(this, name);
    }

    public NavAppSettingsService(IPluginContext context, IPluginConfig config) {
        this(context, config, PluginUserSession::get);
    }

    @Override
    public NavAppSettings getNavAppSettings(final Request request) {

        final UserSettings userSettings = createUserSettings(pluginUserSessionSupplier.get());
        final AppSettings appSettings = createAppSettings(request);

        return new NavAppSettings() {
            @Override
            public UserSettings getUserSettings() {
                return userSettings;
            }

            @Override
            public AppSettings getAppSettings() {
                return appSettings;
            }
        };
    }

    private UserSettings createUserSettings(final PluginUserSession userSession) {
        return new UserSettings() {

            @Override
            public String getUserName() {
                return userSession.getUserName();
            }

            @Override
            public String getEmail() {
                // TODO (meggermont): read optional email address from repository
                return null;
            }

            @Override
            public String getLanguage() {
                return userSession.getLocale().getLanguage();
            }

            @Override
            public TimeZone getTimeZone() {
                return userSession.getTimeZone();
            }
        };
    }

    private AppSettings createAppSettings(Request request) {

        final String parentOrigin = RequestUtils.getFarthestUrlPrefix(request);
        final String contextPath = RequestUtils.getContextPath();
        final String cmsLocation = String.format("%s%s", parentOrigin, contextPath);

        final URI brXmLocation = URI.create(cmsLocation);
        final URI navAppLocation = URI.create(System.getProperty(NAVAPP_LOCATION_SYSTEM_PROPERTY, cmsLocation));
        final List<NavConfigResource> navConfigResources = readNavConfigResources(cmsLocation, parentOrigin);

        return new AppSettings() {

            @Override
            public URI getBrXmLocation() {
                return brXmLocation;
            }

            @Override
            public URI getNavAppLocation() {
                return navAppLocation;
            }

            @Override
            public String getContextPath() {
                return contextPath;
            }

            @Override
            public List<NavConfigResource> getNavConfigResources() {
                return navConfigResources;
            }
        };
    }

    private List<NavConfigResource> readNavConfigResources(String cmsLocation, String parentOrigin) {
        final IPluginConfig navConfigResources = getPluginConfig().getPluginConfig(NAV_CONFIG_RESOURCES);
        final List<NavConfigResource> resources = new ArrayList<>();
        resources.add(createResource(cmsLocation + NAVIGATIONITEMS_ENDPOINT, ResourceType.REST));
        for (IPluginConfig eachResource : navConfigResources.getPluginConfigSet()) {
            resources.add(readResource(eachResource, parentOrigin));
        }
        return resources;
    }

    private NavConfigResource readResource(IPluginConfig resourceConfig, String parentOrigin) {
        final String resourceUrl = resourceConfig.getString(RESOURCE_URL);
        final ResourceType type = ResourceType.valueOf(resourceConfig.getString(RESOURCE_TYPE).toUpperCase());
        final String url = getUrl(type, resourceUrl, parentOrigin);
        return createResource(url, type);
    }

    private String getUrl(ResourceType resourceType, String resourceUrl, String urlPrefix) {
        switch (resourceType) {
            case IFRAME:
                // FIXME (meggermont): remove parent query parameter
                // To be able to setup a postMessage from an iframe to a parent
                // we need to know the parent url. This information should not
                // come from a query parameter, because that would allow anyone
                // to embed the child iframe in their page. Instead the parent
                // origin should be provided on the domain of the iframe itself.
                // This can e.g. be done via a cookie set by the server that
                // serves the source of the iframe.
                // Since we don't have that yet we use the unsafe query parameter
                // for now. This method can be removed once we have a safe way of
                // getting the parent origin.
                return String.format("%s/?parent=%s", resourceUrl, urlPrefix);
            default:
                return resourceUrl;
        }
    }

    private NavConfigResource createResource(final String url, ResourceType resourceType) {
        return new NavConfigResource() {
            @Override
            public String getUrl() {
                return url;
            }

            @Override
            public ResourceType getResourceType() {
                return resourceType;
            }
        };
    }

}
