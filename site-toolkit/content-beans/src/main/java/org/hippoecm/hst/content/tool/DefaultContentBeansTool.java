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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerException;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.ClasspathResourceScanner;
import org.hippoecm.hst.util.ObjectConverterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DefaultContentBeansTool
 */
public class DefaultContentBeansTool implements ContentBeansTool {

    private static Logger log = LoggerFactory.getLogger(DefaultContentBeansTool.class);

    public static final String BEANS_ANNOTATED_CLASSES_CONF_PARAM = "hst-beans-annotated-classes";

    private final ObjectConverter objectConverter;

    private final HstQueryManagerFactory queryManagerFactory;

    public DefaultContentBeansTool(HstQueryManagerFactory queryManagerFactory) {
        this.queryManagerFactory = queryManagerFactory;
        ClasspathResourceScanner classpathResourceScanner = HstServices.getComponentManager().getComponent(ClasspathResourceScanner.class.getName());
        List<Class<? extends HippoBean>> annotatedClasses = getAnnotatedClasses(classpathResourceScanner);
        objectConverter = ObjectConverterUtils.createObjectConverter(annotatedClasses);
    }

    public ObjectConverter getObjectConverter() {
        return objectConverter;
    }

    public ObjectBeanManager getObjectBeanManager() {
        try {
            HstRequestContext requestContext = RequestContextProvider.get();

            if (requestContext == null) {
                throw new IllegalStateException("HstRequestContext is not set in handler.");
            }

            return new ObjectBeanManagerImpl(requestContext.getSession(), getObjectConverter());
        } catch (UnsupportedRepositoryOperationException e) {
            throw new HstSiteMapItemHandlerException(e);
        } catch (RepositoryException e) {
            throw new HstSiteMapItemHandlerException(e);
        }
    }

    public HstQueryManager getQueryManager() throws RepositoryException {
        HstRequestContext requestContext = RequestContextProvider.get();

        if (requestContext == null) {
            throw new IllegalStateException("HstRequestContext is not set in handler.");
        }

        return getQueryManager(requestContext.getSession());
    }

    public HstQueryManager getQueryManager(Session session) throws RepositoryException {
        HstQueryManager queryManager = null;
        queryManager = queryManagerFactory.createQueryManager(session, getObjectConverter());
        return queryManager;
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
            throw new HstSiteMapItemHandlerException(e);
        }

        return annotatedClasses;
    }



}
