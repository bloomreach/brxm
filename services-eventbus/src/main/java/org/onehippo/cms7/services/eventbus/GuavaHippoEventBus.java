/*
 *  Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.eventbus.AsyncEventBus;

import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.ServiceHolder;
import org.onehippo.cms7.services.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuavaHippoEventBus implements HippoEventBus, ServiceTracker<Object> {

    private static final Logger log = LoggerFactory.getLogger(GuavaHippoEventBus.class);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AsyncEventBus eventBus = new AsyncEventBus(executor);

    private final GuavaEventBusListenerProxyFactory proxyFactory = new GuavaEventBusListenerProxyFactory();

    public GuavaHippoEventBus() {
        HippoEventListenerRegistry.get().addTracker(this);
    }

    @Override
    public void serviceRegistered(final ServiceHolder<Object> serviceHolder) {
        GuavaEventBusListenerProxy proxy = proxyFactory.createProxy(serviceHolder);
        if (proxy != null) {
            eventBus.register(proxy);
        }
    }

    @Override
    public void serviceUnregistered(final ServiceHolder<Object> serviceHolder) {
        GuavaEventBusListenerProxy proxy = proxyFactory.removeProxy(serviceHolder);
        if (proxy != null) {
            eventBus.unregister(proxy);
        }
    }

    public void destroy() {
        for (GuavaEventBusListenerProxy proxy : proxyFactory.clear()) {
            eventBus.unregister(proxy);
        }
        executor.shutdown();
    }

    public void post(final Object event) {
        if (event instanceof HippoEvent) {
            ((HippoEvent) event).sealEvent();
        }
        eventBus.post(event);
    }
}
