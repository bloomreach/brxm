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
package org.hippoecm.frontend.plugin.impl;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.wicket.Application;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceFactory;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugin.IServiceTracker;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.ClusterConfigDecorator;
import org.hippoecm.frontend.service.IRenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginContext implements IPluginContext, IDetachable {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PluginContext.class);

    private IPluginConfig config;
    private IPlugin plugin;
    private Map<String, List<IClusterable>> services;
    private Map<IClusterable, ServiceRegistration> registrations;
    private List<ServiceRegistration> registrationOrder;
    private Map<IServiceFactory<IClusterable>, IClusterable> instances;
    private Map<String, List<IServiceTracker<? extends IClusterable>>> listeners;
    private Map<String, ClusterControl> children;
    private PluginManager manager;
    transient boolean initializing = true;
    transient boolean stopping = false;

    public PluginContext(PluginManager manager, IPluginConfig config) {
        this.manager = manager;
        this.config = config;
        if (config == null || config.getName() == null) {
            throw new RuntimeException("Config (name) is null");
        }

        this.services = new LinkedHashMap<String, List<IClusterable>>();
        this.registrations = new IdentityHashMap<IClusterable, ServiceRegistration>();
        this.registrationOrder = new LinkedList<ServiceRegistration>();
        this.instances = new IdentityHashMap<IServiceFactory<IClusterable>, IClusterable>();
        this.listeners = new LinkedHashMap<String, List<IServiceTracker<? extends IClusterable>>>();
        this.children = new TreeMap<String, ClusterControl>();
    }

    public IClusterControl newCluster(IClusterConfig template, IPluginConfig parameters) {
        String name = template.getName();
        if (name != null) {
            name = name.replace(':', '_');
        } else {
            name = "";
        }
        String clusterIdBase = config.getName() + ".cluster." + name;
        String clusterId = clusterIdBase;
        int counter = 0;
        while (children.containsKey(clusterId)) {
            clusterId = clusterIdBase + counter++;
        }
        IClusterConfig decorator = new ClusterConfigDecorator(template, clusterId);
        ClusterControl cluster = new ClusterControl(manager, this, decorator, clusterId);

        for (String service : template.getServices()) {
            String serviceId = clusterId + ".service." + service.replace(':', '_');
            decorator.put(service, serviceId);
            if (parameters != null && parameters.getString(service) != null) {
                cluster.forward(serviceId, parameters.getString(service));
            }
        }
        for (String reference : template.getReferences()) {
            String serviceId;
            if (!template.getServices().contains(reference)) {
                serviceId = clusterId + ".reference." + reference.replace(':', '_');
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

    @SuppressWarnings("unchecked")
    public <T extends IClusterable> T getService(String name, Class<T> clazz) {
        T service = manager.getService(name, clazz);
        if (service != null) {
            return service;
        }

        List<IServiceFactory> list = manager.getServices(name, IServiceFactory.class);
        if (list != null && list.size() > 0) {
            for (IServiceFactory factory : list) {
                if (clazz.isAssignableFrom(factory.getServiceClass())) {
                    if (instances.containsKey(factory)) {
                        return (T) instances.get(factory);
                    } else {
                        service = (T) factory.getService(this);
                        instances.put(factory, service);
                        return service;
                    }
                }
            }
        }

        if (initializing && service == null) {
            for (Map.Entry<IClusterable, ServiceRegistration> entry : registrations.entrySet()) {
                ServiceRegistration registration = entry.getValue();
                if (registration.names.contains(name)) {
                    if (clazz.isInstance(entry.getKey())) {
                        return (T) entry.getKey();
                    }
                }
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends IClusterable> List<T> getServices(String name, Class<T> clazz) {
        List<T> result = manager.getServices(name, clazz);
        List<IServiceFactory> list = manager.getServices(name, IServiceFactory.class);
        if (list != null && list.size() > 0) {
            for (IServiceFactory factory : list) {
                if (clazz.isAssignableFrom(factory.getServiceClass())) {
                    T service;
                    if (instances.containsKey(factory)) {
                        service = (T) instances.get(factory);
                    } else {
                        service = (T) factory.getService(this);
                        instances.put(factory, service);
                    }
                    result.add(service);
                }
            }
        }
        if (initializing) {
            for (Map.Entry<IClusterable, ServiceRegistration> entry : registrations.entrySet()) {
                ServiceRegistration registration = entry.getValue();
                if (registration.names.contains(name)) {
                    if (clazz.isInstance(entry.getKey())) {
                        result.add((T) entry.getKey());
                    }
                }
            }
        }
        return result;
    }

    public <T extends IClusterable> IServiceReference<T> getReference(T service) {
        return manager.getReference(service);
    }

    public void registerService(IClusterable service, String name) {
        if (!stopping) {
            List<IClusterable> list = services.get(name);
            if (list == null) {
                list = new LinkedList<IClusterable>();
                services.put(name, list);
            }
            list.add(service);
            if (initializing) {
                ServiceRegistration registration;
                if (registrations.containsKey(service)) {
                    registration = registrations.get(service);
                    registration.addName(name);
                } else {
                    registration = manager.registerService(service, name);
                    registrations.put(service, registration);
                    registrationOrder.add(registration);
                }
            } else {
                ServiceRegistration registration = manager.registerService(service, name);
                if (registration != null) {
                    registration.notifyTrackers();
                }
            }
        } else {
            log.warn("plugin " + (plugin != null ? plugin.getClass().getName() : "<unknown>")
                    + " is registering a service when stopping; ignoring");
        }
    }

    public void unregisterService(IClusterable service, String name) {
        if (stopping) {
            return;
        }

        List<IClusterable> list = services.get(name);
        if (list != null) {
            if (!list.remove(service)) {
                log.warn("plugin " + (plugin != null ? plugin.getClass().getName() : "<unknown>")
                        + " is unregistering service at " + name + " that wasn't registered.");
                return;
            }
            if (initializing) {
                if (registrations.containsKey(service)) {
                    ServiceRegistration registration = registrations.get(service);
                    registration.removeName(name);
                    if (registration.names.size() == 0) {
                        registration.cleanup();
                        registrations.remove(service);
                        registrationOrder.remove(registration);
                    }
                } else {
                    log.warn("plugin " + (plugin != null ? plugin.getClass().getName() : "<unknown>")
                            + " is unregistering service at " + name + " that wasn't registered.");
                }
            } else {
                if (name != null) {
                    manager.unregisterService(service, name);
                }
            }
        } else {
            log.warn("plugin " + (plugin != null ? plugin.getClass().getName() : "<unknown>")
                    + " is unregistering service at " + name + " that wasn't registered.");
        }
    }

    public void registerTracker(IServiceTracker<? extends IClusterable> listener, String name) {
        if (name == null) {
            log.warn("plugin " + (plugin != null ? plugin.getClass().getName() : "<unknown>")
                    + " is registering a service tracker with a null name");
        }
        if (!stopping) {
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
    }

    public void unregisterTracker(IServiceTracker<? extends IClusterable> listener, String name) {
        if (!stopping) {
            List<IServiceTracker<? extends IClusterable>> list = listeners.get(name);
            if (list != null) {
                list.remove(listener);
                if (!initializing) {
                    manager.unregisterTracker(listener, name);
                }
            }
        }
    }

    // DO NOT CALL THIS API
    @SuppressWarnings("unchecked")
    public void connect(IPlugin plugin) {
        if (initializing) {
            this.plugin = plugin;
            Map<String, List> listeners = new LinkedHashMap(this.listeners);
            initializing = false;

            log.debug("registering trackers for plugin {}", plugin != null ? plugin.getClass().getName() : "unknown");
            for (Map.Entry<String, List> entry : listeners.entrySet()) {
                List<IServiceTracker<? extends IClusterable>> list = entry.getValue();
                for (IServiceTracker<? extends IClusterable> listener : list) {
                    manager.registerTracker(listener, entry.getKey());
                }
            }

            log.debug("registering services for plugin {}", plugin != null ? plugin.getClass().getName() : "unknown");
            for (ServiceRegistration registration : registrationOrder) {
                registration.notifyTrackers();
            }
            registrationOrder.clear();
            registrations.clear();

            if (plugin != null) {
                log.debug("starting plugin {}", plugin.getClass().getName());
                plugin.start();
            }
        } else {
            log.warn("context was already initialized");
        }
    }

    void registerControl(ClusterControl control) {
        children.put(control.getClusterId(), control);
    }

    void unregisterControl(ClusterControl control) {
        children.remove(control.getClusterId());
    }

    PluginContext start(IPluginConfig plugin) {
        return manager.start(plugin);
    }

    void stop() {
        if (!stopping) {
            stopping = true;

            if (plugin != null) {
                plugin.stop();
            }

            log.debug("stopping clusters for plugin {}", plugin != null ? plugin.getClass().getName() : "unknown");
            ClusterControl[] controls = children.values().toArray(new ClusterControl[children.size()]);
            for (ClusterControl control : controls) {
                control.stop();
            }

            if (!initializing) {
                log.debug("unregistering trackers for plugin {}", plugin != null ? plugin.getClass().getName() : "unknown");
                for (Map.Entry<String, List<IServiceTracker<? extends IClusterable>>> entry : listeners.entrySet()) {
                    for (IServiceTracker<? extends IClusterable> service : entry.getValue()) {
                        manager.unregisterTracker(service, entry.getKey());
                    }
                }
            }
            listeners.clear();

            log.debug("releasing service factory instances for plugin {}", plugin != null ? plugin.getClass().getName() : "unknown");
            for (Map.Entry<IServiceFactory<IClusterable>, IClusterable> entry : instances.entrySet()) {
                entry.getKey().releaseService(this, entry.getValue());
            }
            instances.clear();

            if (!initializing) {
                log.debug("unregistering services for plugin {}", plugin != null ? plugin.getClass().getName() : "unknown");
                for (Map.Entry<String, List<IClusterable>> entry : services.entrySet()) {
                    for (IClusterable service : entry.getValue()) {
                        final String key = entry.getKey();
                        if (key != null) {
                            manager.unregisterService(service, key);
                        }
                    }
                }
            } else {
                for (ServiceRegistration registration : registrationOrder) {
                    registration.cleanup();
                }
                registrationOrder.clear();
                registrations.clear();
            }
            services.clear();
        }
    }

    void reset() {
        stop();

        initializing = true;
        stopping = false;
    }
    
    public void detach() {
        for (Map.Entry<IServiceFactory<IClusterable>, IClusterable> entry : instances.entrySet()) {
            IClusterable service = entry.getValue();
            // FIXME: ugly!
            // should all services be IDetachable?
            if ((service instanceof IDetachable) && !(service instanceof IRenderService)) {
                ((IDetachable) service).detach();
            }
        }

        for (Map.Entry<String, List<IClusterable>> entry : services.entrySet()) {
            for (IClusterable service : entry.getValue()) {
                // FIXME: ugly!
                // should all services be IDetachable?
                if ((service instanceof IDetachable) && !(service instanceof IRenderService)) {
                    ((IDetachable) service).detach();
                }
            }
        }

        for (ClusterControl control : children.values()) {
            control.detach();
        }

        if (config != null && config instanceof IDetachable) {
            ((IDetachable) config).detach();
        }
    }

    private void writeObject(ObjectOutputStream output) throws IOException {
        if (stopping && Application.exists()) {
            if (Application.get().getConfigurationType().equals(RuntimeConfigurationType.DEVELOPMENT)) {
                throw new WicketRuntimeException("Stopped plugin is still being referenced: " + plugin.getClass().getName());
            }
        }
        output.defaultWriteObject();
    }

}
