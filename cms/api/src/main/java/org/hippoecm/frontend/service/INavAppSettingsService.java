/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.request.Request;
import org.apache.wicket.util.io.IClusterable;

public interface INavAppSettingsService extends IClusterable {

    String SERVICE_ID = "service.navappsettings";

    /**
     * Returns the navigation application settings for a request of a logged in user. It is assumed that a valid
     * PluginUserSession for this request exists, because the returned NavAppSettings must contain user specific
     * attributes like user name, language and time zone.
     *
     * @param request a request from a logged in user
     * @return navigation application settings for the user request
     */
    NavAppSettings getNavAppSettings(Request request);

    /**
     * Returns the maximum number of milliseconds to wait for an iframe to connect
     *
     * @return max wait time in milliseconds
     */
    int getIframesConnectionTimeout();

}
