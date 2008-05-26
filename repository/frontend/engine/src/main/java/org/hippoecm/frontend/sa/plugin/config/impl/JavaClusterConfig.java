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
package org.hippoecm.frontend.sa.plugin.config.impl;

import java.util.LinkedList;
import java.util.List;

import org.hippoecm.frontend.sa.plugin.config.IClusterConfig;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;

public class JavaClusterConfig extends JavaPluginConfig implements IClusterConfig {
    private static final long serialVersionUID = 1L;

    private List<String> keys;
    private List<IPluginConfig> configs;

    public JavaClusterConfig() {
        configs = new LinkedList<IPluginConfig>();
        keys = new LinkedList<String>();
    }

    public void addPlugin(IPluginConfig config) {
        configs.add(config);
    }

    public void addProperty(String key) {
        keys.add(key);
    }

    public List<IPluginConfig> getPlugins() {
        return configs;
    }

    public List<String> getPropertyKeys() {
        return keys;
    }
}
