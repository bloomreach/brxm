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
 * {@link ChannelEventBus} implementation.
 * <p>
 * This initializes and maintains Guava event bus per site's classloader on listener registration.
 * And, it finds a proper Guava event bus by finding the {@link HstModel} by the <code>contextPath</code> of a
 * site web application and by finally finding the associated web applciation classloader.
 * <p>
 * <p>
 * There are some implementation strategies adopted here:
 * <ul>
 * <li>Guava event bus is instantiated by platform webapp's classloader to guarantee the same library usages.
 *     So, each event listener from site webapp must be annotated with <code>org.onehippo.cms7.services.eventbus.Subscribe</code>,
 *     not <code>com.google.common.eventbus.Subscribe</code>. See the JavaDoc of <code>org.onehippo.cms7.services.eventbus.Subscribe</code>
 *     for detail.
 * <li>When registering a listener, the listener's class' classloader is indicated through its {@link ServiceHolder},
 *     and used to find the internal Guava eventBus for the site webapp or find the {@link HstModel} for the site webapp.
 * </ul>
 */
public class GuavaChannelEventBus implements ChannelEventBus, ServiceTracker<Object> {

    private static Logger log = LoggerFactory.getLogger(GuavaChannelEventBus.class);

    /**
     * Internal Guava eventBus map by site webapp's classloader as keys.
     * <p>
     * Its lifecycle is bound to this bean's lifecycle which is controlled through the init method ({@link #init()})
     * and destroy method ({@link #destroy()}) in spring bean assembly.
     */
    private volatile Map<ClassLoader, EventBusWrapper> eventBusMap;

    /**
     * Internal {@link GuavaEventBusListenerProxyFactory} instance for this eventBus.
     * <p>
     * Its lifecycle is bound to this bean's lifecycle which is controlled through the init method ({@link #init()})
     * and destroy method ({@link #destroy()}) in spring bean assembly.
     */
    private GuavaEventBusListenerProxyFactory proxyFactory;

    /**
     * Initialization lifecycle method, configured as "init-method" in spring bean assembly.
     */
    public void init() {
        proxyFactory = new GuavaEventBusListenerProxyFactory();
        eventBusMap = new ConcurrentHashMap<>();
        ChannelEventListenerRegistry.get().addTracker(this);
        HippoServiceRegistry.register(this, ChannelEventBus.class);
    }

    /**
     * Destroying lifecycle method, configured as "init-method" in spring bean assembly.
     */
    public void destroy() {
        HippoServiceRegistry.unregister(this, ChannelEventBus.class);
        ChannelEventListenerRegistry.get().removeTracker(this);
        proxyFactory.clear();
        eventBusMap.values().forEach(eventBus -> eventBus.dispose());
        eventBusMap.clear();
    }

    @Override
    public void serviceRegistered(final ServiceHolder<Object> serviceHolder) {
        final GuavaEventBusListenerProxy proxy = proxyFactory.createProxy(serviceHolder);

        if (proxy != null) {
            final EventBusWrapper eventBus = getEventBusWrapperByClassLoader(serviceHolder.getClassLoader(), true);
            log.info("Registering event listener proxy for {}", serviceHolder.getServiceObject());
            eventBus.register(proxy);
        }
    }

    @Override
    public void serviceUnregistered(final ServiceHolder<Object> serviceHolder) {
        final GuavaEventBusListenerProxy proxy = proxyFactory.removeProxy(serviceHolder);

        if (proxy != null) {
            final EventBusWrapper eventBus = getEventBusWrapperByClassLoader(serviceHolder.getClassLoader(), false);

            if (eventBus == null) {
                log.warn("No event bus found so cannot unregister the proxy: {}.", proxy);
                return;
            }

            log.info("Unregistering event listener proxy for {}", serviceHolder.getServiceObject());
            eventBus.unregister(proxy);

            // If there's no subscribers remaining in the stage of site webapp's unregistering listeners,
            // then we don't need to keep the classloader and the eventBus for the site webapp any more.
            // In typical deployment scenario, it's not a big deal as all the webapps will be stopped and destroyed
            // together in most cases, almost never undeploying a webapp individually so far.
            // However, still it seems better to remove an eventBus key-value as soon as a site webapp reaches
            // unregistration phase in order to avoid any potential OOM issues by keeping the undeployed webapp's
            // classloader.
            if (eventBus.isSubscribersEmpty()) {
                eventBusMap.remove(serviceHolder.getClassLoader());
            }
        }
    }

    @Override
    public void post(BaseChannelEvent event, String contextPath) {
        final HstModelRegistry registry = HippoServiceRegistry.getService(HstModelRegistry.class);
        final HstModel model = registry.getHstModel(contextPath);
        final EventBusWrapper eventBus = getEventBusWrapperByClassLoader(model.getWebsiteClassLoader(), false);

        if (eventBus == null) {
            log.warn("Cannot post event because no event bus found for contextPath, {}: {}", contextPath, event);
            return;
        }

        log.info("Posting channel event to application ({}) event listener: {}", contextPath, event);
        eventBus.post(event);
    }

    /**
     * Return the {@link EventBusWrapper} by the given {@code classLoader} if existing.
     * If not existing and the {@code create} is true, then it creates a new instance for the {@code classLoader}.
     * @param classLoader site webapp's classloader
     * @param create whether to create a new instance or not if not found
     * @return the {@link EventBusWrapper} by the given {@code classLoader} if existing,
     *         or create a new one if {@code create} is true
     */
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

    /**
     * Internal Guava EventBus wrapper.
     * <p>
     * This wrapper provides the following additionally:
     * <ul>
     * <li><code>dispose()</code> which unregister all the subscribers.
     * <li><code>isSubscribersEmpty()</code> which returns true if the internal subscribers is empty.
     * </ul>
     */
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

        boolean isSubscribersEmpty() {
            return subscribers.isEmpty();
        }

        void dispose() {
            subscribers.forEach(subscriber -> {
                eventBus.unregister(subscriber);
            });
        }
    }
}
