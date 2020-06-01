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
package org.hippoecm.hst.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for scanning (some) annotations for some methods or some class. This annotation scanning also
 * scans all superclasses and super interfaces.
 */
public class AnnotationsScanner {


    private final static  Map<String, List<MethodAnnotations>> alreadyInspectedClasses = new ConcurrentHashMap<String, List<MethodAnnotations>>();


    /**
     * object that contains all the annotations for some method
     */
    public static class MethodAnnotations {
        
        private final Method method;
        private final Set<Annotation> annotations;

        /**
         * A {@link MethodAnnotations} object contains all the fully qualified annotation classes for some method. 
         * Note that this includes annotations defined on superclasses/interfaces for the super classes/interfaces that 
         * have the same method
         */
        private MethodAnnotations(final Method method, final Set<Annotation> annotations) {
            if (annotations == null) {
                throw new IllegalArgumentException("Set not allowed to be null for MethodAnnotations");
            }
            this.method = method;
            this.annotations = annotations;
        }
        
        public Method getMethod() {
            return method;
        }

        /**
         * @return the set of fully qualified annotation classnames for method {@link #getMethod()}
         */
        public Set<Annotation> getAnnotations() {
            return annotations;
        }
    }

    /**
     * <p>
     * Returns the map of method mapped to all the annotation classes on such a method. Note that also annotations
     * on the public methods of super classes and interfaces are present.
     * </p>
     * @param clazz
     * @return the map where the keys are all the {@link Method} instances that contain annotations, and the values are the set of
     * annotation classes for the {@link Method} instance. Return empty map if no single method with an annotation
     * is found
     */
    public static Map<Method, Set<Annotation>> getMethodAnnotations(Class<?> clazz){
        Map<Method, Set<Annotation>> methodAnnotations = new HashMap<Method, Set<Annotation>>();
        List<MethodAnnotations> methodAnnotationsList = getMethodAnnotationsList(clazz);
        for (MethodAnnotations methodAnnotation : methodAnnotationsList) {
            Set<Annotation> overLoadedMethodPresent = methodAnnotations.get(methodAnnotation.getMethod());
            if (overLoadedMethodPresent != null) {
                overLoadedMethodPresent.addAll(methodAnnotation.getAnnotations());
            } else {
             methodAnnotations.put(methodAnnotation.getMethod(), methodAnnotation.getAnnotations());
            }
        }
        return methodAnnotations;
    }
    
    /**
     * Returns the {@link List} of all {@link MethodAnnotations} for class <code>clazz</code> : Thus, all the methods that
     * have an annotation (possibly on super classes/interfaces).
     * @param clazz the clazz to scan its methods 
     * @return the List of all {@link MethodAnnotations} for <code>clazz</code>. Empty list when no annotated methods found
     */
    public static List<MethodAnnotations> getMethodAnnotationsList(Class<?> clazz){
        List<MethodAnnotations> cached = alreadyInspectedClasses.get(clazz.getName());
        if (cached != null) {
            return cached;
        }
        List<MethodAnnotations> methodAnnotations = new ArrayList<MethodAnnotations>();
        for (final Method method : clazz.getMethods()) {
            final Set<Annotation> allAnnotations = new HashSet<Annotation>();
            populateAnnotationsForMethod(method, allAnnotations);
            if(!allAnnotations.isEmpty()) {
                methodAnnotations.add(new MethodAnnotations(method, allAnnotations));
            }
        }
        alreadyInspectedClasses.put(clazz.getName(), methodAnnotations);
        return methodAnnotations;
    }


    private static void populateAnnotationsForMethod(final Method method, final Set<Annotation> allAnnotations) {
        if (method == null) {
            return;
        }
        final Annotation[] annotations = method.getAnnotations();
        // method.getAnnotations() never returns null, see java specs so no null check
        for (Annotation annotation : annotations) {
            allAnnotations.add(annotation);
        }
        Class<?> superC = method.getDeclaringClass().getSuperclass();
        if (superC != null && Object.class != superC) {
            try {
                Method superMethod = superC.getMethod(method.getName(), method.getParameterTypes());
                populateAnnotationsForMethod(superMethod, allAnnotations);
            } catch (NoSuchMethodException ex) {
                // ignore
            }
        }
        for (Class<?> interfaze : method.getDeclaringClass().getInterfaces()) {
            try {
                Method superMethod = interfaze.getMethod(method.getName(), method.getParameterTypes());
                populateAnnotationsForMethod(superMethod, allAnnotations);
            } catch (NoSuchMethodException ex) {
                // ignore
            }
        }
    }

    /**
     * returns the annotated method with annotation clazz and null if the clazz annotation is not present
     * @param method the method to scan annotations for
     * @param clazz the annotation to look for
     * @return the {@link java.lang.reflect.Method} that contains the annotation <code>clazz</code> and <code>null</code> if none found
     */
    public static Method doGetAnnotatedMethod(final Method method, Class<? extends Annotation> clazz) {

        if (method == null) {
            return method;
        }

        Annotation annotation = method.getAnnotation(clazz);
        if(annotation != null ) {
            // found annotation
            return method;
        }

        Class<?> superC = method.getDeclaringClass().getSuperclass();
        if (superC != null && Object.class != superC) {
            try {
                Method superMethod = doGetAnnotatedMethod(superC.getMethod(method.getName(), method.getParameterTypes()), clazz);
                if (superMethod != null) {
                    return superMethod;
                }
            } catch (NoSuchMethodException ex) {
                // ignore
            }
        }
        for (Class<?> i : method.getDeclaringClass().getInterfaces()) {
            try {
                Method superMethod = doGetAnnotatedMethod(i.getMethod(method.getName(), method.getParameterTypes()), clazz);
                if (superMethod != null) {
                    return superMethod;
                }
            } catch (NoSuchMethodException ex) {
                // ignore
            }
        }

        return null;
    }
}
