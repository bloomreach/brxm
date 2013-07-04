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
package org.hippoecm.frontend.service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugin.IServiceTracker;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

/**
 * Wrapper class for IPluginContext that keeps a record of what services and trackers are registered and what clusters
 * are started.  These can later be cleaned up by invoking stop().
 */
public class ServiceContext implements IPluginContext {

    private static final long serialVersionUID = 1L;

    private IPluginContext upstream;
    private Map<String, List<IClusterable>> services;
    private Map<String, List<IServiceTracker<? extends IClusterable>>> listeners;
    private List<IClusterControl> children;

    public ServiceContext(IPluginContext upstream) {
        this.upstream = upstream;

        this.services = new HashMap<String, List<IClusterable>>();
        this.listeners = new HashMap<String, List<IServiceTracker<? extends IClusterable>>>();
        this.children = new LinkedList<IClusterControl>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IClusterControl newCluster(IClusterConfig template, IPluginConfig parameters) {
        final IClusterControl control = upstream.newCluster(template, parameters);
        children.add(control);
        return new IClusterControl() {
            private static final long serialVersionUID = 1L;

            @Override
            public IClusterConfig getClusterConfig() {
                return control.getClusterConfig();
            }

            @Override
            public void start() {
                control.start();
            }

            @Override
            public void stop() {
                control.stop();
                children.remove(control);
            }

        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends IClusterable> T getService(String name, Class<T> clazz) {
        return upstream.getService(name, clazz);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends IClusterable> List<T> getServices(String name, Class<T> clazz) {
        return upstream.getServices(name, clazz);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends IClusterable> IServiceReference<T> getReference(T service) {
        return upstream.getReference(service);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerService(IClusterable service, String name) {
        upstream.registerService(service, name);
        List<IClusterable> list = services.get(name);
        if (list == null) {
            list = new LinkedList<IClusterable>();
            services.put(name, list);
        }
        list.add(service);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterService(IClusterable service, String name) {
        List<IClusterable> list = services.get(name);
        list.remove(service);
        if (list.isEmpty()) {
            services.remove(name);
        }
        upstream.unregisterService(service, name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerTracker(IServiceTracker<? extends IClusterable> listener, String name) {
        upstream.registerTracker(listener, name);
        List<IServiceTracker<? extends IClusterable>> list = listeners.get(name);
        if (list == null) {
            list = new LinkedList<IServiceTracker<? extends IClusterable>>();
            listeners.put(name, list);
        }
        list.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterTracker(IServiceTracker<? extends IClusterable> listener, String name) {
        List<IServiceTracker<? extends IClusterable>> list = listeners.get(name);
        list.remove(listener);
        if (list.isEmpty()) {
            listeners.remove(name);
        }
        upstream.unregisterTracker(listener, name);
    }

    /**
     * Stop clusters that have been started using this service context.  Unregister services and trackers
     * that have been registered.
     */
    public void stop() {
        IClusterControl[] controls = children.toArray(new IClusterControl[children.size()]);
        for (IClusterControl control : controls) {
            control.stop();
        }
        children.clear();

        for (Map.Entry<String, List<IServiceTracker<? extends IClusterable>>> entry : listeners.entrySet()) {
            for (IServiceTracker<? extends IClusterable> service : entry.getValue()) {
                upstream.unregisterTracker(service, entry.getKey());
            }
        }
        listeners.clear();

        for (Map.Entry<String, List<IClusterable>> entry : services.entrySet()) {
            for (IClusterable service : entry.getValue()) {
                upstream.unregisterService(service, entry.getKey());
            }
        }
        services.clear();

        upstream = null;
    }

    /**
     * reattach a service context to a plugin context.
     * This operation is only valid if the service context was stopped before.
     *
     * @param pluginContext the plugin context to attach to.
     */
    public void attachTo(final IPluginContext pluginContext) {
        if (upstream == null) {
            upstream = pluginContext;
        } else if (pluginContext != upstream) {
            throw new IllegalStateException("Service context is still connected");
        }
    }

}
