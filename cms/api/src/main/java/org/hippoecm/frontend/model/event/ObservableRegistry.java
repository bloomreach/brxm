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
package org.hippoecm.frontend.model.event;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Page;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObservableRegistry implements IPlugin {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ObservableRegistry.class);

    @SuppressWarnings("unchecked")
    private class ObservationContext implements IObservationContext {
        private static final long serialVersionUID = 1L;

        List<IObserver> observers;
        List<IObserver> registered;
        IObservable observable;

        ObservationContext(IObservable observable) {
            this.observable = observable;
            this.observers = new ArrayList<IObserver>();
            this.registered = new ArrayList<IObserver>();
            observable.setObservationContext(this);
        }

        void addObserver(IObserver observer) {
            if (observer.getObservable() != observable) {
                observer.getObservable().setObservationContext(this);
            }
            observers.add(observer);
        }

        void removeObserver(IObserver observer) {
            observers.remove(observer);
        }

        boolean hasObservers() {
            return !observers.isEmpty();
        }

        void dispose() {
            for (IObserver observer : registered) {
                ObservableRegistry.this.removeObserver(observer);
            }
        }

        public void notifyObservers(EventCollection events) {
            for (IObserver observer : new ArrayList<IObserver>(observers)) {
                if (observers.contains(observer)) {
                    observer.onEvent(events.iterator());
                }
            }
            getPage().dirty();
        }

        public Page getPage() {
            return pluginContext.getService(Home.class.getName(), Page.class);
        }

        public void registerObserver(IObserver observer) {
            ObservableRegistry.this.addObserver(observer);
            registered.add(observer);
        }

        public void unregisterObserver(IObserver observer) {
            registered.remove(observer);
            ObservableRegistry.this.removeObserver(observer);
        }

    }

    private IPluginContext pluginContext;
    private List<ObservationContext> contexts;
    private Map<IObserver, ObservationContext> observers = new IdentityHashMap<>();

    @SuppressWarnings("unchecked")
    public ObservableRegistry(IPluginContext context, IPluginConfig config) {
        this.pluginContext = context;

        contexts = new ArrayList<ObservationContext>();

        context.registerTracker(new ServiceTracker<IObserver>(IObserver.class) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onServiceAdded(IObserver service, String name) {
                addObserver(service);
            }

            @Override
            protected void onRemoveService(IObserver service, String name) {
                removeObserver(service);
            }

        }, IObserver.class.getName());
    }

    @SuppressWarnings("unchecked")
    void addObserver(IObserver service) {
        IObservable observable = service.getObservable();
        ObservationContext obContext = getContext(observable);
        if (obContext == null) {
            obContext = new ObservationContext(observable);
            obContext.observable.startObservation();
            contexts.add(obContext);
        }
        if (observers.containsKey(service)) {
            log.warn("Same observer {} registered multiple times", service);
        } else {
            observers.put(service, obContext);
        }
        obContext.addObserver(service);
    }

    @SuppressWarnings("unchecked")
    void removeObserver(IObserver service) {
        ObservationContext obContext = observers.get(service);
        if (obContext == null) {
            log.warn("No observation context found for observer {}", service);
            return;
        }
        obContext.removeObserver(service);
        if (!obContext.observers.contains(service)) {
            observers.remove(service);
        }
        if (!obContext.hasObservers()) {
            obContext.observable.stopObservation();
            contexts.remove(obContext);
            obContext.dispose();
        }
    }

    ObservationContext getContext(IObservable observable) {
        for (ObservationContext context : contexts) {
            if (context.observable.equals(observable)) {
                return context;
            }
        }
        return null;
    }

    public void startObservation() {
        for (ObservationContext context : contexts) {
            context.observable.startObservation();
        }
    }

    public void stopObservation() {
        for (ObservationContext context : contexts) {
            context.observable.stopObservation();
        }
    }

    public void start() {
    }

    public void stop() {
    }

}
