/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * AnnotationUtils
 */
public class AnnotationUtils {

    private AnnotationUtils() {
    }

    /**
     * Finds the annotation from the <code>method</code> or all the methods defined in its super classes or interfaces.
     * Returns null if the annotation is not present.
     * <P>
     * The searching order is as follows:
     * <OL>
     * <LI>the input <code>method</code></LI>
     * <LI>if not found yet, all the methods defined in all the interfaces implemented by the class having the input <code>method</code></LI>
     * <LI>if not found yet, recursively search from the super class of the class having the input <code>method</code></LI>
     * </OL>
     * </P>
     * @param method
     * @param annotationClass
     * @return
     */
    public static <A extends Annotation> A findMethodAnnotation(final Method method, Class<A> annotationClass) {
        if (method == null) {
            return null;
        }

        A anno = method.getAnnotation(annotationClass);

        if (anno != null) {
            return anno;
        }

        for (Class<?> iface : method.getDeclaringClass().getInterfaces()) {
            try {
                anno = findMethodAnnotation(iface.getMethod(method.getName(), method.getParameterTypes()), annotationClass);
                if (anno != null) {
                    return anno;
                }
            } catch (NoSuchMethodException ex) {
                // ignore
            }
        }

        Class<?> superClazz = method.getDeclaringClass().getSuperclass();

        if (superClazz != null && Object.class != superClazz) {
            try {
                anno = findMethodAnnotation(superClazz.getMethod(method.getName(), method.getParameterTypes()), annotationClass);

                if (anno != null) {
                    return anno;
                }
            } catch (NoSuchMethodException ex) {
                // ignore
            }
        }

        return null;
    }

}
