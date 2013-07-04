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
package org.hippoecm.frontend.plugin;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

/**
 * A controller for a cluster of plugins.  It can be used to start and stop the plugins in the cluster.
 * <p>
 * Plugins that repeatedly instantiated other clusters of plugins must stop the cluster when it is no longer
 * in use.  If the cluster has the same lifecycle as the plugin, this is not necessary; the plugin framework
 * will stop the cluster when the plugin is stopped.
 */
public interface IClusterControl extends IClusterable {

    /**
     * The cluster configuration.  All parameters have been filled in, so it is no longer the template
     * that was used to create the cluster control in {@link IPluginContext#newCluster(IClusterConfig, IPluginConfig)}.
     *
     * @return the cluster configuration
     */
    IClusterConfig getClusterConfig();

    /**
     * Start the plugins in the cluster.
     */
    void start();

    /**
     * Stop the plugins in the cluster.
     */
    void stop();

}
