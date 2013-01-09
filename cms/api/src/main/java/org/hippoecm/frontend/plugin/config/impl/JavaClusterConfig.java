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
package org.hippoecm.frontend.plugin.config.impl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.plugin.config.ClusterConfigEvent;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class JavaClusterConfig extends JavaPluginConfig implements IClusterConfig {

    private static final long serialVersionUID = 1L;

    private List<IPluginConfig> plugins;
    private List<String> services;
    private List<String> references;
    private List<String> properties;

    public JavaClusterConfig() {
        plugins = new LinkedList<IPluginConfig>();
        services = new LinkedList<String>();
        references = new LinkedList<String>();
        properties = new LinkedList<String>();
    }

    public JavaClusterConfig(IClusterConfig upstream) {
        super(upstream);

        plugins = new LinkedList<IPluginConfig>();
        for (IPluginConfig config : upstream.getPlugins()) {
            plugins.add(newPluginConfig(config));
        }
        this.services = upstream.getServices();
        this.references = upstream.getReferences();
        this.properties = upstream.getProperties();
    }

    @SuppressWarnings("unchecked")
    public void addPlugin(IPluginConfig config) {
        plugins.add(config);
        IObservationContext<IClusterConfig> obContext = (IObservationContext<IClusterConfig>) getObservationContext();
        if (obContext != null) {
            EventCollection<IEvent<IClusterConfig>> collection = new EventCollection<IEvent<IClusterConfig>>();
            collection.add(new ClusterConfigEvent(this, config, ClusterConfigEvent.EventType.PLUGIN_ADDED));
            obContext.notifyObservers(collection);
        }
    }

    public List<IPluginConfig> getPlugins() {
        return Collections.unmodifiableList(plugins);
    }

    public void setPlugins(List<IPluginConfig> plugins) {
        throw new UnsupportedOperationException();
    }

    public void addService(String key) {
        services.add(key);
    }

    public List<String> getServices() {
        return services;
    }

    public void addReference(String key) {
        references.add(key);
    }

    public List<String> getReferences() {
        return references;
    }

    public void addProperty(String key) {
        properties.add(key);
    }

    public List<String> getProperties() {
        return properties;
    }

}
