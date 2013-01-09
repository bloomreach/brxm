/**
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hippoecm.hst.util.AnnotationsScanner;

public class HstComponentMetadataReader {

    private HstComponentMetadataReader() {
    }

    public static HstComponentMetadata getHstComponentMetadata(Class<?> clazz) {

        return new DefaultHstComponentMetadata(AnnotationsScanner.getMethodAnnotations(clazz));

    }

    private static class DefaultHstComponentMetadata implements HstComponentMetadata {

        final Map<String, Set<String>> methodAnnotations;

        public DefaultHstComponentMetadata(final Map<Method, Set<Annotation>> methodAnnotationsMap) {
            methodAnnotations = new HashMap<String, Set<String>>();
            for (Map.Entry<Method, Set<Annotation>> methodAnnotation : methodAnnotationsMap.entrySet()) {
                Set<String> annotations = new HashSet<String>();
                for (Annotation annotation :  methodAnnotation.getValue()) {
                    annotations.add(annotation.annotationType().getName());
                }
                Set<String> annotationsSet = methodAnnotations.get(methodAnnotation.getKey().getName());
                if (annotationsSet == null) {
                    // there is already a method with the same name containing annotations we do not need
                    // to  create a new set but add the annotations of the overloaded method to the existing set
                    annotationsSet = new HashSet<String>();
                    methodAnnotations.put(methodAnnotation.getKey().getName(), annotationsSet);
                }
                annotationsSet.addAll(annotations);
            }
        }

        public boolean hasMethodAnnotatedBy(String annotation, String methodName) {
            if (!methodAnnotations.containsKey(methodName)) {
                return false;
            }
            return methodAnnotations.get(methodName).contains(annotation);
        }
    }
}
