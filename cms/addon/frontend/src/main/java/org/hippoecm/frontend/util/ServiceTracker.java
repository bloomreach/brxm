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
package org.hippoecm.frontend.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.hippoecm.frontend.sa.core.IPluginContext;
import org.hippoecm.frontend.sa.core.IServiceListener;
import org.hippoecm.frontend.sa.core.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceTracker<S extends Serializable> implements IServiceListener, Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ServiceTracker.class);

    private IPluginContext context;
    private String name;
    private Class clazz;
    private List<ServiceReference<S>> services;
    private List<IListener<S>> listeners;

    public interface IListener<S extends Serializable> extends Serializable {

        void onServiceAdded(String name, S service);

        void onServiceChanged(String name, S service);

        void onRemoveService(String name, S service);

    }

    public ServiceTracker(Class clazz) {
        this.clazz = clazz;

        services = new LinkedList<ServiceReference<S>>();
        listeners = new LinkedList<IListener<S>>();
    }

    public void open(IPluginContext context, String name) {
        this.context = context;
        this.name = name;
        context.registerListener(this, name);
    }

    public void close() {
        if (context != null) {
            context.unregisterListener(this, name);
            services = new LinkedList<ServiceReference<S>>();
            context = null;
        } else {
            log.warn("Wasn't open");
        }
    }

    public void addListener(IListener listener) {
        listeners.add(listener);
    }

    public void removeListener(IListener listener) {
        listeners.remove(listener);
    }

    public S getService() {
        if (services.size() > 0) {
            return services.get(0).getService();
        }
        return null;
    }

    public List<S> getServices() {
        List<S> result = new ArrayList<S>(services.size());
        for (ServiceReference<S> reference : services) {
            result.add(reference.getService());
        }
        return result;
    }

    public final void processEvent(int type, String name, Serializable service) {
        if (this.name.equals(name) && clazz.isInstance(service)) {
            S casted = (S) service;
            switch (type) {
            case IServiceListener.ADDED:
                services.add(context.getReference(casted));
                for (IListener listener : listeners) {
                    listener.onServiceAdded(name, casted);
                }
                break;

            case IServiceListener.CHANGED:
                for (IListener listener : listeners) {
                    listener.onServiceChanged(name, casted);
                }
                break;

            case IServiceListener.REMOVE:
                services.remove(context.getReference(service));
                for (IListener listener : listeners) {
                    listener.onRemoveService(name, casted);
                }
                break;
            }
        }
    }
}
