/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.core.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hippoecm.frontend.core.IPluginConfig;
import org.hippoecm.frontend.core.Plugin;
import org.hippoecm.frontend.core.ServiceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginManager implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PluginManager.class);

    private PluginFactory factory;
    private Map<String, List<Serializable>> services;
    private Map<String, List<ServiceListener>> listeners;

    public PluginManager() {
        factory = new PluginFactory();
        services = new HashMap<String, List<Serializable>>();
        listeners = new HashMap<String, List<ServiceListener>>();
    }

    public Plugin start(IPluginConfig config) {
        Plugin plugin = factory.createPlugin(config);
        if (plugin != null) {
            PluginContextImpl context = new PluginContextImpl(this, config);
            plugin.start(context);
        }
        return plugin;
    }

    public void registerService(Serializable service, String name) {
        if (name == null) {
            log.error("service name is null");
            return;
        } else {
            log.info("registering " + service + " as " + name);
        }

        List<Serializable> list = services.get(name);
        if (list == null) {
            list = new LinkedList<Serializable>();
            services.put(name, list);
        }
        list.add(service);

        List<ServiceListener> notify = listeners.get(name);
        if (notify != null) {
            Iterator<ServiceListener> iter = notify.iterator();
            while (iter.hasNext()) {
                ServiceListener entry = iter.next();
                entry.processEvent(ServiceListener.ADDED, name, service);
            }
        }
    }

    public void unregisterService(Serializable service, String name) {
        if (name == null) {
            log.error("service name is null");
            return;
        } else {
            log.info("unregistering " + service + " from " + name);
        }

        List<Serializable> list = services.get(name);
        if (list != null) {
            List<ServiceListener> notify = listeners.get(name);
            if (notify != null) {
                Iterator<ServiceListener> iter = notify.iterator();
                while (iter.hasNext()) {
                    ServiceListener entry = iter.next();
                    entry.processEvent(ServiceListener.REMOVE, name, service);
                }
            }

            list.remove(service);
            if (list.isEmpty()) {
                services.remove(name);
            }
        } else {
            log.error("unregistering a service that wasn't registered.");
        }
    }

    public void registerListener(ServiceListener listener, String name) {
        if (name == null) {
            log.error("listener name is null");
            return;
        } else {
            log.info("registering listener " + listener + " for " + name);
        }

        List<ServiceListener> list = listeners.get(name);
        if (list == null) {
            list = new LinkedList<ServiceListener>();
            listeners.put(name, list);
        }
        list.add(listener);

        List<Serializable> notify = services.get(name);
        if (notify != null) {
            Iterator<Serializable> iter = notify.iterator();
            while (iter.hasNext()) {
                Serializable service = iter.next();
                listener.processEvent(ServiceListener.ADDED, name, service);
            }
        }
    }

    public void unregisterListener(ServiceListener listener, String name) {
        if (name == null) {
            log.error("listener name is null");
            return;
        } else {
            log.info("unregistering listener " + listener + " for " + name);
        }

        List<ServiceListener> list = listeners.get(name);
        if (list != null) {
            if(list.contains(listener)) {
                list.remove(listener);
            }
            if (list.isEmpty()) {
                listeners.remove(name);
            }
        } else {
            log.error("unregistering a listener that wasn't registered.");
        }
    }

}
