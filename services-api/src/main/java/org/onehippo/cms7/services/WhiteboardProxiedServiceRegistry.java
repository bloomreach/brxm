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
package org.onehippo.cms7.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.onehippo.cms7.util.ObjectIdentityKey;

/**
 * WhiteboardProxiedServiceRegistry&lt;T&gt; is an abstract base class for implementing and using the
 * <a href="https://en.wikipedia.org/wiki/Whiteboard_Pattern">Whiteboard Pattern</a>for decoupled lookup/wiring of
 * multiple service objects by a common (main) interface.
 * <p>
 *     This WhiteboardProxiedServiceRegistry base class supports (un)registering service objects, and
 *     {@link ProxiedServiceTracker}s to retrieve those currently registered as well as be notified on future
 *     (un)registrations. Also currently registered service entries can be retrieved through {@link #getEntries()}.
 * </p>
 * <p>
 *     All service objects registered through a WhiteboardProxiedServiceRegistry implementation must implement a common
 *     interface of generic type T.
 * </p>
 * <p>
 *     For example with the following (complete) {@link org.onehippo.repository.events.PersistedHippoEventListenerRegistry
 *     PersistedHippoEventListenerRegistry} implementation from the hippo-repository-api module using interface type
 *     {@link org.onehippo.repository.events.PersistedHippoEventListener PersistedHippoEventListener}, service objects
 *     implementing that interface can be registered:
 *     <pre><code>
 *     public final class PersistedHippoEventListenerRegistry extends WhiteboardProxiedServiceRegistry&lt;PersistedHippoEventListener&gt; {
 *
 *         private static final PersistedHippoEventListenerRegistry INSTANCE = new PersistedHippoEventListenerRegistry();
 *
 *         private HippoEventListenerRegistry() {
 *             super(PersistedHippoEventListener.class);
 *         }
 *
 *         public static PersistedHippoEventListenerRegistry get() { return INSTANCE; }
 *     }</code></pre>
 * </p>
 * <p>
 *     For a typical usage of a WhiteboardProxiedServiceRegistry see the javadoc for the
 *     {@link org.onehippo.repository.events.PersistedHippoEventListenerRegistry PersistedHippoEventListenerRegistry} and
 *     {@link org.onehippo.repository.events.PersistedHippoEventListener PersistedHippoEventListener} service interface
 *     in the hippo-repository-api module.
 * </p>
 * <p>
 *     For each registered service object a {@link ProxiedServiceHolder} is created and passed on to
 *     the {@link ProxiedServiceTracker}(s), storing the service object itself together with the context classloader used to
 *     register the service object. Furthermore it provides access to a dynamically created
 *     {@link ProxiedServiceHolder#getServiceProxy() service proxy} for the service exposing only the service interface
 *     and optionally extra interfaces. When using the service proxy all method invocation will be executed with the
 *     context classloader set to its registration classloader. Also, the
 *     {@link ProxiedServiceTracker#serviceRegistered(ProxiedServiceHolder) serviceRegistered} and
 *     {@link ProxiedServiceTracker#serviceRegistered(ProxiedServiceHolder) serviceUnregistered} callback methods
 *     provided by the service tracker will be invoked using the context classloader of the service tracker.
 * </p>
 * <p>
 *     When a service object should be used as a singleton then use {@link HippoServiceRegistry} instead.
 * </p>
 * <p>
 *     When only the registration and tracking of one or more service objects of a certain (base) type (class or interface)
 *     is needed nor a service proxy with automatic enforcing of its context classloader during method invocation, use the
 *     {@link WhiteboardServiceRegistry WhiteboardServiceRegistry&lt;T&gt;} base class instead, Note that
 *     it then requires <em>manual</em> enforcement of the current context classloader in case of cross-context usage of
 *     the service objects.
 * </p>
 * @param <T> service interface type
 */
public abstract class WhiteboardProxiedServiceRegistry<T> {

    private final Class<T> serviceInterface;
    private final ConcurrentHashMap<ObjectIdentityKey, ProxiedServiceHolder<T>> services = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ObjectIdentityKey, ServiceHolder<ProxiedServiceTracker>> trackers = new ConcurrentHashMap<>();

    /**
     * @return a new copied List of current service entries to guard against potential concurrent service (un)registrations
     */
    protected synchronized List<ProxiedServiceHolder<T>> getEntriesList() {
        return new ArrayList<>(services.values());
    }

