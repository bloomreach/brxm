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
import java.util.function.Supplier;

import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.request.Request;
import org.apache.wicket.util.string.StringValue;
import org.hippoecm.frontend.filter.NavAppRedirectFilter;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.AppSettings;
import org.hippoecm.frontend.service.INavAppSettingsService;
import org.hippoecm.frontend.service.NavAppResource;
import org.hippoecm.frontend.service.NavAppSettings;
import org.hippoecm.frontend.service.ResourceType;
import org.hippoecm.frontend.service.UserSettings;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.repository.api.HippoSession;
import org.onehippo.repository.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NavAppSettingsService extends Plugin implements INavAppSettingsService {

    private static final String JAR_PATH_PREFIX = "navapp";
    static final String NAVAPP_LOCATION_SYSTEM_PROPERTY = "navapp.location";

    static final String NAV_CONFIG_RESOURCES = "navConfigResources";
    static final String LOGIN_RESOURCES = "loginResources";
    static final String LOGOUT_RESOURCES = "logoutResources";
    static final String RESOURCE_URL = "resource.url";
    static final String RESOURCE_TYPE = "resource.type";
    static final String IFRAMES_CONNECTION_TIMEOUT = "iframesConnectionTimeout";

    static final String NAVIGATIONITEMS_ENDPOINT = "/ws/navigationitems";
    private static final Logger log = LoggerFactory.getLogger(NavAppSettingsService.class);
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
        final StringValue initialPath = request.getQueryParameters().getParameterValue(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER);
        final AppSettings appSettings = createAppSettings(initialPath.toString("/"));

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
        final HippoSession hippoSession = (HippoSession) userSession.getJcrSession();
        final String userName = userSession.getUserName();
        final UserSettingsBuilder userSettingsBuilder =
                new UserSettingsBuilder()
                        .timeZone(userSession.getTimeZone())
                        .language(userSession.getLocale().getLanguage())
                        .userName(userName);
        try {
            final User user = hippoSession.getUser();
            userSettingsBuilder
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName());
        } catch (RepositoryException e) {
            log.error("Could not read properties of user with username {}", userName, e);
        }
        return userSettingsBuilder.build();
    }

    private AppSettings createAppSettings(String initialPath) {

        final String navAppLocation = System.getProperty(NAVAPP_LOCATION_SYSTEM_PROPERTY, null);
        final boolean cmsOriginOfNavAppResources = navAppLocation == null;
        final URI navAppResourceLocation =
                URI.create(cmsOriginOfNavAppResources ? JAR_PATH_PREFIX : navAppLocation);
        // It is assumed that the web.xml contains a servlet mapping for /navapp and that
        // the ResourceServlet is being used to serve the resources inside of that directory.
        // When running mvn package the files needed for the navapp (and navigation-communication)
        // are copied into  the target directory. See copy-files.js
        final List<NavAppResource> navConfigResources = readNavConfigResources();
        final List<NavAppResource> loginDomains = readResources(LOGIN_RESOURCES);
        final List<NavAppResource> logoutDomains = readResources(LOGOUT_RESOURCES);
        final int iframesConnectionTimeout = readIframesConnectionTimeout();

        return new AppSettings() {

            @Override
            public String getInitialPath() {
                return initialPath;
            }

            @Override
            public boolean isCmsServingNavAppResources() {
                return cmsOriginOfNavAppResources;
            }

            @Override
            public URI getNavAppResourceLocation() {
                return navAppResourceLocation;
            }

            @Override
            public List<NavAppResource> getNavConfigResources() {
                return navConfigResources;
            }

            @Override
            public List<NavAppResource> getLoginResources() {
                return loginDomains;
            }

            @Override
            public List<NavAppResource> getLogoutResources() {
                return logoutDomains;
            }

            @Override
            public int getIframesConnectionTimeout() {
                return iframesConnectionTimeout;
            }
        };
    }

    private int readIframesConnectionTimeout() {
        return getPluginConfig().getInt(IFRAMES_CONNECTION_TIMEOUT, 30_000);
    }

    private List<NavAppResource> readNavConfigResources() {
        final List<NavAppResource> resources = new ArrayList<>();
        resources.add(createResource(URI.create(NAVIGATIONITEMS_ENDPOINT), ResourceType.INTERNAL_REST));
        resources.addAll(readResources(NAV_CONFIG_RESOURCES));
        return resources;
    }

    private List<NavAppResource> readResources(String resourceKey) {
        final List<NavAppResource> resources = new ArrayList<>();
        final IPluginConfig pluginConfig = getPluginConfig();
        if (pluginConfig.containsKey(resourceKey)) {
            final IPluginConfig navConfigResources = pluginConfig.getPluginConfig(resourceKey);
            final Set<IPluginConfig> configSet = navConfigResources.getPluginConfigSet();
            for (IPluginConfig eachResource : configSet) {
                resources.add(readResource(eachResource));
            }
        }
        return resources;
    }

    private NavAppResource readResource(IPluginConfig resourceConfig) {
        final String resourceUrlString = resourceConfig.getString(RESOURCE_URL).trim();
        if (StringUtils.isBlank(resourceUrlString)) {
            throw new IllegalArgumentException(RESOURCE_URL + " must not be empty or null");
        }
        final URI url = URI.create(resourceUrlString);
        final ResourceType type = ResourceType.valueOf(resourceConfig.getString(RESOURCE_TYPE).toUpperCase());
        return createResource(url, type);
    }


    private NavAppResource createResource(URI url, ResourceType resourceType) {
        return new NavAppResource() {
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
