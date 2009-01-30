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
package org.hippoecm.frontend.plugin.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugin.IServiceTracker;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.ClusterConfigDecorator;
import org.hippoecm.frontend.service.IRenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginContext implements IPluginContext, IDetachable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PluginContext.class);

    private String controlId;
    private IPluginConfig config;
    private Map<String, List<IClusterable>> services;
    private Map<String, List<IServiceTracker<? extends IClusterable>>> listeners;
    private List<ClusterControl> children;
    private boolean initializing = true;
    private PluginManager manager;
    private int clusterCount = 0;

    public PluginContext(PluginManager manager, String controlId, IPluginConfig config) {
        this.controlId = controlId;
        this.manager = manager;
        this.config = config;

        this.services = new HashMap<String, List<IClusterable>>();
        this.listeners = new HashMap<String, List<IServiceTracker<? extends IClusterable>>>();
        this.children = new LinkedList<ClusterControl>();
    }

    public IClusterControl newCluster(IClusterConfig template, IPluginConfig parameters) {
        String clusterId = controlId + ":" + template.getName() + (clusterCount++);
        IClusterConfig decorator = new ClusterConfigDecorator(template, clusterId);
        ClusterControl cluster = new ClusterControl(manager, this, decorator, clusterId);

        for (String service : template.getServices()) {
            String serviceId = clusterId + ":" + service + "(srv)";
            decorator.put(service, serviceId);
            if (parameters != null && parameters.getString(service) != null) {
                cluster.forward(serviceId, parameters.getString(service));
            }
        }
        for (String reference : template.getReferences()) {
            String serviceId;
            if (!template.getServices().contains(reference)) {
                serviceId = clusterId + ":" + reference + "(ref)";
                decorator.put(reference, serviceId);
            } else {
                serviceId = decorator.getString(reference);
            }
            if (parameters != null && parameters.getString(reference) != null) {
                cluster.forward(parameters.getString(reference), serviceId);
            }
        }
        for (String property : template.getProperties()) {
            if (parameters != null && parameters.get(property) != null) {
                decorator.put(property, parameters.get(property));
            }
        }
        return cluster;
    }

    public <T extends IClusterable> T getService(String name, Class<T> clazz) {
        return manager.getService(name, clazz);
    }

    public <T extends IClusterable> List<T> getServices(String name, Class<T> clazz) {
        return manager.getServices(name, clazz);
    }

    public <T extends IClusterable> IServiceReference<T> getReference(T service) {
        return manager.getReference(service);
    }

    public void registerService(IClusterable service, String name) {
        List<IClusterable> list = services.get(name);
        if (list == null) {
            list = new LinkedList<IClusterable>();
            services.put(name, list);
        }
        list.add(service);
        manager.registerService(service, name);
    }

    public void unregisterService(IClusterable service, String name) {
        List<IClusterable> list = services.get(name);
        if (list != null) {
            list.remove(service);
            manager.unregisterService(service, name);
        } else {
            log.warn("unregistering service that wasn't registered.");
        }
    }

    public void registerTracker(IServiceTracker<? extends IClusterable> listener, String name) {
        if (name == null) {
            log.error("listener name is null");
        }
        List<IServiceTracker<? extends IClusterable>> list = listeners.get(name);
        if (list == null) {
            list = new LinkedList<IServiceTracker<? extends IClusterable>>();
            listeners.put(name, list);
        }
        list.add(listener);
        if (!initializing) {
            manager.registerTracker(listener, name);
        }
    }

    public void unregisterTracker(IServiceTracker<? extends IClusterable> listener, String name) {
        List<IServiceTracker<? extends IClusterable>> list = listeners.get(name);
        if (list != null) {
            list.remove(listener);
            if (!initializing) {
                manager.unregisterTracker(listener, name);
            }
        }
    }

    // DO NOT CALL THIS API
    public void connect(IPlugin plugin) {
        if (initializing) {
            for (Map.Entry<String, List<IServiceTracker<? extends IClusterable>>> entry : listeners.entrySet()) {
                List<IServiceTracker<? extends IClusterable>> list = entry.getValue();
                for (IServiceTracker<? extends IClusterable> listener : list) {
                    manager.registerTracker(listener, entry.getKey());
                }
            }
            initializing = false;
        } else {
            log.warn("context was already initialized");
        }
    }

    void registerControl(ClusterControl control) {
        children.add(control);
    }

    void unregisterControl(ClusterControl control) {
        children.remove(control);
    }

    PluginContext start(IPluginConfig plugin, String clusterId) {
        return manager.start(plugin, clusterId);
    }

    void stop() {
        ClusterControl[] controls = children.toArray(new ClusterControl[children.size()]);
        for (ClusterControl control : controls) {
            control.stop();
        }

        for (Map.Entry<String, List<IServiceTracker<? extends IClusterable>>> entry : listeners.entrySet()) {
            for (IServiceTracker<? extends IClusterable> service : entry.getValue()) {
                manager.unregisterTracker(service, entry.getKey());
            }
        }
        listeners = new HashMap<String, List<IServiceTracker<? extends IClusterable>>>();

        for (Map.Entry<String, List<IClusterable>> entry : services.entrySet()) {
            for (IClusterable service : entry.getValue()) {
                manager.unregisterService(service, entry.getKey());
            }
        }
        services = new HashMap<String, List<IClusterable>>();
    }

    public void detach() {
        for (Map.Entry<String, List<IClusterable>> entry : services.entrySet()) {
            for (IClusterable service : entry.getValue()) {
                // FIXME: ugly!
                // should all services be IDetachable?
                if ((service instanceof IDetachable) && !(service instanceof IRenderService)) {
                    ((IDetachable) service).detach();
                }
            }
        }

        for (ClusterControl control : children) {
            control.detach();
        }

        if (config != null) {
            config.detach();
        }
    }

}
