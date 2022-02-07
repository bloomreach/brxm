/*
 * Copyright 2019-2022 Hippo B.V. (http://www.onehippo.com)
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
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
import org.hippoecm.frontend.service.NgxLoggerLevel;
import org.hippoecm.frontend.service.ResourceType;
import org.hippoecm.frontend.service.UserSettings;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.frontend.usagestatistics.UsageStatisticsSettings;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.repository.api.HippoSession;
import org.onehippo.repository.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;

public class NavAppSettingsService extends Plugin implements INavAppSettingsService {

    static final String IFRAMES_CONNECTION_TIMEOUT = "iframesConnectionTimeout";
    static final String LOG_LEVEL = "logLevel";
    static final String LOGIN_TYPE_QUERY_PARAMETER = "logintype";
    public static final String LOGIN_LOGIN_USER_SESSION_ATTRIBUTE_NAME = NavAppSettingsService.class.getName() + "_" +
            LOGIN_TYPE_QUERY_PARAMETER;
    public static final String PATH_PARAM = "path";
    public static final String UUID_PARAM = "uuid";

    static final String NAVIGATIONITEMS_ENDPOINT = "/ws/navigationitems";
    private static final Logger log = LoggerFactory.getLogger(NavAppSettingsService.class);
    // To make unit testing easier (needs to be serializable )
    private final IModel<PluginUserSession> pluginUserSessionSupplier;
    private final IModel<SessionAttributeStore> sessionAttributeStoreSupplier;

    private final transient NavAppResourceService navAppResourceService;

    NavAppSettingsService(IPluginContext context, IPluginConfig config, IModel<PluginUserSession> pluginUserSessionSupplier, IModel<SessionAttributeStore> sessionAttributeStoreSupplier, NavAppResourceService navAppResourceService) {
        super(context, config);
        this.pluginUserSessionSupplier = pluginUserSessionSupplier;
        this.navAppResourceService = navAppResourceService;
        final String name = config.getString(SERVICE_ID, SERVICE_ID);
        this.sessionAttributeStoreSupplier = sessionAttributeStoreSupplier;
        context.registerService(this, name);
    }

    public NavAppSettingsService(IPluginContext context, IPluginConfig config) {
        this(context, config,
                PluginUserSession::get,
                () -> new UserSessionAttributeStore(PluginUserSession::get),
                new NavAppResourceServiceImpl(HstServices.getComponentManager().getContainerConfiguration())
        );
    }

    @Override
    public NavAppSettings getNavAppSettings(final Request request) {

        final PluginUserSession pluginUserSession = pluginUserSessionSupplier.getObject();
        final UserSettings userSettings = createUserSettings(pluginUserSession);

        final IRequestParameters queryParameters = request.getQueryParameters();
        final StringValue initialPathStringValue = queryParameters.getParameterValue(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER);
        final String initialPath = initialPathStringValue.toString(convertLegacyDocumentParameters(request));

        final StringValue loginTypeStringValue = queryParameters.getParameterValue(LOGIN_TYPE_QUERY_PARAMETER);
        if (loginTypeStringValue.toString(EMPTY).equals("local")) {
            this.sessionAttributeStoreSupplier.getObject().setAttribute(LOGIN_LOGIN_USER_SESSION_ATTRIBUTE_NAME, true);
        }

        final boolean localLogin = (boolean) Optional
                .ofNullable(this.sessionAttributeStoreSupplier.getObject().getAttribute(LOGIN_LOGIN_USER_SESSION_ATTRIBUTE_NAME))
                .orElse(false);

        final String logLevelString = queryParameters.getParameterValue(LOG_LEVEL).toString(EMPTY);

        final AppSettings appSettings = createAppSettings(initialPath, localLogin, logLevelString);
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

    @Override
    public int getIframesConnectionTimeout() {
        return getPluginConfig().getInt(IFRAMES_CONNECTION_TIMEOUT, 60_000);
    }

    private String convertLegacyDocumentParameters(final Request request) {
        final StringValue uuid = request.getQueryParameters().getParameterValue(UUID_PARAM);
        if (!uuid.isEmpty()) {
            return String.format("/content/%s/%s", UUID_PARAM, uuid.toString());
        }
        final StringValue path = request.getQueryParameters().getParameterValue(PATH_PARAM);
        if (!path.isEmpty()) {
            return Stream.concat(
                    Stream.of("content", PATH_PARAM),
                    Stream.of(path.toString().split("/"))
                            .filter(StringUtils::isNotBlank)
                            .map(String::trim))
                    .collect(Collectors.joining("/", "/", ""));
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

    private AppSettings createAppSettings(String initialPath, boolean localLogin, String logLevelQueryParamString) {

        final URI cmsNavAppResourceUrl = URI.create("angular/navapp");
        // It is assumed that the web.xml contains a servlet mapping for /navapp and that
        // the ResourceServlet is being used to serve the resources inside of that directory.
        // When running mvn package the files needed for the navapp (and navigation-communication)
        // are copied into  the target directory. See copy-files.js
        final URI navAppResourceLocation = navAppResourceService
                .getNavAppResourceUrl()
                .orElse(cmsNavAppResourceUrl);
        final boolean cmsOriginOfNavAppResources = cmsNavAppResourceUrl.equals(navAppResourceLocation);
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

        final int iframesConnectionTimeout = getIframesConnectionTimeout();
        final NgxLoggerLevel ngxLoggerLevel = readLogLevel(logLevelQueryParamString);
        final boolean usageStaticsticsEnabled = UsageStatisticsSettings.get().isEnabled();

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

            @Override
            public NgxLoggerLevel getLogLevel() {
                return ngxLoggerLevel;
            }

            @Override
            public boolean isUsageStatisticsEnabled() {
                return usageStaticsticsEnabled;
            }
        };
    }

    private NgxLoggerLevel readLogLevel(String logLevelQueryParamString) {

        final NgxLoggerLevel defaultLevel = NgxLoggerLevel.OFF;

        final String logLevelString;
        if (logLevelQueryParamString.isEmpty()) {
            logLevelString = getPluginConfig().getString(LOG_LEVEL, defaultLevel.name());
        } else {
            logLevelString = logLevelQueryParamString;
        }

        try {
            return NgxLoggerLevel.valueOf(logLevelString.toUpperCase());
        } catch (IllegalArgumentException e) {
            if (logLevelQueryParamString.isEmpty()) {
                log.error("'{}' is an invalid value for property '{}'. Valid values are: {}.",
                        logLevelString,
                        LOG_LEVEL,
                        Stream.of(NgxLoggerLevel.values()).collect(toList())
                );
            }
            return defaultLevel;
        }
    }

}
