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
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Supplier;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
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
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NavAppSettingsService extends Plugin implements INavAppSettingsService {

    private static final Logger log = LoggerFactory.getLogger(NavAppSettingsService.class);

    static final String NAVAPP_LOCATION_SYSTEM_PROPERTY = "navapp.location";

    static final String SITE_CONFIG_RESOURCE = "siteConfigResource";

    static final String NAV_CONFIG_RESOURCES = "navConfigResources";
    static final String RESOURCE_URL = "resource.url";
    static final String RESOURCE_TYPE = "resource.type";

    static final String NAVIGATIONITEMS_ENDPOINT = "/ws/navigationitems";

    // To make unit testing easier (needs to be transient to prevent Wicket from serializing it.
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

        final String userName = userSession.getUserName();
        final String email = getEmail(userSession);
        final String language = userSession.getLocale().getLanguage();
        final TimeZone timeZone = userSession.getTimeZone();

        return new UserSettings() {

            @Override
            public String getUserName() {
                return userName;
            }

            @Override
            public String getEmail() {
                return email;
            }

            @Override
            public String getLanguage() {
                return language;
            }

            @Override
            public TimeZone getTimeZone() {
                return timeZone;
            }
        };
    }

    private String getEmail(final PluginUserSession userSession) {
        final Session session = userSession.getJcrSession();
        if (session instanceof HippoSession) {
            final HippoSession hippoSession = (HippoSession) session;
            try {
                return hippoSession.getUser().getEmail();
            } catch (RepositoryException e) {
                log.error("Failed to get user email", e);
                return null;
            }
        }
        return null;
    }

    private AppSettings createAppSettings(Request request) {

        final String parentOrigin = RequestUtils.getFarthestUrlPrefix(request);
        final String contextPath = RequestUtils.getContextPath();
        final String cmsLocation = String.format("%s%s", parentOrigin, contextPath);

        final URI brXmLocation = URI.create(cmsLocation);
        final URI navAppLocation = URI.create(System.getProperty(NAVAPP_LOCATION_SYSTEM_PROPERTY, cmsLocation));

        final NavConfigResource siteConfigResource = readSiteConfigResource();
        final List<NavConfigResource> navConfigResources = readNavConfigResources(cmsLocation);

        return new AppSettings() {

            @Override
            public URI getBrXmLocation() {
                return brXmLocation;
            }

            @Override
            public NavConfigResource getSitesResource() {
                return siteConfigResource;
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

    private NavConfigResource readSiteConfigResource() {
        final IPluginConfig pluginConfig = getPluginConfig();
        final IPluginConfig siteConfigResource = pluginConfig.getPluginConfig(SITE_CONFIG_RESOURCE);
        if (siteConfigResource != null) {
            return readResource(siteConfigResource);
        }
        return null;
    }

    private List<NavConfigResource> readNavConfigResources(String cmsLocation) {
        final List<NavConfigResource> resources = new ArrayList<>();
        resources.add(createResource(URI.create(cmsLocation + NAVIGATIONITEMS_ENDPOINT), ResourceType.REST));

        final IPluginConfig pluginConfig = getPluginConfig();
        final IPluginConfig navConfigResources = pluginConfig.getPluginConfig(NAV_CONFIG_RESOURCES);
        final Set<IPluginConfig> configSet = navConfigResources.getPluginConfigSet();
        for (IPluginConfig eachResource : configSet) {
            resources.add(readResource(eachResource));
        }
        return resources;
    }

    private NavConfigResource readResource(IPluginConfig resourceConfig) {
        final String resourceUrlString = resourceConfig.getString(RESOURCE_URL).trim();
        if (StringUtils.isBlank(resourceUrlString)) {
            throw new IllegalArgumentException(RESOURCE_URL + " must not be empty or null");
        }
        final URI url = URI.create(resourceUrlString);
        final ResourceType type = ResourceType.valueOf(resourceConfig.getString(RESOURCE_TYPE).toUpperCase());
        return createResource(url, type);
    }


    private NavConfigResource createResource(URI url, ResourceType resourceType) {
        return new NavConfigResource() {
            @Override
            public URI getUrl() {
                return url;
            }

            @Override
            public ResourceType getResourceType() {
                return resourceType;
            }
        };
    }

}
