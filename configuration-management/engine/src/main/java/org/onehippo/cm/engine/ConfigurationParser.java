/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cm.engine;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.onehippo.cm.api.model.Configuration;

class ConfigurationParser {

    static final String MODULE_CONFIG_FILENAME = "module-config.yaml";

    Map<String, Configuration> parse(final List<URL> urls) throws IOException {
        return null;
        /*
        Map<String, ConfigurationImpl> groups = new LinkedHashMap<>();
        Map<String, ConfigurationModuleImpl> modules = new LinkedHashMap<>();
        Map<ConfigurationModuleImpl, URL> moduleConfigs = new HashMap<>();
        List<URL> sourceConfigs = new ArrayList<>();

        for (URL url : urls) {
            final String[] parts = url.toString().split("/");
            if (parts.length < 3) {
                throw new IllegalArgumentException(
                        "URL must be composed of 3 or more elements, found only " + parts.length + " elements in "
                        + url.toString());
            }
            final String groupName = parts[parts.length - 3];
            final String moduleName = parts[parts.length - 2];
            final String fileName = parts[parts.length - 1];

            ConfigurationGroupImpl group = groups.get(groupName);
            if (group == null) {
                group = new ConfigurationGroupImpl(groupName);
                groups.put(groupName, group);
            }

            ConfigurationModuleImpl module = modules.get(groupName + "/" + moduleName);
            if (module == null) {
                module = new ConfigurationModuleImpl(group, moduleName);
                modules.put(groupName + "/" + moduleName, module);
                group.addModule(module);
            }

            if (MODULE_CONFIG_FILENAME.equals(fileName)) {
                moduleConfigs.put(module, url);
            } else {
                sourceConfigs.add(url);
            }
        }

        for (ConfigurationModuleImpl module : moduleConfigs.keySet()) {
            final URL moduleConfig = moduleConfigs.get(module);
            parseModuleConfig(moduleConfig, module, modules);
        }

        return new ArrayList<>(groups.values());
        */
    }

}
