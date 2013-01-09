/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.frontend.model.event.IEvent;

public class ClusterConfigEvent implements IEvent<IClusterConfig> {

    public enum EventType {
        PLUGIN_ADDED,
        PLUGIN_CHANGED,
        PLUGIN_REMOVED
    }

    private IClusterConfig source;
    private EventType type;
    private IPluginConfig plugin;

    public ClusterConfigEvent(IClusterConfig source, IPluginConfig plugin, EventType type) {
        this.source = source;
        this.plugin = plugin;
        this.type = type;
    }

    public EventType getType() {
        return type;
    }

    public IPluginConfig getPlugin() {
        return plugin;
    }
    
    public IClusterConfig getSource() {
        return source;
    }

}
