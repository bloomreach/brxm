/*
 * Copyright 2011-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.templatecomposer.plugins;

public final class PluginsBundle {

    private PluginsBundle() {
        // prevent instantiation
    }

    /**
     * List of files that make up the plugins bundle, in the order they should be loaded.
     * N.B. this list is duplicated in the pom.xml file of this module to describe the concatenated
     * sources. When updating this list, make sure to also update the list in the pom.xml file.
     */
    public static final String[] FILES = {
            "floatingwindow/FloatingWindow.js",
            "colorfield/colorfield.js",
            "vtabs/VerticalTabPanel.js"
    };

    /**
     * File that contains all sources concatenated.
     */
    public static final String ALL = "plugins-all.js";

}
