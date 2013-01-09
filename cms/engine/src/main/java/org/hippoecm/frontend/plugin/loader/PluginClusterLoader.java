/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugin.loader;

import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin loads all the plugins from a specified cluster
 *
 */
public class PluginClusterLoader extends Plugin {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PluginClusterLoader.class);

    public static final String CLUSTER_NAME = "cluster.name";
    public static final String CLUSTER_PARAMETERS = "cluster.config";

    public PluginClusterLoader(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }
    
    @Override
    public void start() {
        IPluginContext context = getPluginContext();
        IPluginConfig config = getPluginConfig();
        IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                IPluginConfigService.class);

        String clusterName = config.getString(CLUSTER_NAME);
        if (clusterName == null) {
            log.warn("cluster.name not found. Check your configuration in the console.");
        } else {
            IClusterConfig cluster = pluginConfigService.getCluster(clusterName);
            if (cluster == null) {
                log.warn("Unable to find cluster '" + clusterName + "'. Does it exist in repository?");
            } else {
                IPluginConfig parameters = config.getPluginConfig(CLUSTER_PARAMETERS);
                IClusterControl control = context.newCluster(cluster, parameters);
                control.start();
            }
        }
        super.start();
    }

}
