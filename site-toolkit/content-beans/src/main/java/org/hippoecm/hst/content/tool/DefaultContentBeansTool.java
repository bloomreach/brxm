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
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerException;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.ClasspathResourceScanner;
import org.hippoecm.hst.util.ObjectConverterUtils;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DefaultContentBeansTool
 */
public class DefaultContentBeansTool implements ContentBeansTool {

    private static Logger log = LoggerFactory.getLogger(DefaultContentBeansTool.class);

    public static final String BEANS_ANNOTATED_CLASSES_CONF_PARAM = "hst-beans-annotated-classes";

    private ClasspathResourceScanner classpathResourceScanner;

    private ObjectConverter objectConverter;

    private HstQueryManagerFactory queryManagerFactory;

    public DefaultContentBeansTool() {
    }

    public ClasspathResourceScanner getClasspathResourceScanner() {
        if (classpathResourceScanner == null) {
            ComponentManager compMgr = HstServices.getComponentManager();

            if (compMgr != null) {
                classpathResourceScanner = compMgr.getComponent(ClasspathResourceScanner.class.getName());
            }
        }

        return classpathResourceScanner;
    }

    public void setClasspathResourceScanner(ClasspathResourceScanner classpathResourceScanner) {
        this.classpathResourceScanner = classpathResourceScanner;
    }

    public ObjectConverter getObjectConverter() {
        if (objectConverter == null) {
            List<Class<? extends HippoBean>> annotatedClasses = getAnnotatedClasses();
            objectConverter = ObjectConverterUtils.createObjectConverter(annotatedClasses);
        }

        return objectConverter;
    }

    public void setObjectConverter(ObjectConverter objectConverter) {
        this.objectConverter = objectConverter;
    }

    public HstQueryManagerFactory getQueryManagerFactory() {
        if (queryManagerFactory == null) {
            ComponentManager compMgr = HstServices.getComponentManager();

            if (compMgr != null) {
                queryManagerFactory = (HstQueryManagerFactory) compMgr.getComponent(HstQueryManagerFactory.class.getName());
            }
        }

        return queryManagerFactory;
    }

    public void setQueryManagerFactory(HstQueryManagerFactory queryManagerFactory) {
        this.queryManagerFactory = queryManagerFactory;
    }

    public HippoBean getResolvedContentBean() {
        HstRequestContext requestContext = RequestContextProvider.get();

        if (requestContext == null || requestContext.getResolvedSiteMapItem() == null) {
            return null;
        }

        return getBeanForResolvedSiteMapItem(requestContext.getResolvedSiteMapItem());
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

    public String getSiteContentBasePath() {
        HstRequestContext requestContext = RequestContextProvider.get();

        if (requestContext == null) {
            throw new IllegalStateException("HstRequestContext is not set in handler.");
        }

        return PathUtils.normalizePath(requestContext.getResolvedMount().getMount().getContentPath());
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
        queryManager = getQueryManagerFactory().createQueryManager(session, getObjectConverter());
        return queryManager;
    }

    private List<Class<? extends HippoBean>> getAnnotatedClasses() {
        HstRequestContext requestContext = RequestContextProvider.get();

        if (requestContext == null) {
            throw new IllegalStateException("HstRequestContext is not set in handler.");
        }

        List<Class<? extends HippoBean>> annotatedClasses = null;

        String ocmAnnotatedClassesResourcePath = requestContext.getServletContext().getInitParameter(BEANS_ANNOTATED_CLASSES_CONF_PARAM);

        try {
            ClasspathResourceScanner scanner = getClasspathResourceScanner();
            annotatedClasses = ObjectConverterUtils.getAnnotatedClasses(scanner, StringUtils.split(ocmAnnotatedClassesResourcePath, ", \t\r\n"));
        } catch (Exception e) {
            throw new HstSiteMapItemHandlerException(e);
        }

        return annotatedClasses;
    }

    private HippoBean getBeanForResolvedSiteMapItem(ResolvedSiteMapItem resolvedSiteMapItem) {
        String base = getSiteContentBasePath();
        String relPath = PathUtils.normalizePath(resolvedSiteMapItem.getRelativeContentPath());

        if (relPath == null) {
            log.debug("Cannot return a content bean for relative path null for resolvedSitemapItem belonging to '{}'. Return null", resolvedSiteMapItem.getHstSiteMapItem().getId());
            return null;
        }

        try {
            if ("".equals(relPath)) {
                return (HippoBean) getObjectBeanManager().getObject("/" + base);
            } else {
                return (HippoBean) getObjectBeanManager().getObject("/" + base+ "/" + relPath);
            }
        } catch (ObjectBeanManagerException e) {
            log.error("ObjectBeanManagerException. Return null : {}", e);
        }

        return null;
    }

}
