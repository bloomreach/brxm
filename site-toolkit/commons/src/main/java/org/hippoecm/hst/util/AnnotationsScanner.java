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
        
        private final String methodName;
        private final Set<String> annotations;

        /**
         * A {@link MethodAnnotations} object contains all the fully qualified annotation class names for some method name. 
         * Note that this includes annotations defined on superclasses/interfaces for the method 
         */
        private MethodAnnotations(final String methodName, final Set<String> annotations) {
            if (annotations == null) {
                throw new IllegalArgumentException("Set not allowed to be null for MethodAnnotations");
            }
            this.methodName = methodName;
            this.annotations = annotations;
        }
        
        public String getMethodName() {
            return methodName;
        }

        /**
         * @return the set of fully qualified annotation classnames for method {@link #getMethodName()}
         */
        public Set<String> getAnnotations() {
            return annotations;
        }
    }

    /**
     * <p>
     * Returns the map of method name mapped to all the annotations on such a method name. Note that also annotations
     * on the public methods of super classes and interfaces are present
     * </p>
     * <p>
     *     Note that the annotations for <b>overloaded</b> methods are all combined. Thus the overloaded annotated method
     *     below will result in a Map that has a key <code>annotated</code> and value a <code>Set</code> containing
     *     {TestAnno1.class.getName(),TestAnno2.class.getName(), TestAnno2.class.getName() }
     *     <pre>
     *     <code>
     *          @TestAnno1
     *          public void annotated() {}
     *          @TestAnno2
     *          public String annotated(String foo) {return null;}
     *          @TestAnno3
     *          public void annotated(boolean foo) {}
     *     </code>
     *     </pre>
     * </p>
     * @param clazz
     * @return the map where the keys are all the method names that contain annotations, and the values are the set of
     * fully qualified annotation class name for the method name. Return empty map if no single method with an annotation
     * is found
     */
    public static Map<String,Set<String>> getMethodAnnotations(Class<?> clazz){
        Map<String, Set<String>> methodAnnotations = new HashMap<String, Set<String>>();
        List<MethodAnnotations> methodAnnotationsList = getMethodAnnotationsList(clazz);
        for (MethodAnnotations methodAnnotation : methodAnnotationsList) {
            Set<String> overLoadedMethodPresent = methodAnnotations.get(methodAnnotation.getMethodName());
            if (overLoadedMethodPresent != null) {
                overLoadedMethodPresent.addAll(methodAnnotation.getAnnotations());
            } else {
             methodAnnotations.put(methodAnnotation.getMethodName(), methodAnnotation.getAnnotations());
            }
        }
        return methodAnnotations;
    }
    
    /**
     * Returns the {@link List} of all {@link MethodAnnotations} for class <code>clazz</code> : Thus, all the methods that
     * have an annotation (possibly on super classes/interfaces). <b>note</b> that the List can contain multiple MethodAnnotations
     * with the same {@link MethodAnnotations#getMethodName()} because due to method overloading
     * @param clazz the clazz to scan its methods 
     * @return the List of all {@link MethodAnnotations} for <code>clazz</code>. Empty list when no annotated methods found
     */
    public static List<MethodAnnotations> getMethodAnnotationsList(Class<?> clazz){
        List<MethodAnnotations> cached = alreadyInspectedClasses.get(clazz.getName());
        if (cached != null) {
            return cached;
        }
        List<MethodAnnotations> methodAnnotations = new ArrayList<MethodAnnotations>();
        for (Method method : clazz.getMethods()) {
            Set<String> allAnnotations = new HashSet<String>();
            populateAnnotationsForMethod(method, allAnnotations);
            if(!allAnnotations.isEmpty()) {
                MethodAnnotations ma = new MethodAnnotations(method.getName(), allAnnotations);
                methodAnnotations.add(ma);
            }
        }
        alreadyInspectedClasses.put(clazz.getName(), methodAnnotations);
        return methodAnnotations;
    }


    private static void populateAnnotationsForMethod(final Method method, final Set<String> allAnnotations) {
        if (method == null) {
            return;
        }
        final Annotation[] annotations = method.getAnnotations();
        // method.getAnnotations() never returns null, see java specs so no null check
        for (Annotation annotation : annotations) {
            allAnnotations.add(annotation.annotationType().getName());
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
