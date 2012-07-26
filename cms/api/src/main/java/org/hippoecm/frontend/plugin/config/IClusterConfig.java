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
package org.hippoecm.frontend.plugin.config;

import java.util.List;

import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.service.IRenderService;

/**
 * Descriptor of a cluster of plugins.  These can interact with other plugins by declaring
 * what services are consumed, what services are offered and what properties can be set.
 * <p>
 * Entries at the top level are available as variables to plugins when the cluster is instantiated.
 * Suppose the cluster config has the key "mykey", value "myvalue".  A plugin config contains
 * the key "pluginKey" with value "${mykey}.extra".  When the instantiated {@link IPlugin} invokes
 * {@link IPluginConfig#getString("mykey")}, the value "myvalue.extra" will be returned.
 * <p>
 * In addition to these variables, the "cluster.id" variable has a value that is unique to the
 * instantiated cluster.  It can be used to create cluster-specific service names.
 * <p>
 * When the configuration has been obtained from an instantiated cluster with
 * {@link IClusterControl#getClusterConfig()}, all variable expansion has been applied.  The
 * configuration is read-only in that case.
 */
public interface IClusterConfig extends IPluginConfig {

    /**
     * The plugin configurations in the cluster.
     * Returns an immutable list of plugins.
     */
    List<IPluginConfig> getPlugins();

    /**
     * Update the plugins in the cluster.  Only available when the the configuration is used as a
     * template for the cluster configuration.
     */
    void setPlugins(List<IPluginConfig> plugins);
    
    /**
     * The list of keys for services.  Since service types are not available, it is recommended to
     * always use well-known keys when they exist.  I.e. use "wicket.id" to identify an {@link IRenderService}.
     * <p>
     * The returned list is only mutable when the configuration is used as a template.
     */
    List<String> getServices();

    /**
     * The keys for services that are used by plugins in the cluster.
     * <p>
     * The returned list is only mutable when the configuration is used as a template.
     */
    List<String> getReferences();

    /**
     * Properties that specify additional plugin behavior.
     * <p>
     * The returned list is only mutable when the configuration is used as a template.
     */
    List<String> getProperties();

}
