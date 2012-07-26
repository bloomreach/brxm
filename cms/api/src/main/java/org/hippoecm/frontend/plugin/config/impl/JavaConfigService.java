/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugin.config.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;

public class JavaConfigService implements IPluginConfigService {

    private static final long serialVersionUID = 1L;

    private Map<String, IClusterConfig> configs;

    private String defaultCluster;

    public JavaConfigService(String defaultCluster) {
        this.defaultCluster = defaultCluster;
        configs = new HashMap<String, IClusterConfig>();
    }

    public IClusterConfig getCluster(String key) {
        return configs.get(key);
    }

    public IClusterConfig getDefaultCluster() {
        return configs.get(defaultCluster);
    }

    public void detach() {
        for (IClusterConfig config : configs.values()) {
            if (config instanceof IDetachable) {
                ((IDetachable) config).detach();
            }
        }
    }

    public void addClusterConfig(String name, IClusterConfig configuration) {
        configs.put(name, configuration);
    }

    public List<String> listClusters(String folder) {
        List<String> result = new ArrayList<String>();
        for (String name : configs.keySet()) {
            if (name.startsWith(folder + "/")) {
                result.add(name);
            }
        }
        return result;
    }

}
