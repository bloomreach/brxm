/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.services.eventbus;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.onehippo.cms7.services.HippoServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory of dynamically generated {@link org.onehippo.cms7.services.eventbus.GuavaEventBusListenerProxy} extending
 * proxy classes and caching their instances, for use with Guava EventBus listener registration.
 * <p>
 *   These proxy classes are used and needed to be able to use Guava as {@link HippoEventBus} backing solution, across
 *   multiple web applications (thus across-context and across-classloaders) without having to expose Guava on a shared
 *   classloader.
 * </p>
 * <p>
 *   The Guava EventBus requires using its {@link com.google.common.eventbus.Subscribe Guava Subscribe} annotation, but
 *   {@link HippoEventBus} listeners (thus) cannot use this. Instead, they use the {@link Subscribe Hippo Subscribe}
 *   annotation.
 * </p>
 * <p>
 *   To be able to still register and (let Guava) invoke these listeners, they are first wrapped in a dynamically
 *   generated proxy class with for each of the listener {@link Subscribe Hippo Subscribe} annotated methods (including
 *   inherited ones) a custom method annotated with the {@link com.google.common.eventbus.Subscribe Guava Subscribe}
 *   annotation.
 * </p>
 * <p>
 *   The generated proxy class extends this class and delegates the invocation of the actual listener method to the
 *   {@link org.onehippo.cms7.services.eventbus.GuavaEventBusListenerProxy#handleEvent(int, Object)} method instead,
 *   which also takes care of temporarily setting the current ContextClassLoader to the one from when the listener was
 *   registered.
 * </p>
 * <p>
 *   Such a proxy class is only generated once for each listener class and reused if more than one listener (class)
 *   instance is registered on the {@link GuavaHippoEventBus}, in which case an existing proxy instance is
 *   {@link org.onehippo.cms7.services.eventbus.GuavaEventBusListenerProxy#clone(Object) cloned}
 *   with the new listener instance injected.
 * </p>
 * <p>
 *   The creation and caching of both generated proxy classes and instances is managed through the
 *   {@link org.onehippo.cms7.services.eventbus.GuavaEventBusListenerProxyFactory}.
 * </p>
 */
class GuavaEventBusListenerProxyFactory {

    static final Logger log = LoggerFactory.getLogger(GuavaEventBusListenerProxyFactory.class);

    private Map<Object, GuavaEventBusListenerProxy> proxyMap = new IdentityHashMap<>();
    private SetMultimap<Class, Object> subjectMap = HashMultimap.create();

    GuavaEventBusListenerProxyFactory(){}

    /**
     * Returns the {@link Subscribe} annotated method or null if this annotation is not present on this method nor on
     * its possible (public) parent class(es) or interface(s).
     * @param m method
     * @return the {@link Method} that contains the annotation <code>clazz</code> and <code>null</code> if none found
     */
    private Method getSubscribeMethod(final Method m) {

        if (m != null) {
            Annotation annotation = m.getAnnotation(Subscribe.class);
            if(annotation != null ) {
                return m;
            }

            Class<?> superC = m.getDeclaringClass().getSuperclass();
            if (superC != null && Object.class != superC) {
                try {
                    Method method = getSubscribeMethod(superC.getMethod(m.getName(), m.getParameterTypes()));
                    if (method != null) {
                        return method;
                    }
                } catch (NoSuchMethodException ex) {
                    // ignore
                }
            }
            for (Class<?> i : m.getDeclaringClass().getInterfaces()) {
                try {
                    Method method = getSubscribeMethod(i.getMethod(m.getName(), m.getParameterTypes()));
                    if (method != null) {
                        return method;
                    }
                } catch (NoSuchMethodException ex) {
                    // ignore
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private GuavaEventBusListenerProxy generateProxy(Object listener, ClassLoader cl) {

        String className = listener.getClass().getName();
        ArrayList<Method> subscribeMethods = new ArrayList<>();
        for (Method m : listener.getClass().getMethods()) {
            if (m.getParameterTypes().length == 1) {
                Method subscribeMethod = getSubscribeMethod(m);
                if (subscribeMethod != null) {
                    Annotation[] annotations = subscribeMethod.getAnnotations();
                    boolean ignoreMethod = false;
                    for (Annotation ann : annotations) {
                        if (ann.getClass().getName().equals("org.onehippo.repository.events.Persisted")) {
                            log.warn("Method " + subscribeMethod + "is annotated with both @Subscribe and @Persisted, "+
                                    "which is no longer supported: Listener method ignored.");
                            ignoreMethod = true;
                            break;
                        }
                    }
                    if (!ignoreMethod) {
                        subscribeMethods.add(subscribeMethod);
                    }
                }
            }
        }

        if (!subscribeMethods.isEmpty()) {
            Method[] methods = subscribeMethods.toArray(new Method[subscribeMethods.size()]);
            byte[] proxyClassBytes = generateProxyClassBytes(className, methods);
            Class proxyClass = new ClassLoader(GuavaEventBusListenerProxy.class.getClassLoader()) {
                public Class defineClass(String className, byte[] bytes) {
                    return defineClass(className, bytes, 0, bytes.length);
                }
            }.defineClass(className, proxyClassBytes);
            try {
                Constructor constructor = proxyClass.getConstructor(Object.class, ClassLoader.class, Method[].class);
                return (GuavaEventBusListenerProxy)constructor.newInstance(listener, cl, methods);
            }
            catch (Exception e)
            {
                log.error("Failed to create GuavaEventbusListenerProxy instance", e);
            }
        }
        return null;
    }

    /**
     * Dynamically generate the optcodes for a proxy class using ASM
     * <p>
     *   The generated proxy class (bytes) will have the same fully qualified class name as the wrapped listener, and
     *   the generated {@link com.google.common.eventbus.Subscribe} methods also the same name and signature as the
     *   methods they wrap. These methods, when invoked through Guava EventBus, will delegate the actual invocation of
     *   the wrapped method to their {@link org.onehippo.cms7.services.eventbus.GuavaEventBusListenerProxy} super class.
     * </p>
     * <p>
     *   The dynamically generated class is equivalent to the following pseudo template code:
     *   <pre><code>
     *       public class ${className} extends GuavaEventBusListenerProxy {
     *
     *           public ${className}(final Object listener, final ClassLoader cl, final Method[] methods) {
     *               super(listener, cl, methods);
     *           }
     *
     *      #for(int index = 0, Method method = methods[index]; index < methods.length; index++)
     *
     *           @ com.google.common.eventbus.Subscribe
     *           public void ${method.name}(${method.parameterTypes[0].name} event) throws InvocationTargetException {
     *               super.handleEvent(${index}, event);
     *           }
     *
     *      #end
     *
     *      }
     *   </code></pre>
     * </p>
     * <p>
     *   The actual ASM implementation code was largely generated by first writing an example class, similar to the
     *   above pseudo code, and thereafter running it through the ASM ASMiflier utility which gnerates the ASM code to
     *   (re)produce that exact class dynamically.
     * </p>
     * @param listenerClassName The name of the class to generate a proxy for
     * @param methods The array of {@link Subscribe org.onehippo.cms7.services.eventbus.Subscribe} annotated methods to
     *                wrap with a {@link com.google.common.eventbus.Subscribe} annotation.
     * @return The ASM generated optcodes for the proxy class
     */
    private byte[] generateProxyClassBytes(String listenerClassName, Method[] methods) {
        ClassWriter cw = new ClassWriter(0);

        String listenerInternalClassName = listenerClassName.replace('.', '/');
        String guavaEventbusListenerProxyInternalClassName = GuavaEventBusListenerProxy.class.getName().replace('.','/');

        // public class ${className} extends GuavaEventBusListenerProxy {
        cw.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, listenerInternalClassName, null,
                guavaEventbusListenerProxyInternalClassName, null);

        //     public ${className}(final Object listener, final ClassLoader cl, final Method[] methods) {
        //         super(listener, cl, methods);
        //     }
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>",
                "(Ljava/lang/Object;Ljava/lang/ClassLoader;[Ljava/lang/reflect/Method;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, guavaEventbusListenerProxyInternalClassName, "<init>",
                "(Ljava/lang/Object;Ljava/lang/ClassLoader;[Ljava/lang/reflect/Method;)V");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(4, 4);
        mv.visitEnd();

        for (int index = 0; index < methods.length; index++) {

            String paramTypeInternalClassName = methods[index].getParameterTypes()[0].getName().replace('.','/');

            //     @Subscribe
            //     public void ${method.name}(${method.parameterTypes[0].name} event) throws InvocationTargetException {
            //         super.handleEvent(${index}, event);
            //     }

            mv = cw.visitMethod(Opcodes.ACC_PUBLIC, methods[index].getName(), "(L"+paramTypeInternalClassName+";)V",
                    null, new String[] { "java/lang/reflect/InvocationTargetException" });
            mv.visitAnnotation("Lcom/google/common/eventbus/Subscribe;", true).visitEnd();
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitIntInsn(Opcodes.BIPUSH, index);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, guavaEventbusListenerProxyInternalClassName, "handleEvent",
                    "(ILjava/lang/Object;)V");
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(3, 2);
            mv.visitEnd();
        }

        // }
        cw.visitEnd();

        return cw.toByteArray();
    }

    /**
     * Create a new dynamically generated proxy for the provided listener.
     * <p>
     *   A proxy class is only generated for the first instance of the provided listener class (in the internal cache),
     *   for subsequent instances a clone of the first proxy instance will be created and seeded with the new listener
     *   object.
     * </p>
     * <p>
     *   If the provided listener is an instance of {@link HippoServiceRegistration}, a proxy for the
     *   {@link HippoServiceRegistration#getService()} is created instead, and its
     *   {@link HippoServiceRegistration#getClassLoader()} is used instead of the current ContextClassLoader to be
     *   used by the proxy instance when delegating and invoking the {@link Subscribe} annotated listener method.
     * </p>
     * @param subject The listener for which to generate a proxy wrapper, or a HippoRegistration with a listener service
     * @return A new proxy wrapper for the provided listener, or null if the provided listener does not have any
     *         (proper) {@link Subscribe} annotations.
     */
    synchronized GuavaEventBusListenerProxy createProxy(Object subject) {
        GuavaEventBusListenerProxy proxy = proxyMap.get(subject);
        if (proxy == null) {
            Object listener;
            ClassLoader cl;
            if (subject instanceof HippoServiceRegistration) {
                HippoServiceRegistration registration = (HippoServiceRegistration)subject;
                listener = registration.getService();
                cl = registration.getClassLoader();
            } else {
                listener = subject;
                cl = Thread.currentThread().getContextClassLoader();
            }
            Class listenerClass = listener.getClass();
            Set<Object> subjects = subjectMap.get(listenerClass);
            if (!subjects.isEmpty()) {
                proxy = proxyMap.get(subjects.iterator().next()).clone(listener);
            }
            else {
                proxy = generateProxy(listener, cl);
            }
            if (proxy != null) {
                subjectMap.put(listenerClass, subject);
                proxyMap.put(subject, proxy);
            }
        }
        return proxy;
    }

    /**
     * Remove a previously created proxy from the internal cache, after first
     * {@link GuavaEventBusListenerProxy#destroy() destroying} it.
     * <p>
     *   If the last proxy instance for the provided listener its class is removed, the cache will no longer have a
     *   reference to its generated proxy class either. Subsequent registering another listener of the same instance
     *   will then lead to anew generation of such a proxy class.
     * </p>
     * @param subject The listener previously used to create a proxy for
     * @return The already {@link GuavaEventBusListenerProxy#destroy() destroyed} proxy for the provided listener, or
     *         null if no proxy is registered for this listener
     */
    synchronized GuavaEventBusListenerProxy removeProxy(Object subject) {
        GuavaEventBusListenerProxy proxy = proxyMap.remove(subject);
        if (proxy != null) {
            Object listener;
            if (subject instanceof HippoServiceRegistration) {
                listener = ((HippoServiceRegistration)subject).getService();
            }
            else {
                listener = subject;
            }
            subjectMap.remove(listener.getClass(), subject);
            proxy.destroy();
        }
        return proxy;
    }

    /**
     * Clears the cache of proxy classes and their instances.
     * <p>
     *     Returns the proxy instances which were cached, already {@link GuavaEventBusListenerProxy#destroy() destroyed}
     *     which should be used to unregister them from the Guava EventBus.
     * </p>
     * @return the previously cached and already {@link GuavaEventBusListenerProxy#destroy() destroyed} proxy instances
     */
    public Collection<GuavaEventBusListenerProxy> clear() {
        subjectMap.clear();
        Collection<GuavaEventBusListenerProxy> proxies = proxyMap.values();
        for (GuavaEventBusListenerProxy proxy : proxyMap.values()) {
            proxy.destroy();
        }
        proxyMap.clear();
        return proxies;
    }
}
