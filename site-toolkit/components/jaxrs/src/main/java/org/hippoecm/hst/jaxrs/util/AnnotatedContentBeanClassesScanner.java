/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.jaxrs.util;

import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.ClasspathResourceScanner;
import org.hippoecm.hst.util.ObjectConverterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * AnnotatedContentBeanClassesScanner
 * @version $Id$
 */
public class AnnotatedContentBeanClassesScanner {
    
    private static Logger log = LoggerFactory.getLogger(AnnotatedContentBeanClassesScanner.class);
    
    private static final String BEANS_ANNOTATED_CLASSES_CONF_PARAM_ERROR_MSG = 
        "Please check HST-2 Content Beans Annotation configuration as servlet context parameter.\n" +
        "You can set a servlet context parameter named 'hst-beans-annotated-classes' with xml or classes location filter.\n" +
        "For example, '/WEB-INF/beans-annotated-classes.xml' or 'classpath*:org/examples/beans/**/*.class'";
    
    private AnnotatedContentBeanClassesScanner() {
        
    }
    
    public static List<Class<? extends HippoBean>> scanAnnotatedContentBeanClasses(HstRequestContext requestContext, String annoClassesResourcePath) {
        List<Class<? extends HippoBean>> annotatedClasses = null;
        
        if (!StringUtils.isBlank(annoClassesResourcePath)) {
            if (annoClassesResourcePath.startsWith("classpath*:")) {
                ComponentManager compManager = HstServices.getComponentManager();
                if (compManager != null) {
                    ClasspathResourceScanner scanner = (ClasspathResourceScanner) compManager.getComponent(ClasspathResourceScanner.class.getName());
                    
                    if (scanner != null) {
                        try {
                            annotatedClasses = ObjectConverterUtils.getAnnotatedClasses(scanner, StringUtils.split(annoClassesResourcePath, ", \t\r\n"));
                        } catch (Exception e) {
                            log.warn("Failed to collect annotated classes", e);
                        }
                    }
                }
            } else {
                try {
                    URL xmlConfURL = requestContext.getServletContext().getResource(annoClassesResourcePath);
                    if (xmlConfURL == null) {
                        throw new IllegalStateException(BEANS_ANNOTATED_CLASSES_CONF_PARAM_ERROR_MSG);
                    }

                    annotatedClasses = ObjectConverterUtils.getAnnotatedClasses(xmlConfURL);
                } catch (Exception e) {
                    log.warn("Failed to collect annotated classes", e);
                }
            }
        }
        
        if (annotatedClasses == null) {
            annotatedClasses = Collections.emptyList();
        }
        
        return annotatedClasses;
    }

}
