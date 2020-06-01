/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IServiceTracker;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterControl implements IClusterControl, IServiceTracker<IClusterable>, IDetachable {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ClusterControl.class);

    static class Extension implements IClusterable {
        private static final long serialVersionUID = 1L;

        ServiceForwarder forwarder;
        List<String> names;

        Extension(ServiceForwarder forwarder) {
            this.forwarder = forwarder;
            this.names = new LinkedList<String>();
        }

        void addName(String name) {
            names.add(name);
        }

        void removeName(String name) {
            names.remove(name);
        }

        boolean exists() {
            return names.size() > 0;
        }
    }

    private PluginManager mgr;
    private PluginContext context;
    private IClusterConfig config;
    private String clusterId;
    private Map<String, Extension> extensions;
    private List<ServiceForwarder> forwarders;
    private PluginContext[] contexts;
    private boolean running = false;
    private boolean hollow = false;

    ClusterControl(PluginManager mgr, PluginContext context, IClusterConfig config, String id) {
        this.mgr = mgr;
        this.context = context;
        this.config = config;
        this.clusterId = id;
        forwarders = new LinkedList<ServiceForwarder>();
        extensions = new HashMap<String, Extension>();
    }

    public IClusterConfig getClusterConfig() {
        return config;
    }

    public <S extends IClusterable> S getService(String name, Class<S> clazz) {
        if (running) {
            return context.getService(config.getString(name), clazz);
        } else {
            log.warn("service cannot be acquired after cluster has been stopped");
            return null;
        }
    }

    public void start() {
        if (running) {
            log.warn("cluster has already been started");
            return;
        }

        if (context.stopping) {
            hollow = true;
            log.debug("plugin is stopping, not starting cluster " + clusterId);
            return;
        }
        
        running = true;
        contexts = new PluginContext[config.getPlugins().size()];

        // start forwarders in two steps
        // all need to have started before they actually register services;
        // this is necessary for them to be able to detect loops.
        for (ServiceForwarder forwarder : forwarders) {
            forwarder.start();
        }
        for (ServiceForwarder forwarder : forwarders) {
            forwarder.connect();
        }

        for (String service : config.getServices()) {
            context.registerTracker(this, config.getString(service));
        }

        int i = 0;
        for (IPluginConfig plugin : config.getPlugins()) {
            contexts[i++] = context.start(plugin);
        }

        context.registerControl(this);

        if (log.isDebugEnabled()) {
            log.debug("cluster " + clusterId + " has started");
        }
    }

    public void stop() {
        if (hollow) {
            hollow = false;
            return;
        }

        if (!running) {
            log.debug("cluster has already been stopped");
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("stopping cluster " + clusterId);
        }

        running = false;

        context.unregisterControl(this);

        for (PluginContext context : contexts) {
            if (context != null) {
                context.stop();
            }
        }
        contexts = null;

        for (String service : config.getServices()) {
            context.unregisterTracker(this, config.getString(service));
        }
        for (ServiceForwarder forwarder : forwarders) {
            forwarder.disconnect();
        }
        for (ServiceForwarder forwarder : forwarders) {
            forwarder.stop();
        }
    }

    public void addService(IClusterable service, String name) {
        String serviceId = context.getReference(service).getServiceId();
        Extension extension = extensions.get(serviceId);
        if (extension == null) {
            ServiceForwarder forwarder = new ServiceForwarder(mgr, IClusterable.class, clusterId, serviceId);
            extension = new Extension(forwarder);
            extensions.put(serviceId, extension);
            forwarder.start();
        }
        extension.addName(name);
    }

    public void removeService(IClusterable service, String name) {
        String serviceId = context.getReference(service).getServiceId();
        Extension extension = extensions.get(serviceId);
        if (extension != null) {
            extension.removeName(name);
            if (!extension.exists()) {
                extension.forwarder.stop();
                extensions.remove(serviceId);
            }
        } else {
            log.error("Unknown extension " + serviceId + " of service " + service);
        }
    }

    public void detach() {
        if (config instanceof IDetachable) {
            ((IDetachable) config).detach();
        }
        for (PluginContext context : contexts) {
            if (context != null) {
                context.detach();
            }
        }
    }

    void forward(String source, String target) {
        forwarders.add(new ServiceForwarder(mgr, IClusterable.class, source, target));
    }

    String getClusterId() {
        return clusterId;
    }
}
