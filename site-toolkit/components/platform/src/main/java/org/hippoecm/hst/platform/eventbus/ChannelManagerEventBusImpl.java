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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.reflect.MethodUtils;
import org.hippoecm.hst.configuration.channel.ChannelManagerEvent;
import org.hippoecm.hst.configuration.channel.ChannelManagerEventListener;
import org.hippoecm.hst.configuration.channel.ChannelManagerEventListenerRegistry;
import org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEvent;
import org.hippoecm.hst.platform.api.ChannelManagerEventBus;
import org.hippoecm.hst.platform.model.HstModel;
import org.hippoecm.hst.platform.model.HstModelRegistry;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ChannelManagerEventBus} implementation, also implementing {@link ChannelManagerEventListenerRegistry}.
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
public class ChannelManagerEventBusImpl implements ChannelManagerEventListenerRegistry, ChannelManagerEventBus {

    private static Logger log = LoggerFactory.getLogger(ChannelManagerEventBusImpl.class);

    private volatile Map<ClassLoader, EventBusWrapper> appEventBusWrapperMap = new ConcurrentHashMap<>();

    public void init() {
        log.info("Registering ChannelManagerEventBus...");
        HippoServiceRegistry.register(this, ChannelManagerEventListenerRegistry.class);
        HippoServiceRegistry.register(this, ChannelManagerEventBus.class);
    }

    public void destroy() {
        log.info("Unregistering ChannelManagerEventBus...");
        HippoServiceRegistry.unregister(this, ChannelManagerEventListenerRegistry.class);
        HippoServiceRegistry.unregister(this, ChannelManagerEventBus.class);
    }

    @Override
    public void post(ChannelManagerEvent event, String contextPath) {
        final EventBusWrapper eventBus = getAppEventBusWrapperByContextPath(contextPath);

        if (eventBus == null) {
            log.error(
                    "Cannot deliver an event as the target website app's event bus is not found by the contextPath: '{}'.",
                    contextPath);
            return;
        }

        log.info("Posting a ChannelManagerEvent to eventBus for {}: {}", contextPath, event);
        eventBus.post(event);
    }

    @Override
    public void post(ChannelEvent event, String contextPath) {
        final EventBusWrapper eventBus = getAppEventBusWrapperByContextPath(contextPath);

        if (eventBus == null) {
            log.error(
                    "Cannot deliver an event as the target website app's event bus is not found by the contextPath: '{}'.",
                    contextPath);
            return;
        }

        log.info("Posting a ChannelEvent to eventBus for {}: {}", contextPath, event);
        eventBus.post(event);
    }

    @Override
    public void registerChannelManagerEventListener(ChannelManagerEventListener listener) {
        final EventBusWrapper eventBus = getAppEventBusWrapperByContextClassLoader(listener.getClass().getClassLoader(),
                true);
        log.info("Registering ChannelManagerEventListener: {}", listener);
        eventBus.register(listener);
    }

    @Override
    public void unregisterChannelManagerEventListener(ChannelManagerEventListener listener) {
        final EventBusWrapper eventBus = getAppEventBusWrapperByContextClassLoader(listener.getClass().getClassLoader(),
                true);
        log.info("Unregistering ChannelManagerEventListener: {}", listener);
        eventBus.unregister(listener);
    }

    @Override
    public void registerChannelEventListener(Object listener) {
        final EventBusWrapper eventBus = getAppEventBusWrapperByContextClassLoader(listener.getClass().getClassLoader(),
                true);
        log.info("Registering ChannelEventListener: {}", listener);
        eventBus.register(listener);
    }

    @Override
    public void unregisterChannelEventListener(Object listener) {
        final EventBusWrapper eventBus = getAppEventBusWrapperByContextClassLoader(listener.getClass().getClassLoader(),
                true);
        log.info("Unregistering ChannelEventListener: {}", listener);
        eventBus.unregister(listener);
    }

    private EventBusWrapper getAppEventBusWrapperByContextPath(final String contextPath) {
        final HstModel hstModel = getHstModelByContextPath(contextPath);

        if (hstModel == null) {
            log.error("Cannot deliver an event as the target website app model is not found by the contextPath: '{}'.",
                    contextPath);
            return null;
        }

        final EventBusWrapper eventBus = getAppEventBusWrapperByContextClassLoader(hstModel.getWebsiteClassLoader(),
                false);

        return eventBus;
    }

    private EventBusWrapper getAppEventBusWrapperByContextClassLoader(final ClassLoader contextClassLoader,
            final boolean create) {
        EventBusWrapper eventBusWrapper = appEventBusWrapperMap.get(contextClassLoader);

        if (eventBusWrapper == null && create) {
            synchronized (this) {
                eventBusWrapper = appEventBusWrapperMap.get(contextClassLoader);

                if (eventBusWrapper == null) {
                    eventBusWrapper = new EventBusWrapper(contextClassLoader);
                    appEventBusWrapperMap.put(contextClassLoader, eventBusWrapper);
                }
            }
        }

        return eventBusWrapper;
    }

    private HstModel getHstModelByContextPath(final String contextPath) {
        final HstModelRegistry hstModelRegistry = HippoServiceRegistry.getService(HstModelRegistry.class);
        final HstModel hstModel = hstModelRegistry.getHstModel(contextPath);
        return hstModel;
    }

    private class EventBusWrapper {

        private final ClassLoader contextClassLoader;
        private final Object eventBus;
        private final Map<Object, Boolean> subscribers = new ConcurrentHashMap<>();

        EventBusWrapper(final ClassLoader contextClassLoader) {
            this.contextClassLoader = contextClassLoader;

            try {
                eventBus = contextClassLoader.loadClass("com.google.common.eventbus.EventBus").newInstance();
            } catch (Exception e) {
                throw new IllegalStateException("Guava event bus cannot be created using website classloader.", e);
            }
        }

        void post(final Object event) {
            final ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();

            try {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
                MethodUtils.invokeMethod(eventBus, "post", event);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to post event to the internal Guava event bus.", e);
            } finally {
                Thread.currentThread().setContextClassLoader(currentLoader);
            }
        }

        synchronized void register(final Object subscriber) {
            final ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();

            try {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
                subscribers.put(subscriber, Boolean.TRUE);
                MethodUtils.invokeMethod(eventBus, "register", subscriber);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to register event listener to the internal Guava event bus.",
                        e);
            } finally {
                Thread.currentThread().setContextClassLoader(currentLoader);
            }
        }

        synchronized void unregister(final Object subscriber) {
            final ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();

            try {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
                MethodUtils.invokeMethod(eventBus, "unregister", subscriber);
                subscribers.remove(subscriber);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to unregister event listener to the internal Guava event bus.",
                        e);
            } finally {
                Thread.currentThread().setContextClassLoader(currentLoader);
            }

            // If no subscribers remain, you can de-reference it to avoid potential leaks.
            if (subscribers.isEmpty()) {
                appEventBusWrapperMap.remove(contextClassLoader);
            }
        }
    }
}
