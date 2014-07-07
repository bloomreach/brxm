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

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.request.HstRequestContext;
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

    public DefaultContentBeansTool(HstQueryManagerFactory queryManagerFactory) {
        this.queryManagerFactory = queryManagerFactory;
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
        HstRequestContext requestContext = RequestContextProvider.get();

        if (requestContext == null) {
            throw new IllegalStateException("HstRequestContext is not set in handler.");
        }

        List<Class<? extends HippoBean>> annotatedClasses = null;
        String ocmAnnotatedClassesResourcePath = requestContext.getServletContext().getInitParameter(BEANS_ANNOTATED_CLASSES_CONF_PARAM);

        try {
            annotatedClasses = ObjectConverterUtils.getAnnotatedClasses(classpathResourceScanner, StringUtils.split(ocmAnnotatedClassesResourcePath, ", \t\r\n"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        return annotatedClasses;
    }



}
