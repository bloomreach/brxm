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

/**
 * Utility to load classes from the classpath using a marker annotation. This is intended for internal use within the
 * Hippo CMS and HST, and not for general use by third parties.
 */
public class ClasspathResourceAnnotationScanner {

    private static final Logger log = LoggerFactory.getLogger(ClasspathResourceAnnotationScanner.class);

    // cache intermediate results of previous calls to scan... as long as caller holds a reference to this instance
    private final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
    private final MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resourcePatternResolver);

    /**
     * Produces a set of full class-names for classes that match a marker annotation and also one of a specified set of
     * resource location patterns.
     * @param annotationType the Class representing the marker annotation for classes we wish to find
     * @param locationPatterns resource location patterns following {@link PathMatchingResourcePatternResolver} syntax
     * @return a Set<String> of full class-names for matching classes, suitable for use via Class.forName()
     */
    public Set<String> scanClassNamesAnnotatedBy(Class<? extends Annotation> annotationType, String ... locationPatterns) {

        // we need a constraint on the resource paths, so we don't scan the entire classpath
        // this method is intended for internal platform use, so we don't want to pick up third-party hacks
        if (locationPatterns == null || locationPatterns.length == 0) {
            throw new IllegalArgumentException("Provide one or more location pattern(s).");
        }

        // filter results by matching the given annotation class
        TypeFilter typeFilter = new AnnotationTypeFilter(annotationType);

        try {
            // accumulate unique results
            final Set<String> annotatedClassNames = new LinkedHashSet<>();

            // check all resources matching any of the patterns
            for (String locationPattern : locationPatterns) {
                for (Resource resource : resourcePatternResolver.getResources(locationPattern)) {

                    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
                    if (typeFilter.match(metadataReader, metadataReaderFactory)) {
                        // getClassMetadata() is cheaper than getAnnotationMetadata()
                        annotatedClassNames.add(metadataReader.getClassMetadata().getClassName());
                    }
                }
            }
            return annotatedClassNames;
        }
        catch (IOException e) {
            log.error("Cannot load resource(s) from the classpath.", e);
            throw new RuntimeException("Cannot load resource(s) from the classpath.", e);
        }
    }
}
