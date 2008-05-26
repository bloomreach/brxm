/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.sa.template.impl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.sa.template.ITemplateConfig;

public class JavaTemplateConfig extends JavaPluginConfig implements ITemplateConfig {
    private static final long serialVersionUID = 1L;

    private IPluginConfig[] plugins;
    private List<String> properties;

    public JavaTemplateConfig(IPluginConfig[] plugins) {
        this.plugins = plugins;
        this.properties = new LinkedList<String>();
    }

    // implement ITemplateConfig

    public List<String> getPropertyKeys() {
        return properties;
    }

    public List<IPluginConfig> getPlugins() {
        List<IPluginConfig> list = new ArrayList<IPluginConfig>(plugins.length);
        for (IPluginConfig config : plugins) {
            list.add(config);
        }
        return list;
    }

    // managing the properties
    public void addProperty(String key) {
        properties.add(key);
    }

    public void removeProperty(String key) {
        properties.remove(key);
    }

}
