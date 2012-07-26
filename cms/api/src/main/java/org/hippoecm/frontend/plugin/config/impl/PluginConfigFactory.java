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

import java.util.List;

import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.session.UserSession;

public class PluginConfigFactory implements IPluginConfigService {

    private static final long serialVersionUID = 1L;

    private IPluginConfigService pluginConfigService;

    public PluginConfigFactory(UserSession userSession, IApplicationFactory defaultFactory) {
        String appName = userSession.getApplicationName();
        IPluginConfigService baseService = null;
        try {
            if (appName == null) {
                baseService = defaultFactory.getDefaultApplication();
            } else {
                baseService = defaultFactory.getApplication(appName);
            }
        } catch(Exception ex) {
        }
        if (baseService == null) {
            JavaConfigService fallbackService = new JavaConfigService("test");
            JavaClusterConfig plugins = new JavaClusterConfig();
            fallbackService.addClusterConfig("test", plugins);
            baseService = fallbackService;
        }
        pluginConfigService = baseService;
    }

    public IClusterConfig getCluster(String key) {
        return pluginConfigService.getCluster(key);
    }

    public IClusterConfig getDefaultCluster() {
        return pluginConfigService.getDefaultCluster();
    }

    public List<String> listClusters(String folder) {
        return pluginConfigService.listClusters(folder);
    }

    public void detach() {
        pluginConfigService.detach();
    }

}
