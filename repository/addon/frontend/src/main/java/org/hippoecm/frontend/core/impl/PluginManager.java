package org.hippoecm.frontend.core.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hippoecm.frontend.core.Plugin;
import org.hippoecm.frontend.core.PluginConfig;
import org.hippoecm.frontend.core.ServiceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginManager implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PluginManager.class);

    private static class ListenerEntry implements Serializable {
        private static final long serialVersionUID = 1L;

        ServiceListener listener;
        PluginConfig config;

        ListenerEntry(PluginConfig config, ServiceListener listener) {
            this.config = config;
            this.listener = listener;
        }
    }

    private PluginFactory factory;
    private Map<String, List<Serializable>> services;
    private Map<String, List<ListenerEntry>> listeners;

    public PluginManager() {
        factory = new PluginFactory();
        services = new HashMap<String, List<Serializable>>();
        listeners = new HashMap<String, List<ListenerEntry>>();
    }

    public Plugin start(PluginConfig config) {
        Plugin plugin = factory.createPlugin(config);
        if (plugin != null) {
            PluginContextImpl context = new PluginContextImpl(this, config);
            plugin.start(context);
        }
        return plugin;
    }

    public List<Serializable> getServices(String name) {
        return services.get(name);
    }

    public void registerService(Serializable service, String name) {
        List<Serializable> list = services.get(name);
        if (list == null) {
            list = new LinkedList<Serializable>();
            services.put(name, list);

            List<ListenerEntry> notify = listeners.get(name);
            if (notify != null) {
                Iterator<ListenerEntry> iter = notify.iterator();
                while (iter.hasNext()) {
                    ListenerEntry entry = iter.next();
                    String localName = entry.config.resolve(name);
                    entry.listener.processEvent(ServiceListener.ADDED, localName, service);
                }
            }
        }
        list.add(service);
    }

    public void unregisterService(Serializable service, String name) {
        List<Serializable> list = services.get(name);
        if (list != null) {
            // TODO Auto-generated method stub
            list.remove(service);
            if (list.isEmpty()) {
                services.put(name, null);
            }

            List<ListenerEntry> notify = listeners.get(name);
            if (notify != null) {
                Iterator<ListenerEntry> iter = notify.iterator();
                while (iter.hasNext()) {
                    ListenerEntry entry = iter.next();
                    String localName = entry.config.resolve(name);
                    entry.listener.processEvent(ServiceListener.REMOVED, localName, service);
                }
            }
        } else {
            log.error("unregistering a service that wasn't registered.");
        }
    }

    public void registerListener(PluginConfig config, ServiceListener listener, String name) {
        List<ListenerEntry> list = listeners.get(name);
        if (list == null) {
            list = new LinkedList<ListenerEntry>();
            listeners.put(name, list);
        }
        list.add(new ListenerEntry(config, listener));

        String localName = config.resolve(name);
        List<Serializable> notify = services.get(name);
        if (notify != null) {
            Iterator<Serializable> iter = notify.iterator();
            while (iter.hasNext()) {
                Serializable service = iter.next();
                listener.processEvent(ServiceListener.ADDED, localName, service);
            }
        }
    }

    public void unregisterListener(ServiceListener listener, String name) {
        List<ListenerEntry> list = listeners.get(name);
        if (list != null) {
            Iterator<ListenerEntry> iter = list.iterator();
            while (iter.hasNext()) {
                ListenerEntry entry = iter.next();
                if (entry.listener == listener) {
                    iter.remove();
                    break;
                }
            }
            if (list.isEmpty()) {
                listeners.put(name, null);
            }
        } else {
            log.error("unregistering a listener that wasn't registered.");
        }
    }
}
