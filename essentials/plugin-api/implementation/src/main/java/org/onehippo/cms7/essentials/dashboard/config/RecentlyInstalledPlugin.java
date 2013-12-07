/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.config;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
@DocumentType("ProjectSettingsBean")
@Node(discriminator = false, jcrType = "essentials:document")
public class RecentlyInstalledPlugin extends BaseDocument{


    @Field
    private String pluginClass;
    @Field
    private String description;
    @Field
    private String pluginName;

    public RecentlyInstalledPlugin() {
    }

    public RecentlyInstalledPlugin(final String name) {
        super(name);
    }

    public RecentlyInstalledPlugin(final String name, final String path) {
        super(name, path);
    }

    public String getPluginClass() {
        return pluginClass;
    }

    public void setPluginClass(final String pluginClass) {
        this.pluginClass = pluginClass;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(final String pluginName) {
        this.pluginName = pluginName;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RecentlyInstalledPlugin{");
        sb.append("pluginClass='").append(pluginClass).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", pluginName='").append(pluginName).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
