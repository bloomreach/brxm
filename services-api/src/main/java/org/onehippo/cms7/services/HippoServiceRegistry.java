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
package org.onehippo.cms7.services;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * The singleton Hippo service registry.  Serves as a service locator across applications running in the same JVM.
 */
public final class HippoServiceRegistry {
    
    private volatile static int version = 0;

    private static class NamedRegistration extends HippoServiceRegistration {

        private final Object proxy;
        private final Class<?> iface;

        private NamedRegistration(final ClassLoader classLoader, final Object service, Class<?> iface) {
            super(classLoader, service);
            if (!iface.isInstance(service)) {
                throw new HippoServiceException("Service does not implement provided interface " + iface.getName());
            }
            this.iface = iface;
            this.proxy = Proxy.newProxyInstance(classLoader, new Class[]{iface}, new InvocationHandler() {

                @Override
                public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                    final ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(classLoader);
                    try {
                        return method.invoke(service, args);
                    }
                    catch (InvocationTargetException ite) {
                        throw (ite.getCause() != null) ? ite.getCause() : ite;
                    } finally {
                        Thread.currentThread().setContextClassLoader(currentContextClassLoader);
                    }
                }
            });
        }

        public Object getProxy() {
            return proxy;
        }

        public Class<?> getInterface() {
            return iface;
        }

