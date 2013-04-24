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
package com.google.common.eventbus;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import org.onehippo.cms7.services.HippoServiceRegistration;
import org.onehippo.cms7.services.eventbus.Subscribe;

/**
 * Fork of the guava AnnotationHandlerFinder that is injected by reflection.
 * The guava handler finding strategy api is not yet public, but uses package security.
 */
public class HippoAnnotationHandlerFinder implements HandlerFindingStrategy {

    @Override
    public Multimap<Class<?>, EventHandler> findAllHandlers(Object registered) {
        Multimap<Class<?>, EventHandler> methodsInListener = HashMultimap.create();
        Object listener;
        ClassLoader classLoader;
        if (registered instanceof HippoServiceRegistration) {
            HippoServiceRegistration registration = (HippoServiceRegistration) registered;
            listener = registration.getService();
            classLoader = registration.getClassLoader();
        } else {
            listener = registered;
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        Class<?> clazz = listener.getClass();

        // loop through all the classes' methods
        for (Method method : clazz.getMethods()) {
            Method annotatedMethod = doGetAnnotatedMethod(method, Subscribe.class);
            if (annotatedMethod != null) {
                // the method has a Subscribe annotation
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 1) {
                    throw new IllegalArgumentException(
                            "Method " + method + " has @Subscribe annotation, but requires " +
                                    parameterTypes.length + " arguments.  Event handler methods " +
                                    "must require a single argument.");
                }
                Class<?> eventType = parameterTypes[0];
                if (acceptMethod(listener, method.getAnnotations(), eventType)) {
                    EventHandler handler = new HippoSynchronizedEventHandler(listener, method, classLoader);
                    methodsInListener.put(eventType, handler);
                }
            }
        }
    
        return methodsInListener;
    }

    protected boolean acceptMethod(Object listener, Annotation[] annotations, Class<?> parameterType) {
        return true;
    }
   
    /**
     * returns the annotated method with annotation clazz and null if the clazz annotation is not present
     * @param m
     * @param clazz the annotation to look for
     * @return the {@link Method} that contains the annotation <code>clazz</code> and <code>null</code> if none found
     */
    private static Method doGetAnnotatedMethod(final Method m, Class<Subscribe> clazz) {

        if (m == null) {
            return m;
        }

        Subscribe annotation = m.getAnnotation(clazz);
        if(annotation != null ) {
            // found Subscribe annotation
            return m;
        }
        
        Class<?> superC = m.getDeclaringClass().getSuperclass();
        if (superC != null && Object.class != superC) {
            try {
                Method method = doGetAnnotatedMethod(superC.getMethod(m.getName(), m.getParameterTypes()), clazz);
                if (method != null) {
                    return method;
                }
            } catch (NoSuchMethodException ex) {
                // ignore
            }
        }
        for (Class<?> i : m.getDeclaringClass().getInterfaces()) {
            try {
                Method method = doGetAnnotatedMethod(i.getMethod(m.getName(), m.getParameterTypes()), clazz);
                if (method != null) {
                    return method;
                }
            } catch (NoSuchMethodException ex) {
                // ignore
            }
        }

        return null;
    }

}
