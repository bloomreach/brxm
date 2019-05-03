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

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.frontend.session.PluginUserSession;

class NavAppSettingsFactory {

    NavAppSettings getNavAppSettings(PluginUserSession userSession) {
        final NavAppSettings.UserSettings userSettings = getUserSetttings(userSession);
        final NavAppSettings.AppSettings appSettings = getAppSettings();
        final NavAppSettings navAppSettings = new NavAppSettings();
        navAppSettings.setAppSettings(appSettings);
        navAppSettings.setUserSettings(userSettings);
        return navAppSettings;
    }

    private NavAppSettings.AppSettings getAppSettings() {
        final List<NavAppSettings.NavConfigResource> navConfigResources = new ArrayList<>();

        navConfigResources.add(getBrXmResource());
        // TODO (meggermont): read other ones from repo

        final NavAppSettings.AppSettings appSettings = new NavAppSettings.AppSettings();
        appSettings.setNavConfigResources(navConfigResources);

        return appSettings;
    }

    private NavAppSettings.NavConfigResource getBrXmResource() {
        final NavAppSettings.NavConfigResource brXmResource = new NavAppSettings.NavConfigResource();
        brXmResource.setResourceType(NavAppSettings.ResourceType.REST);
        // TODO (meggermont): set the URL
        return brXmResource;
    }

    private NavAppSettings.UserSettings getUserSetttings(final PluginUserSession userSession) {
        final NavAppSettings.UserSettings userSettings = new NavAppSettings.UserSettings();
        userSettings.setLanguage(userSession.getLocale().getLanguage());
        userSettings.setTimeZone(userSession.getTimeZone());
        userSettings.setUserName(userSession.getUserName());
        return userSettings;
    }
}
