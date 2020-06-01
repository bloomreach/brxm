/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.tool;

import java.util.List;

import javax.jcr.Session;
import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.ClasspathResourceScanner;
import org.hippoecm.hst.util.ObjectConverterUtils;

/**
 * DefaultContentBeansTool
 */
public class DefaultContentBeansTool implements ContentBeansTool {

    public static final String BEANS_ANNOTATED_CLASSES_CONF_PARAM = "hst-beans-annotated-classes";

    private final HstQueryManagerFactory queryManagerFactory;

    /*
     * On purpose, the objectConverter below is not <code>volatile</code>. This means that the getObjectConverter()
     * is not a correct double-checked locking singleton pattern. However, objectConverter is used by every thread
     * throughout the entire life time of the application. Making access to it volatile is way more expensive than
     * the very small chance that it gets created 2 or 3 times in the very beginning after start up of the application.
     * Thus, on purpose not a correct double checked locking in favor of less expensive access later
     */
    private ObjectConverter objectConverter;

    private String annotatedClassesResourcePath;

    public DefaultContentBeansTool(HstQueryManagerFactory queryManagerFactory) {
        this.queryManagerFactory = queryManagerFactory;
    }

    /**
     * Gets the manually configured annotated classes resource path.
     * @return
     */
    public String getAnnotatedClassesResourcePath() {
        return annotatedClassesResourcePath;
    }

    /**
     * Sets the annotated classes resource path manually.
     * If not set, then it reads the servlet context init parameter named 'hst-beans-annotated-classes' by default.
     * @param annotatedClassesResourcePath
     */
    public void setAnnotatedClassesResourcePath(String annotatedClassesResourcePath) {
        this.annotatedClassesResourcePath = annotatedClassesResourcePath;
    }

    public ObjectConverter getObjectConverter() {
        if (objectConverter == null) {
            synchronized (this) {
                if (objectConverter == null) {
                    if (!HstServices.isAvailable()) {
                        throw new IllegalStateException("HST Services are not available.");
                    }

                    ClasspathResourceScanner classpathResourceScanner = HstServices.getComponentManager().getComponent(ClasspathResourceScanner.class.getName());
                    List<Class<? extends HippoBean>> annotatedClasses = getAnnotatedClasses(classpathResourceScanner);
                    objectConverter = ObjectConverterUtils.createObjectConverter(annotatedClasses);
                }
            }
        }

        return objectConverter;
    }

    public void setObjectConverter(ObjectConverter objectConverter) {
        this.objectConverter = objectConverter;
    }

    public ObjectBeanManager createObjectBeanManager(Session session) {
        return new ObjectBeanManagerImpl(session, getObjectConverter());
    }

    public HstQueryManager createQueryManager(Session session) throws IllegalStateException {
        return queryManagerFactory.createQueryManager(session, getObjectConverter());
    }

    private List<Class<? extends HippoBean>> getAnnotatedClasses(final ClasspathResourceScanner classpathResourceScanner) {
        List<Class<? extends HippoBean>> annotatedClasses = null;

        String ocmAnnotatedClassesResourcePath = getAnnotatedClassesResourcePath();

        // if not manually configured, then read it from servlet context init parameter.
        if (ocmAnnotatedClassesResourcePath == null) {
            ServletContext servletContext = HstServices.getComponentManager().getServletContext();

            if (servletContext == null) {
                throw new IllegalStateException("ServletContext is not found.");
            }

            ocmAnnotatedClassesResourcePath = servletContext.getInitParameter(BEANS_ANNOTATED_CLASSES_CONF_PARAM);
        }

        if (ocmAnnotatedClassesResourcePath == null) {
            throw new IllegalStateException("No content bean annotation class resource path found.");
        }

        try {
            annotatedClasses = ObjectConverterUtils.getAnnotatedClasses(classpathResourceScanner, StringUtils.split(ocmAnnotatedClassesResourcePath, ", \t\r\n"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        return annotatedClasses;
    }



}
