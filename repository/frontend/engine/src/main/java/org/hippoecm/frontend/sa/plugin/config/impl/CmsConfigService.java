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
package org.hippoecm.frontend.sa.plugin.config.impl;

import java.util.LinkedList;
import java.util.List;

import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfigService;

// Legacy cms application
// REMOVE this class when all cms plugins have been ported to the new architecture
class CmsConfigService implements IPluginConfigService {
    private static final long serialVersionUID = 1L;

    private List<IPluginConfig> plugins;

    CmsConfigService() {
        plugins = new LinkedList<IPluginConfig>();
     
        // wrap legacy cms plugins
        IPluginConfig config = new JavaPluginConfig();
        config.put("plugin.class", "org.hippoecm.frontend.sa.adapter.AdapterPlugin");
        config.put("wicket.id", "service.root");                                  
        config.put("legacy.base", "/hippo:configuration/hippo:frontend_deprecated/hippo:cms");
        config.put("legacy.plugin", "rootPlugin");
        
        plugins.add(config);
    }

    public List<IPluginConfig> getPlugins(String key) {
        return plugins;
    }
    

}
