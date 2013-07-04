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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.PluginPage;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugin.IServiceTracker;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
public class PluginManager implements IClusterable {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PluginManager.class);

    public static final String SERVICES = "services.";

    static class RefCount implements IClusterable {
        private static final long serialVersionUID = 1L;

        IClusterable service;
        int count;

        RefCount(IClusterable service) {
            this.service = service;
            count = 0;
        }

        void addRef() {
            count++;
        }

        boolean release() {
            return (--count == 0);
        }
    }

    private PluginPage page;
    private PluginFactory factory;
    private Map<String, List<IClusterable>> services;
    private Map<String, List<IServiceTracker>> listeners;
    private Map<Integer, RefCount> referenced;
    private Map<IClusterable, Integer> lookupMap;
    private int nextReferenceId;

    public PluginManager(PluginPage page) {
        this.page = page;
        factory = new PluginFactory();
        services = new HashMap<String, List<IClusterable>>();
        listeners = new HashMap<String, List<IServiceTracker>>();
        referenced = new HashMap<Integer, RefCount>();
        lookupMap = new IdentityHashMap<IClusterable, Integer>();
        nextReferenceId = 0;
    }

    public PluginContext start(IPluginConfig config) {
        final PluginContext context = new PluginContext(this, config);
        IPlugin plugin = factory.createPlugin(context, config);
        if (plugin != null) {
            context.connect(plugin);
            return context;
        } else {
            context.stop();
            return null;
        }
    }

    <T extends IClusterable> T getService(String name, Class<T> clazz) {
        List<IClusterable> list = services.get(name);
        if (list != null && list.size() > 0) {
            for (IClusterable service : list) {
                if (clazz.isInstance(service)) {
                    return (T) service;
                }
            }
        }
        return null;
    }

    <T extends IClusterable> List<T> getServices(String name, Class<T> clazz) {
        List<IClusterable> list = services.get(name);
        List<T> result = new ArrayList<T>();
        if (list != null && list.size() > 0) {
            for (IClusterable service : list) {
                if (clazz.isInstance(service)) {
                    result.add((T) service);
                }
            }
        }
        return result;
    }

    <T extends IClusterable> IServiceReference<T> getReference(T service) {
        Map.Entry<Integer, RefCount> entry = internalGetReference(service);
        if (entry == null) {
            log.warn("Referenced service " + service + " (hash: " + service.hashCode() + ") was not registered");
            return null;
        }
        return new ServiceReference<T>(page, SERVICES + entry.getKey());
    }

    ServiceRegistration registerService(IClusterable service, String name) {
        if (log.isDebugEnabled()) {
            log.debug("registering " + service + " (hash: " + service.hashCode() + ") as " + name);
        }

        Map.Entry<Integer, RefCount> entry = internalGetReference(service);
        if (entry == null) {
            Integer id = nextReferenceId++;
            referenced.put(id, new RefCount(service));
            lookupMap.put(service, id);
            if (log.isDebugEnabled()) {
                log.debug("assigning id " + id + " to service " + service + " (hash: " + service.hashCode() + ")");
            }
            String serviceId = SERVICES + id;
            return new ServiceRegistration(this, service, serviceId, name);
        } else {
            entry.getValue().addRef();

            if (name != null) {
                internalRegisterService(service, name);
            }
            return null;
        }
    }

    void cleanup(IClusterable service) {
        Map.Entry<Integer, RefCount> entry = internalGetReference(service);
        if (entry != null) {
            referenced.remove(entry.getKey());
            lookupMap.remove(service);
        }
    }

    void unregisterService(IClusterable service, String name) {
        if (log.isDebugEnabled()) {
            log.debug("unregistering " + service + " (hash: " + service.hashCode() + ") from " + name);
        }
        if (name != null) {
            List<IClusterable> list = services.get(name);
            if (list != null) {
                internalUnregisterService(service, name);
            } else {
                log.error("Unknown service name {}.", name);
            }
        }

        Map.Entry<Integer, RefCount> entry = internalGetReference(service);
        if (entry != null) {
            RefCount ref = entry.getValue();
            if (ref.release()) {
                Integer id = entry.getKey();
                String serviceId = SERVICES + id;
                if (log.isDebugEnabled()) {
                    log.debug("dropping id " + id + " for service " + service + " (hash: " + service.hashCode() + ")");
                }
                internalUnregisterService(service, serviceId);
                referenced.remove(id);
                lookupMap.remove(service);
            }
        } else {
            log.error("unregistering a service that wasn't registered.");
        }
    }

    void registerTracker(IServiceTracker listener, String name) {
        if (name == null) {
            log.warn("listener name is null");
            return;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("registering listener " + listener + " for " + name);
            }
        }

        List<IServiceTracker> list = listeners.get(name);
        if (list == null) {
            list = new LinkedList<IServiceTracker>();
            listeners.put(name, list);
        }
        list.add(listener);

        List<IClusterable> notify = services.get(name);
        if (notify != null) {
            Iterator<IClusterable> iter = notify.iterator();
            while (iter.hasNext()) {
                IClusterable service = iter.next();
                listener.addService(service, name);
            }
        }
    }

    void unregisterTracker(IServiceTracker listener, String name) {
        if (name == null) {
            log.error("listener name is null");
            return;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("unregistering listener " + listener + " for " + name);
            }
        }

        List<IClusterable> notify = services.get(name);
        if (notify != null) {
            for (IClusterable service : new ArrayList<IClusterable>(notify)) {
                if (notify.contains(service)) {
                    listener.removeService(service, name);
                }
            }
        }

        List<IServiceTracker> list = listeners.get(name);
        if (list != null) {
            if (list.contains(listener)) {
                list.remove(listener);
            }
            if (list.isEmpty()) {
                listeners.remove(name);
            }
        } else {
            log.error("unregistering a listener that wasn't registered.");
        }
    }

    <T extends IClusterable> T getService(ServiceReference<T> ref) {
        List<IClusterable> list = services.get(ref.getServiceId());
        if (list.size() > 0) {
            return (T) list.get(0);
        }
        return null;
    }

    void internalRegisterService(IClusterable service, String name) {
        if (name == null) {
            throw new RuntimeException("Cannot internally register service " + service + "under a null name.");
        }

        List<IClusterable> list = services.get(name);
        if (list == null) {
            list = new LinkedList<IClusterable>();
            services.put(name, list);
        }
        list.add(service);

        List<IServiceTracker> notify = listeners.get(name);
        if (notify != null) {
            Iterator<IServiceTracker> iter = notify.iterator();
            while (iter.hasNext()) {
                IServiceTracker tracker = iter.next();
                tracker.addService(service, name);
            }
        }
    }

    void internalUnregisterService(IClusterable service, String name) {
        List<IServiceTracker> notify = listeners.get(name);
        if (notify != null) {
            Iterator<IServiceTracker> iter = notify.iterator();
            while (iter.hasNext()) {
                IServiceTracker tracker = iter.next();
                try {
                    tracker.removeService(service, name);
                } catch (Exception ex) {
                    log.error("Error while unregistering service", ex);
                }
            }
        }

        List<IClusterable> list = services.get(name);
        list.remove(service);
        if (list.isEmpty()) {
            services.remove(name);
        }
    }

    <T extends IClusterable> Map.Entry<Integer, RefCount> internalGetReference(T service) {
        final Integer ref = lookupMap.get(service);
        final RefCount refCount = referenced.get(ref);
        if (refCount == null) {
            return null;
        }
        return new Map.Entry<Integer, RefCount>() {

            public Integer getKey() {
                return ref;
            }

            public RefCount getValue() {
                return refCount;
            }

            public RefCount setValue(RefCount value) {
                return null;
            }
        };
    }

}
