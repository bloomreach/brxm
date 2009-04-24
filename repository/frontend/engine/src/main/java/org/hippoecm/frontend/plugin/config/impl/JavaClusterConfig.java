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

import java.util.LinkedList;
import java.util.List;

import org.hippoecm.frontend.model.event.ListenerList;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IClusterConfigListener;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class JavaClusterConfig extends JavaPluginConfig implements IClusterConfig {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private List<IPluginConfig> plugins;
    private List<String> services;
    private List<String> references;
    private List<String> properties;
    private List<IClusterConfigListener> listeners;

    public JavaClusterConfig() {
        plugins = new LinkedList<IPluginConfig>();
        services = new LinkedList<String>();
        references = new LinkedList<String>();
        properties = new LinkedList<String>();
        listeners = new ListenerList<IClusterConfigListener>();
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
        this.listeners = new ListenerList<IClusterConfigListener>();
    }

    public void addPlugin(IPluginConfig config) {
        plugins.add(config);
        for (IClusterConfigListener listener : listeners) {
            listener.onPluginAdded(config);
        }
    }

    public List<IPluginConfig> getPlugins() {
        return plugins;
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

    public void addClusterConfigListener(IClusterConfigListener listener) {
        listeners.add(listener);
    }

    public void removeClusterConfigListener(IClusterConfigListener listener) {
        listeners.remove(listener);
    }

}
