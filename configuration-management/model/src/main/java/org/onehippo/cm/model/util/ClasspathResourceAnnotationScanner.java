/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.util;


import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

public class ClasspathResourceAnnotationScanner {

    private static final Logger log = LoggerFactory.getLogger(ClasspathResourceAnnotationScanner.class);

    private ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    public Set<String> scanClassNamesAnnotatedBy(Class<? extends Annotation> annotationType, String ... locationPatterns) {

        if (locationPatterns == null || locationPatterns.length == 0) {
            throw new IllegalArgumentException("Provide one or more location pattern(s).");
        }

        final Set<String> annotatedClassNames = new LinkedHashSet<String>();

        final MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resourcePatternResolver);

        try {
            TypeFilter typeFilter = new AnnotationTypeFilter(annotationType);

            for (String locationPattern : locationPatterns) {
                Resource[] resources = resourcePatternResolver.getResources(locationPattern);

                for (Resource resource : resources) {
                    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);

                    if (typeFilter.match(metadataReader, metadataReaderFactory)) {
                        annotatedClassNames.add(metadataReader.getAnnotationMetadata().getClassName());
                    }
                }
            }
        } catch (IOException e) {
            log.error("Cannot load resource(s) from the classpath.", e);
            throw new RuntimeException("Cannot load resource(s) from the classpath.", e);
        }

        return annotatedClassNames;
    }
}
