package org.hippoecm.frontend.util;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.core.ServiceListener;

public class ServiceTracker<S extends Serializable> implements ServiceListener, Serializable {
    private static final long serialVersionUID = 1L;

    private PluginContext context;
    private String name;
    private Class clazz;
    private List<S> services;
    private List<IListener<S>> listeners;

    public interface IListener<S extends Serializable> extends Serializable {

        void onServiceAdded(String name, S service);

        void onServiceChanged(String name, S service);

        void onRemoveService(String name, S service);

    }

    public ServiceTracker(Class clazz) {
        this.clazz = clazz;

        services = new LinkedList<S>();
        listeners = new LinkedList<IListener<S>>();
    }

    public void open(PluginContext context, String name) {
        this.context = context;
        this.name = name;
        context.registerListener(this, name);
    }

    public void close() {
        context.unregisterListener(this, name);
        services = new LinkedList<S>();
    }

    public void addListener(IListener listener) {
        listeners.add(listener);
    }

    public void removeListener(IListener listener) {
        listeners.remove(listener);
    }

    public List<S> getServices() {
        return services;
    }

    public final void processEvent(int type, String name, Serializable service) {
        if (this.name.equals(name) && clazz.isInstance(service)) {
            S casted = (S) service;
            switch (type) {
            case ServiceListener.ADDED:
                services.add(casted);
                for (IListener listener : listeners) {
                    listener.onServiceAdded(name, casted);
                }
                break;

            case ServiceListener.CHANGED:
                for (IListener listener : listeners) {
                    listener.onServiceChanged(name, casted);
                }
                break;
                
            case ServiceListener.REMOVE:
                services.remove(service);
                for (IListener listener : listeners) {
                    listener.onRemoveService(name, casted);
                }
                break;
            }
        }
    }
}
