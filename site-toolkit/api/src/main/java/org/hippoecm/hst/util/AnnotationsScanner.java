/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.hst.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Utility class for scanning (some) annotations for some methods or some class. This annotation scanning also
 * scans all superclasses and super interfaces.
 */
public class AnnotationsScanner {



    /**
     * returns the annotated method with annotation clazz and null if the clazz annotation is not present
     * @param m
     * @param clazz the annotation to look for
     * @return the {@link java.lang.reflect.Method} that contains the annotation <code>clazz</code> and <code>null</code> if none found
     */
    public static Method doGetAnnotatedMethod(final Method m, Class<? extends Annotation> clazz) {

        if (m == null) {
            return m;
        }

        Annotation annotation = m.getAnnotation(clazz);
        if(annotation != null ) {
            // found annotation
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
