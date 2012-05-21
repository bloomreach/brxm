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
package org.hippoecm.frontend.service.restproxy.test.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility class to help test code collecting Java {@link Annotation}(s) of interest
 */
public class AnnotationsCollector {

    public static List<? extends Annotation> collect(Class<?> clazz, List<Class<? extends Annotation>> annotationTypes) {
        return collect(false, clazz, annotationTypes);
    }

    public static List<? extends Annotation> collect(boolean collectClassAnnotations, Class<?> clazz, List<Class<? extends Annotation>> annotationTypes) {
        List<Annotation> annotationsFound = new ArrayList<Annotation>();

        if (collectClassAnnotations) {
            // Collect annotations of the class level
            for (Annotation annotation : clazz.getAnnotations()) {
                if (annotationTypes.contains(annotation)) {
                    annotationsFound.add(annotation);
                }
            }
        }

        // Collect annotations defined on clazz's methods
        for (Method method : clazz.getMethods()) {
            for (Annotation annotation : method.getAnnotations()) {
                if (annotationTypes.contains(annotation.annotationType())) {
                    annotationsFound.add(annotation);
                }
            }
        }

        return annotationsFound;
    }

}
