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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The ProxiedServiceHolder holds a service object and a proxy instance of its service interface(s).
 */
public class ProxiedServiceHolder<T> extends ServiceHolder<T> {

    private final Class<T> serviceInterface;
    private final Class<?>[] extraInterfaces;
    private final T service;

    /**
     * Create a holder for a service object and a {@link #getServiceProxy()} proxy implementing the serviceInterface.
     * Optionally additional extra interfaces may be specified also to be implemented by the proxy.
     * @param serviceObject the service object to create the holder for
     * @param serviceInterface the interface to be implemented by the proxy
     * @param extraInterfaces optional additional interfaces to be implemented by the proxy
     */
    @SuppressWarnings("unchecked")
    ProxiedServiceHolder(final T serviceObject, final Class<T> serviceInterface, final Class<?>... extraInterfaces) {
        super(serviceObject);
        Objects.requireNonNull(serviceInterface, "serviceInterface must not be null");
        Objects.requireNonNull(extraInterfaces, "extraInterfaces must not be null");
        this.serviceInterface = serviceInterface;
        final Set<Class> interfaces = new HashSet<>();
        interfaces.add(serviceInterface);
        if (extraInterfaces.length > 0) {
            // check and cleanup extraInterfaces:
            // - guard against null elements
            // - remove duplicates (Proxy creation will otherwise fail)
            for (final Class extraInterface : extraInterfaces) {
                Objects.requireNonNull(extraInterface, "extraInterfaces must not be null");
                if (!extraInterface.isInstance(serviceObject)) {
                    throw new HippoServiceException("Service object not implementing interface " + extraInterface.getName());
                }
                interfaces.add(extraInterface);
            }
        }
        service = (T)Proxy.newProxyInstance(getClassLoader(), interfaces.toArray(new Class[0]), (proxy, method, args) -> {
            final ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(getClassLoader());
            try {
                return method.invoke(getServiceObject(), args);
            } catch (InvocationTargetException ite) {
                throw (ite.getCause() != null) ? ite.getCause() : ite;
            } finally {
                Thread.currentThread().setContextClassLoader(currentContextClassLoader);
            }
        });
        interfaces.remove(serviceInterface);
        this.extraInterfaces = interfaces.toArray(new Class[0]);
    }

    /**
     * @return the interface to be implemented by the proxy returned by {@link #getServiceProxy()}
     */
    public Class<T> getServiceInterface() {
        return serviceInterface;
    }

    /**
     * @return the extra interfaces to be implemented by the proxy returned by {@link #getServiceProxy()},
     * if specified during the creation of the service holder, otherwise an empty stream will be returned.
     */
    public Stream<Class<?>> getExtraInterfaces() {
        return Arrays.stream(extraInterfaces);
    }

    /**
     * @return The proxy implementing {{@link #getServiceInterface()}} and {@link #getExtraInterfaces()} if defined,
     * Methods invoked on the proxy will be executed with the
     * {@link #getClassLoader() Thread.currentThread().getContextClassLoader} when this service holder was created
     */
    public T getServiceProxy() {
        return service;
    }
}