        @Override
        public boolean equals(final Object obj) {
            // although this is the same as super.equals, we add the equals here explicitly : NamedRegistration's should
            // always be compared on reference identity
            return (this == obj);
        }
    }

    private static class ServiceDescriptor {

        private final WhiteboardService whiteboardService;
        private final SingletonService singletonService;
        private final Class<?> iface;

        private ServiceDescriptor(Class<?> iface) {
            if (!iface.isInterface()) {
                throw new HippoServiceException(
                        "Can only register services under an interface, " + iface.getName() + " is not an interface");
            }

            this.iface = iface;
            this.singletonService = iface.getAnnotation(SingletonService.class);
            this.whiteboardService = iface.getAnnotation(WhiteboardService.class);
        }

        public Class<?> getInterface() {
            return iface;
        }

        public void checkSingleton() {
            if (singletonService == null) {
                throw new HippoServiceException("No SingletonService annotation present on interface " + iface);
            }
        }

        public void checkNonSingleton() {
            if (singletonService != null) {
                throw new HippoServiceException("SingletonService annotation is present on interface " + iface);
            }
        }

        public void checkWhiteboard() {
            if (whiteboardService == null) {
                throw new HippoServiceException("No WhitboardService annotation present on interface " + iface);
            }
        }

        public boolean isSingleton(Object service) {
            return singletonService != null && iface.isAssignableFrom(service.getClass());
        }

        public boolean isWhiteboard(Object service) {
            return whiteboardService != null && !iface.isAssignableFrom(service.getClass());
        }

    }

    private static final Map<String, NamedRegistration> namedServices = new HashMap<String, NamedRegistration>();
    private static final Map<Class<?>, List<HippoServiceRegistration>> unnamedServices = new HashMap<Class<?>, List<HippoServiceRegistration>>();

    private HippoServiceRegistry() {
    }

    // public Singleton service method

    /**
     * Register a service under a {@link SingletonService} annotated interface.
     * If the service is an instance of the interface, it will be registered as the singleton.
     * <p>
     * If the interface has an {@link WhiteboardService} annotation, it is still possible to
     * register the service.
     *
     * @param service
     * @param ifaceClass
     */
    public synchronized static void registerService(Object service, Class<?> ifaceClass) {
        ServiceDescriptor descriptor = new ServiceDescriptor(ifaceClass);
        descriptor.checkSingleton();
        if (descriptor.isSingleton(service)) {
            registerNamedServiceInternal(service, ifaceClass, ifaceClass.getName());
        } else {
            descriptor.checkWhiteboard();
            registerUnnamedServiceInternal(service, ifaceClass);
        }
    }

    public synchronized static void unregisterService(Object service, Class<?> ifaceClass) {
        ServiceDescriptor descriptor = new ServiceDescriptor(ifaceClass);
        descriptor.checkSingleton();
        if (descriptor.isSingleton(service)) {
            unregisterNamedServiceInternal(service, ifaceClass, ifaceClass.getName());
        } else {
            descriptor.checkWhiteboard();
            unregisterUnnamedServiceInternal(service, ifaceClass);
        }
    }

    /**
     * Retrieve a service by it's default (class) name.  This provides a mechanism for services that used to be
     * singletons to be upgraded to non-singleton situations.
     *
     * @param ifaceClass
     * @return
     */
    public synchronized static <T> T getService(Class<T> ifaceClass) {
        return getNamedServiceInternal(ifaceClass, ifaceClass.getName());
    }

    /**
     * Retrieve a list of whiteboard services by it's default (class) name.
     *
     * @param ifaceClass
     * @return
     */
    public synchronized static List<HippoServiceRegistration> getRegistrations(Class ifaceClass) {
        return getUnnamedServicesInternal(ifaceClass);
    }


    // public non-singleton services

    public synchronized static void registerService(Object service, Class<?> ifaceClass, String name) {
        ServiceDescriptor descriptor = new ServiceDescriptor(ifaceClass);
        descriptor.checkNonSingleton();
        registerNamedServiceInternal(service, ifaceClass, name);
    }

    public synchronized static void unregisterService(Object service, Class<?> ifaceClass, String name) {
        ServiceDescriptor descriptor = new ServiceDescriptor(ifaceClass);
        descriptor.checkNonSingleton();
        unregisterNamedServiceInternal(service, ifaceClass, name);
    }

    public synchronized static <T> T getService(Class<T> ifaceClass, String name) {
        ServiceDescriptor descriptor = new ServiceDescriptor(ifaceClass);
        descriptor.checkNonSingleton();
        return getNamedServiceInternal(ifaceClass, name);
    }

    /**
     * @return the version of this {@link HippoServiceRegistry} : Every time the registry gets a service added or removed
     * the version is incremented. This is for classes using the HippoServiceRegistry that they can easily check whether
     * they need to register or unregister new {@link HippoServiceRegistration}s
     */
    public static int getVersion() {
        return version;
    }


    // internal registry management

    private static void registerNamedServiceInternal(Object service, Class<?> ifaceClass, String name) {
        NamedRegistration registration = newRegistration(service, ifaceClass);
        if (!namedServices.containsKey(name)) {
            namedServices.put(name, registration);
        } else {
            throw new HippoServiceException("A service was already registered with name " + name);
        }
        version++;
    }

    private static void unregisterNamedServiceInternal(Object service, Class<?> ifaceClass, String name) {
        NamedRegistration registration = namedServices.get(name);
        if (registration != null && registration.getService() == service) {
            namedServices.remove(name);
        }
        version++;
    }

    private static <T> T getNamedServiceInternal(final Class<T> ifaceClass, final String name) {
        NamedRegistration registration = namedServices.get(name);
        if (registration != null) {
            if (ifaceClass.isAssignableFrom(registration.getInterface())) {
                return (T) registration.getProxy();
            }
        }
        return null;
    }

    private static void registerUnnamedServiceInternal(Object service, Class<?> ifaceClass) {
        if (ifaceClass.isInstance(service)) {
            throw new HippoServiceException("Service implements provided interface " + ifaceClass.getName() +
                                                    ", a whiteboard listener should not do that");
        }

        HippoServiceRegistration registration = newRegistration(service);
        List<HippoServiceRegistration> siblings;
        if (!unnamedServices.containsKey(ifaceClass)) {
            siblings = new LinkedList<HippoServiceRegistration>();
            unnamedServices.put(ifaceClass, siblings);
        } else {
            siblings = unnamedServices.get(ifaceClass);
        }
        siblings.add(registration);
        version++;
    }

    private static void unregisterUnnamedServiceInternal(Object service, Class<?> ifaceClass) {
        List<HippoServiceRegistration> siblings = unnamedServices.get(ifaceClass);
        if (siblings != null) {
            for (HippoServiceRegistration sibling : siblings) {
                if (sibling.getService() == service) {
                    siblings.remove(sibling);
                    break;
                }
            }
            if (siblings.size() == 0) {
                unnamedServices.remove(ifaceClass);
            }
        }
        version++;
    }

    private static List<HippoServiceRegistration> getUnnamedServicesInternal(final Class<?> clazz) {
        List<HippoServiceRegistration> siblings = unnamedServices.get(clazz);
        if (siblings != null) {
            return new ArrayList<HippoServiceRegistration>(siblings);
        }
        return Collections.emptyList();
    }

    // utility methods

    private static NamedRegistration newRegistration(final Object service, final Class<?> ifaceClass) {
        return new NamedRegistration(Thread.currentThread().getContextClassLoader(), service, ifaceClass);
    }

    private static HippoServiceRegistration newRegistration(final Object service) {
        return new HippoServiceRegistration(Thread.currentThread().getContextClassLoader(), service);
    }
}
