package org.hippoecm.frontend.util;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.core.ServiceListener;

public class ServiceTracker implements ServiceListener, Serializable {
    private static final long serialVersionUID = 1L;

    private PluginContext context;
    private String name;
    private Class clazz;
    private List<Serializable> services;
    private List<IListener> listeners;

    public interface IListener extends Serializable {

        void onServiceAdded(String name, Serializable service);

        void onServiceChanged(String name, Serializable service);

        void onServiceRemoved(String name, Serializable service);

    }

    public ServiceTracker(Class clazz) {
        this.clazz = clazz;

        services = new LinkedList<Serializable>();
        listeners = new LinkedList<IListener>();
    }

    public void open(PluginContext context, String name) {
        this.context = context;
        this.name = name;
        context.registerListener(this, name);
    }

    public void close() {
        context.unregisterListener(this, name);
        services = new LinkedList<Serializable>();
    }

    public void addListener(IListener listener) {
        listeners.add(listener);
    }

    public void removeListener(IListener listener) {
        listeners.remove(listener);
    }

    public List<Serializable> getServices() {
        return services;
    }

    public final void processEvent(int type, String name, Serializable service) {
        if (this.name.equals(name) && clazz.isInstance(service)) {
            switch (type) {
            case ServiceListener.ADDED:
                services.add(service);
                for (IListener listener : listeners) {
                    listener.onServiceAdded(name, service);
                }
                break;

            case ServiceListener.CHANGED:
                for (IListener listener : listeners) {
                    listener.onServiceChanged(name, service);
                }
                break;
                
            case ServiceListener.REMOVED:
                services.remove(service);
                for (IListener listener : listeners) {
                    listener.onServiceRemoved(name, service);
                }
                break;
            }
        }
    }
}
