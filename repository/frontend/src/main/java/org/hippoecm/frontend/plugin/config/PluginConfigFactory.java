/*
 * Copyright 2007 Hippo
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
package org.hippoecm.frontend.plugin.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.Application;
import org.hippoecm.frontend.Main;

public class PluginConfigFactory {

    private final static String pluginConfigFactoryParam = "frontend-plugin-config";
    private final static String defaultPluginConfigFactory = "java";

    private Map pluginConfigs;

    public PluginConfigFactory() {
        pluginConfigs = new HashMap();
        pluginConfigs.put("java", PluginJavaConfig.class);
        pluginConfigs.put("repository", PluginRepositoryConfig.class);
        pluginConfigs.put("properties", PluginPropertiesConfig.class);
        pluginConfigs.put("spring", PluginSpringConfig.class);
    }

    public PluginConfig getPluginConfig() {
        Main main = (Main) Application.get();
        String pluginConfigType = main.getConfigurationParameter(pluginConfigFactoryParam, defaultPluginConfigFactory);
             
        PluginConfig result;
        try {
            Class pluginConfigClass = (Class)pluginConfigs.get(pluginConfigType);
            result = (PluginConfig)pluginConfigClass.newInstance();
        } catch (Exception e) {
            String message = e.getClass().getName() + ": " + e.getMessage();
            message += "\nFailed to initialize plugin configuration '" + pluginConfigType + "', falling back to default hardcoded configuration.\n";
            System.err.println(message);
            result = new PluginJavaConfig();
         }
        return result;
    }

}
