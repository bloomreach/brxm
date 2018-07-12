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
 * WhiteboardServiceRegistry&lt;T&gt; is an abstract base class for implementing and using the
 * <a href="https://en.wikipedia.org/wiki/Whiteboard_Pattern">Whiteboard Pattern</a>for decoupled lookup/wiring of
 * multiple service objects by a common (base) type, which can be a class or interface.
 * <p>
 *     This WhiteboardServiceRegistry base class supports (un)registering service <em>objects</em>,
 *     and {@link ServiceTracker}s to retrieve those currently registered as well as be notified on future
 *     (un)registrations. Currently registered service object holders also can be retrieved through
 *     {@link #getEntries()}.
 * </p>
 * <p>
 *     All service objects registered through a WhiteboardServiceRegistry implementation need to be an instanceof
 *     of a common class or interface type T.
 * </p>
 * <p>
 *     For example with the following (complete) {@link org.onehippo.cms7.services.eventbus.HippoEventListenerRegistry
 *     HippoEventListenerRegistry} implementation, using implementation type Object, effectively all possible service
 *     objects can be registered:
 *     <pre><code>
 *     public final class HippoEventListenerRegistry extends WhiteboardServiceRegistry&lt;Object&gt; {

 *         private static final HippoEventListenerRegistry INSTANCE = new HippoEventListenerRegistry();

 *         private HippoEventListenerRegistry() {}

 *         public static HippoEventListenerRegistry get() { return INSTANCE; }
 *     }</code></pre>
 * </p>
 * <p>
 *     For a typical usage of a WhiteboardServiceRegistry see the javadoc for the
 *     {@link org.onehippo.cms7.services.eventbus.HippoEventListenerRegistry HippoEventListenerRegistry} and
 *     {@link org.onehippo.cms7.services.eventbus.HippoEventBus HippoEventBus} service interface.
 * </p>
 * <p>
 *     A more restricted variation is the PersistedHippoEventListenerRegistry in the hippo-repository-api module
 *     which is restricted to registering service objects implementing the PersistedHippoEventListener interface.
 * </p>
 * <p>
 *     For each registered service object a {@link ServiceHolder} is created and passed on to
 *     the {@link ServiceTracker}(s) (if any), which is just holding the service object itself together with the context
 *     classloader used to register the object. The {@link ServiceTracker#serviceRegistered(ServiceHolder) serviceRegistered}
 *  *     and {@link ServiceTracker#serviceRegistered(ServiceHolder) serviceUnregistered} callback methods
 *  *     provided by the service tracker will be invoked using the context classloader of the service tracker.
 * </p>
 * <p>
 *     When one or more service objects (only) should be exposed through a common service interface (and optionally
 *     additional interfaces), use the {@link WhiteboardProxiedServiceRegistry WhiteboardProxiedServiceRegistry&lt;T&gt;}
 *     base class instead, or the {@link HippoServiceRegistry} for singleton service registration and lookup, like
 *     for the {@link org.onehippo.cms7.services.eventbus.HippoEventBus HippoEventBus} service.
 * </p>
 * @param <T> service object implementation type
 */
public abstract class WhiteboardServiceRegistry<T> {

    private final ConcurrentHashMap<ObjectIdentityKey, ServiceHolder<T>> services = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ObjectIdentityKey, ServiceHolder<ServiceTracker>> trackers = new ConcurrentHashMap<>();

    protected synchronized List<ServiceHolder<T>> getEntriesList() {
        return new ArrayList<>(services.values());
    }

    /**
     * Register a service object of type &lt;T&gt;
     * @param serviceObject the service object
     * @throws HippoServiceException when the service object was already registered
     */
    @SuppressWarnings("unchecked")
    public synchronized void register(final T serviceObject) throws HippoServiceException {
        Objects.requireNonNull(serviceObject, "serviceObject must not be null");
        final ObjectIdentityKey key = new ObjectIdentityKey(serviceObject);
        if (services.containsKey(key)) {
            throw new HippoServiceException("serviceObject already registered");
        }
        final ServiceHolder<T> serviceHolder = new ServiceHolder<>(serviceObject);

        services.put(key, serviceHolder);
        if (!trackers.isEmpty()) {
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {
                for (final ServiceHolder<ServiceTracker> trackerHolder : trackers.values()) {
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
        final ServiceHolder<T> serviceHolder = services.remove(new ObjectIdentityKey(serviceObject));
        if (serviceHolder != null && !trackers.isEmpty()) {
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {
                for (final ServiceHolder<ServiceTracker> trackerHolder : trackers.values()) {
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
     * Add a {@link ServiceTracker ServiceTracker&lt;T&gt;} for tracking service objects of type &lt;T&gt;
     * {@link #register(Object) registered} and {@link #unregister(Object) unregistered} in this
     * registry.
     * @param tracker the service tracker
     * @throws HippoServiceException when the provided service tracker instance already was added before
     */
    public synchronized void addTracker(final ServiceTracker<T> tracker) throws HippoServiceException {
        Objects.requireNonNull(tracker, "tracker must not be null");
        final ObjectIdentityKey key = new ObjectIdentityKey(tracker);
        if (trackers.containsKey(key)) {
            throw new HippoServiceException("tracker already added");
        }
        final ServiceHolder<ServiceTracker> trackerHolder = new ServiceHolder<>(tracker);
        trackers.put(key, trackerHolder);
        for (ServiceHolder<T> serviceHolder: services.values()) {
            tracker.serviceRegistered(serviceHolder);
        }
    }

    /**
     * Remove a previously added {@link ServiceTracker ServiceObjectTracker&lt;T&gt;}.
     * @param tracker the tracker
     * @return true if the tracker was added before and now removed, false otherwise
     */
    public synchronized boolean removeTracker(final ServiceTracker<T> tracker) {
        Objects.requireNonNull(tracker, "tracker must not be null");
        return trackers.remove(new ObjectIdentityKey(tracker)) != null;
    }

    /**
     * @return the registered service entries
     */
    public Stream<ServiceHolder<T>> getEntries() {
        return getEntriesList().stream();
    }

    /**
     * @return the number of services registered
     */
    public synchronized int size() {
        return services.size();
    }
}
