/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.site.container;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hippoecm.hst.util.ClasspathResourceScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

/**
 * SpringMetadataReaderClasspathResourceScanner
 * <P>
 * This class implements {@link ClasspathResourceScanner} by leveraging Spring Framework components.
 * </P>
 * @version $Id$
 */
public class SpringMetadataReaderClasspathResourceScanner implements ClasspathResourceScanner, ResourceLoaderAware {
    
    private static Logger log = LoggerFactory.getLogger(SpringMetadataReaderClasspathResourceScanner.class);
    
    private ResourcePatternResolver resourcePatternResolver;
    
    public SpringMetadataReaderClasspathResourceScanner() {
        
    }
    
    public Set<String> scanClassNamesAnnotatedBy(Class<? extends Annotation> annotationType, boolean matchSuperClass, String ... locationPatterns) {
        if (resourcePatternResolver == null) {
            throw new IllegalStateException("ResourceLoader has not been set.");
        }
        
        if (locationPatterns == null || locationPatterns.length == 0) {
            throw new IllegalArgumentException("Provide one or more location pattern(s).");
        }
        
        Set<String> annotatedClassNames = new LinkedHashSet<String>();
        
        MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resourcePatternResolver);
        
        try {
            TypeFilter typeFilter = new CustomAnnotationTypeFilter(annotationType, matchSuperClass);
            
            for (String locationPattern : locationPatterns) {
                Resource [] resources = resourcePatternResolver.getResources(locationPattern);
                
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
    
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
    }
    
    private static class CustomAnnotationTypeFilter extends AnnotationTypeFilter {
        
        private boolean matchSuperClass;
        
        public CustomAnnotationTypeFilter(Class<? extends Annotation> annotationType, boolean matchSuperClass) {
            super(annotationType);
            this.matchSuperClass = matchSuperClass;
        }
        
        public CustomAnnotationTypeFilter(Class<? extends Annotation> annotationType, boolean considerMetaAnnotations, boolean matchSuperClass) {
            super(annotationType, considerMetaAnnotations);
            this.matchSuperClass = matchSuperClass;
        }
        
        @Override
        protected Boolean matchSuperClass(String superClassName) {
            if (matchSuperClass) {
                return super.matchSuperClass(superClassName);
            } else {
                return Boolean.FALSE;
            }
        }
    }
}
