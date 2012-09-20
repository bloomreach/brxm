/**
 * Copyright 2012 Hippo.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.core.component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hippoecm.hst.content.annotations.Persistable;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

public class HstComponentMetadataReader {

    private HstComponentMetadataReader() {
    }

    public static HstComponentMetadata getHstComponentMetadata(ClassLoader classloader, String componentClassName) {
        HstComponentMetadata componentMetadata = null;

        try {
            MetadataReaderFactory factory = new SimpleMetadataReaderFactory(classloader);
            MetadataReader reader = factory.getMetadataReader(componentClassName);
            AnnotationMetadata annotationMetadata = reader.getAnnotationMetadata();

            Map<String, MethodMetadata> methodMetadataMap = new HashMap<String, MethodMetadata>();
            Set<MethodMetadata> annotatedMethodMetadataSet = annotationMetadata.getAnnotatedMethods(Persistable.class.getName());

            for (MethodMetadata annotatedMethodMetadata : annotatedMethodMetadataSet) {
                methodMetadataMap.put(annotatedMethodMetadata.getMethodName(), annotatedMethodMetadata);
            }

            componentMetadata = new DefaultHstComponentMetadata(componentClassName, methodMetadataMap);
        } catch (Exception e) {
        }

        return componentMetadata;
    }

    private static class DefaultHstComponentMetadata implements HstComponentMetadata {

        private final String className;
        private final Map<String, MethodMetadata> methodMetadataMap;

        private DefaultHstComponentMetadata(final String className, final Map<String, MethodMetadata> methodMetadataMap) {
            this.className = className;
            this.methodMetadataMap = methodMetadataMap;
        }

        public String getClassName() {
            return className;
        }

        public boolean hasMethodAnnotatedBy(String annotationType, String methodName) {
            if (!methodMetadataMap.containsKey(methodName)) {
                return false;
            }

            MethodMetadata metadata = methodMetadataMap.get(methodName);
            return metadata.isAnnotated(annotationType);
        }
    }
}
