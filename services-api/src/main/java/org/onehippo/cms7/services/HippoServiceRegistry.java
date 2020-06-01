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
package org.onehippo.cms7.services;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The HippoServiceRegistry is used for registration and lookup/discovery of singleton services which can be shared
 * between multiple web applications (cross-context, using different classloaders).
 * <p>
 *     The HippoServiceRegistry itself is a static singleton created in the common/shared classloader, and services
 *     can only be looked up through the service interface under which they are registered. This also implies that
 *     services registered with an interface which is <em>not</em> provided through the shared classloader will only be
 *     visible to their own web application, which can be considered a feature!
 * </p>
 * <p>
 *     A registered service will be wrapped in a dynamic proxy which only exposes the service interface, and optionally
 *     extra implemented interfaces as specified during the registration. Furthermore, all method invocations on the
 *     service proxy will temporarily set the {@link Thread#getContextClassLoader() current content classloader}
 *     to the context classloader at the time of the service registration, for cross-context/cross-classloader
 *     usages.
 * </p>
 * <p>
 *     While registration of services by a non-shared interface is supported, more practical usage use a shared interface
 *     for the registration but to also specify non-shared extra implemented interfaces. This allows using web application
 *     internal interfaces to be used to access additional methods not to be shared across web applications. The
 *     {@link #getService(Class, Class)} method can be used to automatically return a specialized service type for that
 *     purpose.
 * </p>
 * <p>
 *     An additional feature is using a {@link #addTracker(ProxiedServiceTracker, Class)} as a callback on
 *     (un)registration of a specific service by its interface. This can be used to wait for and 'chain' specific
 *     processes which depend on a service to be(come) available, or when it is removed. Note that a ProxiedServiceTracker
 *     only can be used for monitoring (un)registration of a service by its singleton interface, not any of its extra
 *     interfaces or a subtype. The {@link ProxiedServiceTracker#serviceRegistered(ProxiedServiceHolder) serviceRegistered}
 *     and {@link ProxiedServiceTracker#serviceRegistered(ProxiedServiceHolder) serviceUnregistered} callback methods
 *     provided by the service tracker will be invoked using the context classloader of the service tracker.
 * </p>
 * <p>
 *     For registration and tracking of non-singleton services by a common interface and automatic proxy wrapping, extend
 *     or use concrete implementations of a {@link WhiteboardProxiedServiceRegistry} base class instead. Or for more basic
 *     non-singleton service objects registration and tracking the {@link WhiteboardServiceRegistry} base class.
 *     Both these base classes support using the <a href="https://en.wikipedia.org/wiki/Whiteboard_Pattern">
 *     Whiteboard Pattern</a>for decoupled lookup/wiring of multiple service objects by a common interface or common base
 *     type (class or interface) respectively. Example implementation and usages of these are the
 *     {@link org.onehippo.repository.events.PersistedHippoEventListenerRegistry PersistedHippoEventListenerRegistry}
 *     from the hippo-repository-api module and the {@link org.onehippo.cms7.services.eventbus.HippoEventListenerRegistry
 *     HippoEventListenerRegistry}
 * </p>
 */
public final class HippoServiceRegistry {

    // Use ConcurrentHashMap to make sure newly registered services and trackers are immediately visible.
    // HippoServiceRegistry public methods are synchronized but the underlying maps also must be concurrent to ensure
    // newly registered services and trackers are immediately visible to other threads. (see: CMS-8998)
    private static final ConcurrentHashMap<Class<?>, ProxiedServiceHolder> services = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, List<ServiceHolder<ProxiedServiceTracker>>> trackers = new ConcurrentHashMap<>();

    private HippoServiceRegistry() {
    }

    /**
     * Register a service object as a singleton service of a certain interface type.
     * <p>
     *     The service object will be proxied to expose only the service interface, and optionally extra interfaces, and
     *     to enforce setting the Thread ContextClassLoader to its registration classloader during invocation of its
     *     service (interface) methods.
     * </p>
     * <p>
     *     Additional interfaces can be specified by the extraInterfaces parameter to be also exposed through the proxy.
     *     See the class level documentation for usages and common use-cases.
     * </p>
     * @param serviceObject service object to register
     * @param serviceInterface the service interface to implement and lookup the service
     * @param extraInterfaces optional extra interfaces to proxy for the service,
     * @throws HippoServiceException when the service object was already registered
     */
    @SuppressWarnings("unchecked")
    public synchronized static <T> void register(final T serviceObject, final Class<T> serviceInterface,
                                                 final Class<?> ... extraInterfaces) {
        Objects.requireNonNull(serviceObject, "serviceObject must not be null");
        Objects.requireNonNull(serviceInterface, "serviceInterface must not be null");
        if (services.containsKey(serviceInterface)) {
            throw new HippoServiceException("A service of type "+serviceInterface.getName()+" is already registered.");
        }
        final ProxiedServiceHolder<T> serviceHolder =
                new ProxiedServiceHolder<>(serviceObject, serviceInterface, extraInterfaces);
        services.put(serviceInterface, serviceHolder);
        final List<ServiceHolder<ProxiedServiceTracker>> trackersList = trackers.get(serviceInterface);
        if (trackersList != null) {
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {
                for (final ServiceHolder<ProxiedServiceTracker> trackerHolder : trackersList) {
                    Thread.currentThread().setContextClassLoader(trackerHolder.getClassLoader());
                    trackerHolder.getServiceObject().serviceRegistered(serviceHolder);
                }
            } finally {
                Thread.currentThread().setContextClassLoader(cl);
            }
        }
    }

    /**
     * @deprecated since v13.0. Use {@link #register(Object, Class, Class[])} instead.
     */
    @Deprecated
    public synchronized static <T> void registerService(final T serviceObject, final Class<T> serviceInterface) {
        register(serviceObject, serviceInterface);
    }

    /**
     * Unregister a previously registered service object
     * @param serviceObject the service interface under which the service was registered
     * @return true if the service object was registered before and now removed, false otherwise
     */
    @SuppressWarnings("unchecked")
    public synchronized static <T> boolean unregister(final T serviceObject, final Class<T> serviceInterface) {
        Objects.requireNonNull(serviceObject, "serviceObject must not be null");
        Objects.requireNonNull(serviceInterface, "serviceInterface must not be null");
        final ProxiedServiceHolder serviceHolder = services.get(serviceInterface);
        if (serviceHolder != null && serviceHolder.getServiceObject() == serviceObject) {
            services.remove(serviceInterface);
            final List<ServiceHolder<ProxiedServiceTracker>> trackersList = trackers.get(serviceInterface);
            if (trackersList != null) {
                final ClassLoader cl = Thread.currentThread().getContextClassLoader();
                try {
                    for (final ServiceHolder<ProxiedServiceTracker> trackerHolder : trackersList) {
                        Thread.currentThread().setContextClassLoader(trackerHolder.getClassLoader());
                        trackerHolder.getServiceObject().serviceUnregistered(serviceHolder);
                    }
                } finally {
                    Thread.currentThread().setContextClassLoader(cl);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * @deprecated since v13.0. Use {@link #unregister(Object, Class)} instead.
     */
    @Deprecated
    public synchronized static <T> void unregisterService(final T serviceObject, final Class<T> serviceInterface) {
        unregister(serviceObject, serviceInterface);
    }

    /**
     * Lookup a service (proxy) by its (main) service interface.
     * @param serviceInterface the service interface under which the service was registered
     * @return the service proxy or null if not found
     */
    @SuppressWarnings("unchecked")
    public static <T> T getService(final Class<T> serviceInterface) {
        Objects.requireNonNull(serviceInterface, "serviceInterface must not be null");
        final ProxiedServiceHolder serviceHolder = services.get(serviceInterface);
        if (serviceHolder != null) {
            return (T)serviceHolder.getServiceProxy();
        }
        return null;
    }

    /**
     * Lookup a service (proxy) by its (main) service interface and cast it to one of the extra interfaces exposed
     * by the proxy.
     * @param serviceInterface the service interface under which the service was registered
     * @param extraInterface the extra interface exposed by the proxy to be used as return type
     * @return the service proxy casted to the extra interface, or null if the service either was not found or doesn't
     * implement the extra interface to be casted to
     */
    @SuppressWarnings("unchecked")
    public static <T> T getService(final Class<?> serviceInterface, final Class<T> extraInterface) {
        Objects.requireNonNull(serviceInterface, "serviceInterface must not be null");
        Objects.requireNonNull(extraInterface, "extraInterface must not be null");
        final ProxiedServiceHolder serviceHolder = services.get(serviceInterface);
        if (serviceHolder != null && extraInterface.isInstance(serviceHolder.getServiceProxy())) {
            return (T)serviceHolder.getServiceProxy();
        }
        return null;
    }

    /**
     * Add a {@link ProxiedServiceTracker service tracker} for tracking a service (to be) registered with a specific service
     * interface. Multiple trackers may be added to track (un)registration of the same service interface.
     * @param tracker the service tracker
     * @throws HippoServiceException when the provided tracker instance already was added before for tracking the
     * specific service interface
     */
    @SuppressWarnings("unchecked")
    public synchronized static <T> void addTracker(final ProxiedServiceTracker<T> tracker, final Class<T> serviceInterface) {
        Objects.requireNonNull(tracker, "tracker must not be null");
        Objects.requireNonNull(serviceInterface, "serviceInterface must not be null");
        final List<ServiceHolder<ProxiedServiceTracker>> trackersList = trackers.computeIfAbsent(serviceInterface, k -> new ArrayList<>());
        for (final ServiceHolder<ProxiedServiceTracker> trackerHolder : trackersList) {
            if (trackerHolder.getServiceObject() == tracker) {
                throw new HippoServiceException("tracker already added for service interface "+serviceInterface.getName());
            }
        }
        final ServiceHolder<ProxiedServiceTracker> trackerHolder = new ServiceHolder<>(tracker);
        trackersList.add(trackerHolder);
        final ProxiedServiceHolder<T> serviceHolder = (ProxiedServiceHolder<T>)services.get(serviceInterface);
        if (serviceHolder != null) {
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(trackerHolder.getClassLoader());
                tracker.serviceRegistered(serviceHolder);
            } finally {
                Thread.currentThread().setContextClassLoader(cl);
            }
        }
    }

    /**
     * Remove a previously added {@link ProxiedServiceTracker ProxiedServiceTracker&lt;T&gt;}.
     * @param tracker the service tracker
     * @return true if the service tracker was added before and now removed, false otherwise
     */
    public synchronized static <T> boolean removeTracker(final ProxiedServiceTracker<T> tracker, final Class<T> serviceInterface) {
        Objects.requireNonNull(tracker, "tracker must not be null");
        Objects.requireNonNull(serviceInterface, "serviceInterface must not be null");
        final List<ServiceHolder<ProxiedServiceTracker>> trackersList = trackers.get(serviceInterface);
        if (trackersList != null) {
            for (final Iterator<ServiceHolder<ProxiedServiceTracker>> iterator = trackersList.iterator(); iterator.hasNext(); ) {
                if (iterator.next().getServiceObject() == tracker) {
                    iterator.remove();
                    return true;
                }
            }
        }
        return false;
    }
}
