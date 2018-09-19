/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.eventbus;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.hippoecm.hst.configuration.channel.ChannelEventListenerRegistry;
import org.hippoecm.hst.configuration.channel.ChannelManagerEventListenerRegistry;
import org.hippoecm.hst.pagecomposer.jaxrs.api.BaseChannelEvent;
import org.hippoecm.hst.platform.api.ChannelEventBus;
import org.hippoecm.hst.platform.model.HstModel;
import org.hippoecm.hst.platform.model.HstModelRegistry;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.ServiceHolder;
import org.onehippo.cms7.services.ServiceTracker;
import org.onehippo.cms7.services.eventbus.GuavaEventBusListenerProxy;
import org.onehippo.cms7.services.eventbus.GuavaEventBusListenerProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

/**
 * {@link ChannelEventBus} implementation, also implementing {@link ChannelManagerEventListenerRegistry}.
 * <p>
 * This initializes and maintains Guava event bus per site's classloader on listener registration.
 * And, it finds a proper Guava event bus by finding the {@link HstModel} by the <code>contextPath</code> of a
 * site web application and by finally finding the associated web applciation classloader.
 * <p>
 * <p>
 * There are some implementation strategies adopted here:
 * <ul>
 * <li>Guava event bus is instantiated by site webapp's classloader as its listener depends on its own Guave library.
 *     So, each event bus instance may scan annotations of the listener class loaded by its classloader.
 * <li>When registering a listener, the listener's class' classloader is assumed to be the same as its site webapp's classloader.
 * <li>As Guava event bus is instantiated by each site webapp's classloader, Java reflection was used whenever
 *     invoking on the specific Guava event bus.
 * <li>To avoid any potential application specific errors from the current thread's context classloader that could
 *     be referred by the specific application code, whenever invoking on a specific Guava event bus, the current
 *     context classloader is switched to the classloader of the Guava event bus.
 * </ul>
 */
public class GuavaChannelEventBus implements ChannelEventBus, ServiceTracker<Object> {

    private static Logger log = LoggerFactory.getLogger(GuavaChannelEventBus.class);

    private volatile Map<ClassLoader, EventBusWrapper> eventBusMap = new ConcurrentHashMap<>();

    private final GuavaEventBusListenerProxyFactory proxyFactory = new GuavaEventBusListenerProxyFactory();

    public void init() {
        HippoServiceRegistry.register(this, ChannelEventBus.class);
        ChannelEventListenerRegistry.get().addTracker(this);
    }

    public void destroy() {
        proxyFactory.clear();
        eventBusMap.values().forEach(eventBus -> eventBus.dispose());
        ChannelEventListenerRegistry.get().removeTracker(this);
        HippoServiceRegistry.unregister(this, ChannelEventBus.class);
    }

    @Override
    public void serviceRegistered(final ServiceHolder<Object> serviceHolder) {
        GuavaEventBusListenerProxy proxy = proxyFactory.createProxy(serviceHolder);

        if (proxy != null) {
            final EventBusWrapper eventBus = getEventBusWrapperByClassLoader(serviceHolder.getClassLoader(), true);
            log.info("Registering event listener proxy for {}", serviceHolder.getServiceObject());
            eventBus.register(proxy);
        }
    }

    @Override
    public void serviceUnregistered(final ServiceHolder<Object> serviceHolder) {
        GuavaEventBusListenerProxy proxy = proxyFactory.removeProxy(serviceHolder);

        if (proxy != null) {
            final EventBusWrapper eventBus = getEventBusWrapperByClassLoader(serviceHolder.getClassLoader(), false);

            if (eventBus != null) {
                log.info("Unregistering event listener proxy for {}", serviceHolder.getServiceObject());
                eventBus.unregister(proxy);
            }
        }
    }

    @Override
    public void post(BaseChannelEvent event, String contextPath) {
        final HstModelRegistry registry = HippoServiceRegistry.getService(HstModelRegistry.class);
        final HstModel model = registry.getHstModel(contextPath);
        final EventBusWrapper eventBus = getEventBusWrapperByClassLoader(model.getWebsiteClassLoader(), false);

        if (eventBus == null) {
            log.error("No event bus found for contextPath: {}.", contextPath);
        }

        log.info("Posting channel event to application ({}) event listener: {}", contextPath, event);
        eventBus.post(event);
    }

    private EventBusWrapper getEventBusWrapperByClassLoader(final ClassLoader classLoader, final boolean create) {
        EventBusWrapper eventBus = eventBusMap.get(classLoader);

        if (eventBus == null && create) {
            synchronized (this) {
                eventBus = eventBusMap.get(classLoader);

                if (eventBus == null) {
                    eventBus = new EventBusWrapper();
                    eventBusMap.put(classLoader, eventBus);
                }
            }
        }

        return eventBus;
    }

    static class EventBusWrapper {

        private final EventBus eventBus = new EventBus();
        private final Set<Object> subscribers = Collections.synchronizedSet(new HashSet<>());

        void register(Object subscriber) {
            subscribers.add(subscriber);
            eventBus.register(subscriber);
        }

        void unregister(Object subscriber) {
            eventBus.unregister(subscriber);
            subscribers.remove(subscriber);
        }

        void post(Object event) {
            eventBus.post(event);
        }

        void dispose() {
            subscribers.forEach(subscriber -> {
                eventBus.unregister(subscriber);
            });
        }
    }
}
