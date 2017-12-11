/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
 */

package org.onehippo.cms7.essentials.dashboard.model;

/**
 * Library represents an artifact to be loaded into Essentials' front-end dashboard in order to enable a certain
 * functionality of an Essentials Plugin, such as installation of a feature or a configuration screen of a tool.
 *
 * It is typically defined through the plugin's descriptor, and passed to the dashboard application to dynamically
 * load additional resources (JS, CSS).
 */
public class Library {
    private String browser;   // browser restriction, only supports IE-related checks // TODO remove?
    private String component; // typically the plugin-ID
    private String file;      // filename of the resource, to be found at /<plugin-type>/<component>/<file>

    public Library(final String component, final String file, final String browser) {
        this.component = component;
        this.file = file;
        this.browser = browser;
    }

    public Library(final String component, final String file) {
        this.component = component;
        this.file = file;
    }

    public Library() {
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(final String browser) {
        this.browser = browser;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(final String component) {
        this.component = component;
    }

    public String getFile() {
        return file;
    }

    public void setFile(final String file) {
        this.file = file;
    }
}
