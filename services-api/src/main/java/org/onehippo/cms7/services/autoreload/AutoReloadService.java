/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.services.autoreload;

/**
 * Automatically reloads the current page in connected browsers. If auto-reload is disabled, nothing happens when
 * {@link #broadcastPageReload()} is called.
 */
@SuppressWarnings("UnusedDeclaration")
public interface AutoReloadService {

    /**
     * @return true if auto-reload is enabled, false otherwise.
     */
    boolean isEnabled();

    /**
     * Enables or disabled auto-reload.
     * @param isEnabled true when auto-reload should be enabled, false when it should be disabled.
     */
    void setEnabled(boolean isEnabled);

    /**
     * @param contextPath the current context path of the page in which the JavaScript will be included. The context
     *                    path can be an empty string when the page is served at URL path '/' (e.g. when the web
     *                    application is deployed as ROOT.war). When the context path is not empty, it must start with
     *                    a slash, followed by the context path string. The context path must not end with a slash.
     *                    For example, valid context paths are "/site", "/intranet" and "".
     * @return the JavaScript to include in a browser that handles the auto-reloading.
     * @throws IllegalArgumentException if the context path is not well-formed.
     */
    String getJavaScript(String contextPath);

    /**
     * Reloads the current page in all connected browsers. If auto-reload is disabled, nothing happens.
     */
    void broadcastPageReload();

}
