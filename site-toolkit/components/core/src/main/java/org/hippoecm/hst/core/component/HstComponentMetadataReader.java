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

import java.util.Map;
import java.util.Set;

import org.hippoecm.hst.util.AnnotationsScanner;

public class HstComponentMetadataReader {

    private HstComponentMetadataReader() {
    }

    public static HstComponentMetadata getHstComponentMetadata(Class<?> clazz) {

        final Map<String, Set<String>> methodAnnotations = AnnotationsScanner.getMethodAnnotations(clazz);
        return new DefaultHstComponentMetadata(methodAnnotations);

    }

    private static class DefaultHstComponentMetadata implements HstComponentMetadata {

        final Map<String, Set<String>> methodAnnotations;

        public DefaultHstComponentMetadata(final Map<String, Set<String>> methodAnnotations) {
            this.methodAnnotations = methodAnnotations;
        }

        public boolean hasMethodAnnotatedBy(String annotation, String methodName) {
            if (!methodAnnotations.containsKey(methodName)) {
                return false;
            }
            return methodAnnotations.get(methodName).contains(annotation);
        }
    }
}
