/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.eventbus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.eventbus.AsyncEventBus;

import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.HippoServiceRegistration;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuavaHippoEventBus implements HippoEventBus {

    private static final Logger log = LoggerFactory.getLogger(GuavaHippoEventBus.class);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AsyncEventBus eventBus = new AsyncEventBus(executor);

    private final List<HippoServiceRegistration> listeners = Collections.synchronizedList(new ArrayList<HippoServiceRegistration>());
    private final GuavaEventBusListenerProxyFactory proxyFactory = new GuavaEventBusListenerProxyFactory();

    private volatile int version = -1;
    
    public void destroy() {
        for (GuavaEventBusListenerProxy proxy : proxyFactory.clear()) {
            eventBus.unregister(proxy);
        }
        listeners.clear();
        executor.shutdown();
    }

    @Override
    public void register(final Object listener) {
        registerProxy(listener);
        log.warn("HippoEventBus method #register is deprecated, use whiteboard pattern instead");
    }

    @Override
    public void unregister(final Object listener) {
        unregisterProxy(listener);
    }

    public void post(final Object event) {
        if (version != HippoServiceRegistry.getVersion()) {
            updateListeners();
        }
        if (event instanceof HippoEvent) {
            ((HippoEvent) event).sealEvent();
        }
        eventBus.post(event);
    }

    protected void unregisterProxy(final Object listener) {

        GuavaEventBusListenerProxy proxy = proxyFactory.removeProxy(listener);
        if (proxy != null) {
            eventBus.unregister(proxy);
        }
    }

    protected void registerProxy(final Object listener) {
        GuavaEventBusListenerProxy proxy = proxyFactory.createProxy(listener);
        if (proxy != null) {
            eventBus.register(proxy);
        }
    }

    private void updateListeners() {
        List<HippoServiceRegistration> registered = HippoServiceRegistry.getRegistrations(HippoEventBus.class);
        for (HippoServiceRegistration registration : registered) {
            if (!listeners.contains(registration)) {
                listeners.add(registration);
                registerProxy(registration);
            }
        }
        Iterator<HippoServiceRegistration> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            HippoServiceRegistration registration = iterator.next();
            if (!registered.contains(registration)) {
                iterator.remove();
                unregisterProxy(registration);
            }
        }
        version = HippoServiceRegistry.getVersion();
    }
}
