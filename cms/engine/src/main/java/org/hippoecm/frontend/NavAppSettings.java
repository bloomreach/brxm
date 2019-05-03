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

import java.util.List;
import java.util.TimeZone;

public class NavAppSettings {

    private UserSettings userSettings;
    private AppSettings appSettings;

    public UserSettings getUserSettings() {
        return userSettings;
    }

    public void setUserSettings(final UserSettings userSettings) {
        this.userSettings = userSettings;
    }

    public AppSettings getAppSettings() {
        return appSettings;
    }

    public void setAppSettings(final AppSettings appSettings) {
        this.appSettings = appSettings;
    }

    public static class UserSettings {

        private String userName;
        private String language;
        private TimeZone timeZone;

        public String getUserName() {
            return userName;
        }

        public void setUserName(final String userName) {
            this.userName = userName;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(final String language) {
            this.language = language;
        }

        public TimeZone getTimeZone() {
            return timeZone;
        }

        public void setTimeZone(final TimeZone timeZone) {
            this.timeZone = timeZone;
        }
    }


    public static class AppSettings {

        private List<NavConfigResource> navConfigResources;

        public List<NavConfigResource> getNavConfigResources() {
            return navConfigResources;
        }

        public void setNavConfigResources(final List<NavConfigResource> navConfigResources) {
            this.navConfigResources = navConfigResources;
        }
    }

    public enum ResourceType {
        REST, IFRAME
    }

    public static class NavConfigResource {
        private String url;
        private ResourceType resourceType;

        public String getUrl() {
            return url;
        }

        public void setUrl(final String url) {
            this.url = url;
        }

        public ResourceType getResourceType() {
            return resourceType;
        }

        public void setResourceType(final ResourceType resourceType) {
            this.resourceType = resourceType;
        }
    }

}


