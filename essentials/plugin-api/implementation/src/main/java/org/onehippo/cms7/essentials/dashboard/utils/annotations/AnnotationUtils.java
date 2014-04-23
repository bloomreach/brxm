/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.dashboard.utils.annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for parsing annotations and bean properties (fields/methods)
 *
 * @version $Id$
 */
public final class AnnotationUtils {

    private static Logger log = LoggerFactory.getLogger(AnnotationUtils.class);


    private AnnotationUtils() {
    }

    /**
     * Return annotation of specific type
     *
     * @param clazz           class file we are scanning
     * @param annotationClass annotation type we are interested in
     * @param <T>             Annotation type
     * @return null if no annotation found, annotation otherwise
     */
    public static <T extends Annotation> T getClassAnnotation(final Class<?> clazz, final Class<T> annotationClass) {
        if (!clazz.isAnnotationPresent(annotationClass)) {
            return null;
        }
        return clazz.getAnnotation(annotationClass);
    }

    /**
     * Get fields of an class which are annotated with specific
     * annotation and set them accessible (if necessary)
     *
     * @param clazz           class we are scanning for annotated fields.
     * @param annotationClass annotation we are interested in
     * @return a collection containing (accessible) fields we have found (or an empty collection)
     */
    public static Collection<Field> getAnnotatedFields(final Class<?> clazz, final Class<? extends Annotation> annotationClass) {
        Collection<Field> fields = getClassFields(clazz);
        Iterator<Field> iterator = fields.iterator();
        while (iterator.hasNext()) {
            Field field = iterator.next();
            if (!field.isAnnotationPresent(annotationClass)) {
                iterator.remove();
            } else if (!field.isAccessible()) {
                try {
                    field.setAccessible(true);
                } catch (SecurityException se) {
                    log.error(MessageFormat.format("Security exception while setting accessible: {0}", se));
                }
            }
        }
        return fields;
    }

    /**
     * Find a class for given name
     *
     * @param name of the class
     * @return null if not found or Class if found
     */
    @SuppressWarnings({"unchecked"})
    public static <T> Class<T> findClass(final String name) {
        try {
            return (Class<T>) Thread.currentThread().getContextClassLoader().loadClass(name);
        } catch (ClassNotFoundException e) {
            log.error("No class found within class loader " + e);
        }
        return null;
    }

    /**
     * Get fields for given class.
     *
     * @param clazz class to scan for fields
     * @return collection of Fields
     */
    public static Collection<Field> getClassFields(Class<?> clazz) {
        Map<String, Field> fields = new HashMap<>();
        Class<?> myClass = clazz;
        for (; myClass != null; ) {
            for (Field field : myClass.getDeclaredFields()) {
                if (!fields.containsKey(field.getName())) {
                    fields.put(field.getName(), field);
                }
            }
            myClass = myClass.getSuperclass();
        }

        return fields.values();
    }

    /**
     * Scans class for declared methods
     *
     * @param clazz class we are interested in
     * @return collection of declared methods
     */
    public static Collection<Method> getMethods(Class<?> clazz) {
        Map<String, Method> returnValue = new HashMap<>();
        Class<?> myClass = clazz;
        for (; myClass != null; ) {
            for (Method method : myClass.getDeclaredMethods()) {
                boolean isOverridden = false;
                for (Method overriddenMethod : returnValue.values()) {
                    if (overriddenMethod.getName().equals(method.getName()) && Arrays.deepEquals(method.getParameterTypes(), overriddenMethod.getParameterTypes())) {
                        isOverridden = true;
                        break;
                    }
                }
                if (!isOverridden) {
                    returnValue.put(method.getName(), method);
                }
            }
            myClass = myClass.getSuperclass();
        }
        return returnValue.values();
    }
}

