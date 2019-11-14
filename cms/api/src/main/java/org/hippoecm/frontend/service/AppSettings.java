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

package org.hippoecm.frontend.service;

import java.net.URI;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * JavaBean containing the settings of the Navigation Application.
 * These are the settings that the app needs to bootstrap itself.
 */
public interface AppSettings {

    /**
     * Returns the path that the navapp should navigate to after the navapp has been fully initialized.
     *
     * @return initial path
     */
    String getInitialPath();

    /**
     * Returns {@code true} if the CMS is serving the Navigation Application resources and {@code false} otherwise
     *
     * @return if CMS is serving the navapp resources
     */
    @JsonIgnore
    boolean isCmsServingNavAppResources();

    /**
     * Returns the location of the Navigation Application resources (like javascript, css, images).
     *
     * @return navapp resource location
     */
    URI getNavAppResourceLocation();

    /**
     * Returns the maximum number of milliseconds to wait for Iframes to connect before giving up.
     *
     * @return Iframe connection timeout in milliseconds
     */
    int getIframesConnectionTimeout();

    /**
     * Returns the log level to use in the browser
     *
     * @return logLevel
     */
    NgxLoggerLevel getLogLevel();

    /**
     * Returns list of resources that must be called by the navapp to retrieve menu related navigation items.
     *
     * @return list of nav config resources
     */
    List<NavAppResource> getNavConfigResources();

    /**
     * Returns list of resources that must be called by the navapp to make sure all apps are authenticated
     * before accessing them.
     *
     * @return list of login resources
     */
    List<NavAppResource> getLoginResources();

    /**
     * Returns list of resources that must be called by the navapp to make sure all apps clean up their state and
     * authentication ids (e.g. cookies) before redirecting to the login page again.
     *
     * @return list of logout resources
     */
    List<NavAppResource> getLogoutResources();

}
