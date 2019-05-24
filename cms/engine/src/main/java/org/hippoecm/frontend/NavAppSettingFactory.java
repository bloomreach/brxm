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

package org.hippoecm.frontend;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.request.Request;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.frontend.util.RequestUtils;

final class NavAppSettingFactory {

    static final String NAVAPP_LOCATION = "navapp.location";

    private NavAppSettingFactory() {
    }

    static NavAppSettings newInstance(Request request, PluginUserSession userSession) {
        final NavAppSettings navAppSettings = new NavAppSettings();
        navAppSettings.setAppSettings(getAppSettings(request));
        navAppSettings.setUserSettings(getUserSettings(userSession));
        return navAppSettings;
    }

    private static NavAppSettings.UserSettings getUserSettings(PluginUserSession userSession) {
        final NavAppSettings.UserSettings userSettings = new NavAppSettings.UserSettings();
        userSettings.setUserName(userSession.getUserName());
        userSettings.setLanguage(userSession.getLocale().getLanguage());
        userSettings.setTimeZone(userSession.getTimeZone());
        return userSettings;
    }

    private static NavAppSettings.AppSettings getAppSettings(final Request request) {
        final NavAppSettings.AppSettings appSettings = new NavAppSettings.AppSettings();
        appSettings.setContextPath(RequestUtils.getContextPath());
        final String cmsLocation = RequestUtils.getCmsLocation(request);
        appSettings.setBrXmLocation(URI.create(cmsLocation));
        appSettings.setNavAppLocation(URI.create(System.getProperty(NAVAPP_LOCATION, cmsLocation)));
        appSettings.setNavConfigResources(getNavConfigResources(cmsLocation));
        return appSettings;
    }

    private static List<NavAppSettings.NavConfigResource> getNavConfigResources(final String cmsLocation) {

        NavAppSettings.NavConfigResource navConfigResource = new NavAppSettings.NavConfigResource();
        navConfigResource.setResourceType(NavAppSettings.ResourceType.REST);
        navConfigResource.setUrl(cmsLocation + "/ws/navigationitems");

        return Collections.singletonList(navConfigResource);
    }
}