    /**
     * Constructor needed for storing the required common interface of generic type T which must be implemented by all
     * service objects registered.
     * @param serviceInterface common interface
     */
    public WhiteboardProxiedServiceRegistry(final Class<T> serviceInterface) {
        this.serviceInterface = serviceInterface;
        if (!serviceInterface.isInterface()) {
            throw new IllegalArgumentException("serviceInterface "+serviceInterface.getName()+" is not an interface");
        }
    }

    /**
     * Register a service object implementing interface type &lt;T&gt; and optionally some additional interfaces which
     * all will be exposed by the {@link ProxiedServiceHolder#getServiceProxy() service proxy} created for this service object.
     * @param serviceObject the service object
     * @param extraInterfaces additional interfaces implemented by the service object to be exposed by the service proxy
     * @throws HippoServiceException when the service object was already registered
     */
    @SuppressWarnings("unchecked")
    public synchronized void register(final T serviceObject, final Class<?> ... extraInterfaces) {
        Objects.requireNonNull(serviceObject, "serviceObject must not be null");
        final ObjectIdentityKey key = new ObjectIdentityKey(serviceObject);
        if (services.containsKey(key)) {
            throw new HippoServiceException("serviceObject already registered");
        }
        final ProxiedServiceHolder<T> serviceHolder =
                new ProxiedServiceHolder<>(serviceObject, serviceInterface, extraInterfaces);

        services.put(key, serviceHolder);
        if (!trackers.isEmpty()) {
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {
                for (final ServiceHolder<ProxiedServiceTracker> trackerHolder : trackers.values()) {
                    Thread.currentThread().setContextClassLoader(trackerHolder.getClassLoader());
                    trackerHolder.getServiceObject().serviceRegistered(serviceHolder);
                }
            } finally {
                Thread.currentThread().setContextClassLoader(cl);
            }
        }
    }

    /**
     * Unregister a previously registered service object
     * @param serviceObject the service object
     * @return true if the service object was registered before and now removed, false otherwise
     */
    @SuppressWarnings("unchecked")
    public synchronized boolean unregister(final T serviceObject) {
        Objects.requireNonNull(serviceObject, "serviceObject must not be null");
        final ProxiedServiceHolder<T> serviceHolder = services.remove(new ObjectIdentityKey(serviceObject));
        if (serviceHolder != null && !trackers.isEmpty()) {
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {
                for (final ServiceHolder<ProxiedServiceTracker> trackerHolder : trackers.values()) {
                    Thread.currentThread().setContextClassLoader(trackerHolder.getClassLoader());
                    trackerHolder.getServiceObject().serviceUnregistered(serviceHolder);
                }
            } finally {
                Thread.currentThread().setContextClassLoader(cl);
            }
        }
        return serviceHolder != null;
    }

    /**
     * Add a {@link ProxiedServiceTracker ProxiedServiceTracker&lt;T&gt;} for tracking service objects of common
     * interface &lt;T&gt; {@link #register(Object, Class[]) registered} and {@link #unregister(Object) unregistered}
     * in this registry.
     * @param tracker the service tracker
     * @throws HippoServiceException when the provided tracker instance already was added before
     */
    public synchronized void addTracker(final ProxiedServiceTracker<T> tracker) {
        Objects.requireNonNull(tracker, "tracker must not be null");
        final ObjectIdentityKey key = new ObjectIdentityKey(tracker);
        if (trackers.containsKey(key)) {
            throw new HippoServiceException("tracker already added");
        }
        final ServiceHolder<ProxiedServiceTracker> trackerHolder = new ServiceHolder<>(tracker);
        trackers.put(key, trackerHolder);
        for (ProxiedServiceHolder<T> serviceHolder: services.values()) {
            tracker.serviceRegistered(serviceHolder);
        }
    }

    /**
     * Remove a previously added {@link ProxiedServiceTracker ProxiedServiceTracker&lt;T&gt;}.
     * @param tracker the service tracker
     * @return true if the service tracker was added before and now removed, false otherwise
     */
    public synchronized boolean removeTracker(final ProxiedServiceTracker<T> tracker) {
        Objects.requireNonNull(tracker, "tracker must not be null");
        return trackers.remove(new ObjectIdentityKey(tracker)) != null;
    }

    /**
     * @return the registered service entries
     */
    public Stream<ProxiedServiceHolder<T>> getEntries() {
        return getEntriesList().stream();
    }

    /**
     * @return the registered services (proxied)
     */
    public Stream<T> getServices() {
        return getEntriesList().stream().map(ProxiedServiceHolder::getServiceProxy);
    }

    /**
     * @return the number of registered services
     */
    public synchronized int size() {
        return services.size();
    }
}
