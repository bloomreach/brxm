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
import java.util.Optional;
import java.util.function.Supplier;

import javax.jcr.RepositoryException;

import org.apache.wicket.request.IRequestParameters;
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
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.repository.api.HippoSession;
import org.onehippo.repository.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NavAppSettingsService extends Plugin implements INavAppSettingsService {

    private static final String JAR_PATH_PREFIX = "navapp";
    static final String NAVAPP_LOCATION_SYSTEM_PROPERTY = "navapp.location";

    static final String IFRAMES_CONNECTION_TIMEOUT = "iframesConnectionTimeout";
    static final String LOGIN_TYPE_QUERY_PARAMETER = "logintype";
    public static final String LOGIN_LOGIN_USER_SESSION_ATTRIBUTE_NAME = NavAppSettingsService.class.getName() + "_" +
            LOGIN_TYPE_QUERY_PARAMETER;
    public static final String PATH_PARAM = "path";
    public static final String UUID_PARAM = "uuid";

    static final String NAVIGATIONITEMS_ENDPOINT = "/ws/navigationitems";
    private static final Logger log = LoggerFactory.getLogger(NavAppSettingsService.class);
    // To make unit testing easier (needs to be transient to prevent Wicket from serializing it.
    private final transient Supplier<PluginUserSession> pluginUserSessionSupplier;
    private final transient SessionAttributeStore sessionAttributeStore;

    private final transient NavAppResourceService navAppResourceService;

    NavAppSettingsService(IPluginContext context, IPluginConfig config, Supplier<PluginUserSession> pluginUserSessionSupplier, SessionAttributeStore sessionAttributeStore, NavAppResourceService navAppResourceService) {
        super(context, config);
        this.pluginUserSessionSupplier = pluginUserSessionSupplier;
        this.navAppResourceService = navAppResourceService;
        final String name = config.getString(SERVICE_ID, SERVICE_ID);
        this.sessionAttributeStore = sessionAttributeStore;
        context.registerService(this, name);
    }

    public NavAppSettingsService(IPluginContext context, IPluginConfig config) {
        this(context, config,
                PluginUserSession::get,
                new UserSessionAttributeStore(PluginUserSession.get()),
                new NavAppResourceServiceImpl(HstServices.getComponentManager().getContainerConfiguration().toProperties())
        );
    }

    @Override
    public NavAppSettings getNavAppSettings(final Request request) {

        final PluginUserSession pluginUserSession = pluginUserSessionSupplier.get();
        final UserSettings userSettings = createUserSettings(pluginUserSession);

        final IRequestParameters queryParameters = request.getQueryParameters();
        final StringValue initialPathStringValue = queryParameters.getParameterValue(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER);
        final String initialPath = initialPathStringValue.toString(convertLegacyDocumentParameters(request));

        final StringValue loginTypeStringValue = queryParameters.getParameterValue(LOGIN_TYPE_QUERY_PARAMETER);
        if (loginTypeStringValue.toString("").equals("local")){
            this.sessionAttributeStore.setAttribute(LOGIN_LOGIN_USER_SESSION_ATTRIBUTE_NAME, true);
        }
        boolean localLogin = (boolean) Optional
                .ofNullable( this.sessionAttributeStore.getAttribute(LOGIN_LOGIN_USER_SESSION_ATTRIBUTE_NAME))
                .orElse(false);

        final AppSettings appSettings = createAppSettings(initialPath, localLogin);
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

    private String convertLegacyDocumentParameters(final Request request) {
        final StringValue uuid = request.getQueryParameters().getParameterValue(UUID_PARAM);
        if (!uuid.isEmpty()) {
            return "/browser/uuid/" + uuid.toString();
        }
        final StringValue path = request.getQueryParameters().getParameterValue(PATH_PARAM);
        if (!path.isEmpty()) {
            return "/browser/path/" + path.toString();
        }
        return "/";
    }

    private UserSettings createUserSettings(final PluginUserSession userSession) {
        final HippoSession hippoSession = userSession.getJcrSession();
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

    private AppSettings createAppSettings(String initialPath, boolean localLogin) {

        final String navAppLocation = System.getProperty(NAVAPP_LOCATION_SYSTEM_PROPERTY, null);
        final boolean cmsOriginOfNavAppResources = navAppLocation == null;
        final URI navAppResourceLocation =
                URI.create(cmsOriginOfNavAppResources ? JAR_PATH_PREFIX : navAppLocation);
        // It is assumed that the web.xml contains a servlet mapping for /navapp and that
        // the ResourceServlet is being used to serve the resources inside of that directory.
        // When running mvn package the files needed for the navapp (and navigation-communication)
        // are copied into  the target directory. See copy-files.js

        final List<NavAppResource> navConfigResources = new ArrayList<>();
        final List<NavAppResource> loginResources = new ArrayList<>();
        final List<NavAppResource> logoutResources = new ArrayList<>();

        final NavAppResource cmsNavigationItemsResource = new NavAppResourceBuilder()
                .resourceType(ResourceType.INTERNAL_REST)
                .resourceUrl(URI.create(NAVIGATIONITEMS_ENDPOINT))
                .build();
        navConfigResources.add(cmsNavigationItemsResource);

        if (!localLogin) {
            navConfigResources.addAll(navAppResourceService.getNavigationItemsResources());
            loginResources.addAll(navAppResourceService.getLoginResources());
            logoutResources.addAll(navAppResourceService.getLogoutResources());
        }

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
                return loginResources;
            }

            @Override
            public List<NavAppResource> getLogoutResources() {
                return logoutResources;
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

}
