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
package org.hippoecm.frontend.plugin.config;

import java.io.Serializable;
import java.util.Set;

import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.frontend.service.IRenderService;

/**
 * The plugin configuration.  It is a map with some helper methods 
 * inherited from the Wicket {@link IValueMap}.  Although the configuration
 * is specific to the class that uses it, it is recommended to use standard
 * keys for service names.  (e.g. the service name under which an {@link IRenderService}
 * should be registered is <code>wicket.id</code>)
 * <p>
 * It is observable, and will broadcast {@link PluginConfigEvent}s for
 * changes that occur at any depth in the reachable config hierarchy.
 * <p>
 * There are two implementations provided, {@link JcrPluginConfig}
 * and {@link JavaPluginConfig}.
 */
public interface IPluginConfig extends IValueMap, IObservable, Serializable {

    /**
     * The name of the configuration.  When the configuration is passed to a plugin,
     * this name is unique within the system.  It can therefore be used to construct
     * unique service names.
     */
    String getName();

    /**
     * Retrieve a child config of a particular name.  In the JCR implementation, this
     * corresponds to a child node of type frontend:pluginconfig.
     */
    IPluginConfig getPluginConfig(Object key);

    /**
     * Retrieve a set of child plugin configurations.
     */
    Set<IPluginConfig> getPluginConfigSet();

}
