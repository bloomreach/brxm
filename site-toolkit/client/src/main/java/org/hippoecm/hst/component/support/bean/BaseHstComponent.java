/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.component.support.bean;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.servlet.ServletConfig;

import org.apache.commons.collections.list.TreeList;
import org.apache.commons.digester.Digester;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.manager.ObjectConverterImpl;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.query.HstQueryManagerImpl;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDirectory;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.content.beans.standard.HippoFacetSearch;
import org.hippoecm.hst.content.beans.standard.HippoFacetSelect;
import org.hippoecm.hst.content.beans.standard.HippoFixedDirectory;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.util.DefaultKeyValue;
import org.hippoecm.hst.util.KeyValue;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class BaseHstComponent extends GenericHstComponent {

    private static Logger log = LoggerFactory.getLogger(BaseHstComponent.class);

    public static final String BEANS_ANNOTATED_CLASSES_CONF_PARAM = "beans-annotated-classes";
    public static final String DEFAULT_BEANS_ANNOTATED_CLASSES_CONF = "/WEB-INF/beans-annotated-classes.xml";
    public static final String BEANS_REQUEST_CONTEXT_ATTR_NAME = BaseHstComponent.class.getName() + ".beans";
    public static final String QUERY_REQUEST_CONTEXT_ATTR_NAME = BaseHstComponent.class.getName() + ".query";

    private boolean beansInitialized;
    private ObjectConverter objectConverter;
    private HstQueryManager queryManager;

    public void init(ServletConfig servletConfig, ComponentConfiguration componentConfig) throws HstComponentException {
        super.init(servletConfig, componentConfig);
        if (!this.beansInitialized) {
            initBeansObjects();
        }
    }

    protected String getParameter(String name, HstRequest request) {
        return (String)this.getComponentConfiguration().getParameter(name, request.getRequestContext().getResolvedSiteMapItem());
    }
    
    protected  Map<String,String> getParameters(HstRequest request){
        return this.getComponentConfiguration().getParameters(request.getRequestContext().getResolvedSiteMapItem());
    }
    
    protected HippoBean getContentNode(HstRequest request) {
        ResolvedSiteMapItem resolvedSitemapItem = request.getRequestContext().getResolvedSiteMapItem();
        
        String base = PathUtils.normalizePath(resolvedSitemapItem.getHstSiteMapItem().getHstSiteMap().getSite().getContentPath());
        String relPath = PathUtils.normalizePath(resolvedSitemapItem.getRelativeContentPath());
        try {
            if(relPath == null || "".equals(relPath)) {
                return (HippoBean) getObjectBeanManager(request).getObject("/"+base);
            } else {
                return (HippoBean) getObjectBeanManager(request).getObject("/"+base+ "/" + relPath);
            }
        } catch (ObjectBeanManagerException e) {
            log.error("ObjectBeanManagerException. Return null : {}", e);
        }
        return null;
        
    }
    
    protected HippoBean getSiteContentBaseNode(HstRequest request) {
        ResolvedSiteMapItem resolvedSitemapItem = request.getRequestContext().getResolvedSiteMapItem();
        String base = PathUtils.normalizePath(resolvedSitemapItem.getHstSiteMapItem().getHstSiteMap().getSite().getContentPath());
        try {
            return (HippoBean) getObjectBeanManager(request).getObject("/"+base);
        } catch (ObjectBeanManagerException e) {
            log.error("ObjectBeanManagerException. Return null : {}", e);
        }
        return null;
    }
    
    protected HstQueryManager getQueryManager(){
       return this.queryManager;
    }
    
    protected ObjectBeanManager getObjectBeanManager(HstRequest request) {
        HstRequestContext requestContext = request.getRequestContext();
        ObjectBeanManager obm = (ObjectBeanManager) requestContext.getAttribute(BEANS_REQUEST_CONTEXT_ATTR_NAME);
        if (obm == null) {
            try {
                obm = new ObjectBeanManagerImpl(requestContext.getSession(), this.objectConverter);
                requestContext.setAttribute(BEANS_REQUEST_CONTEXT_ATTR_NAME, obm);
            } catch (UnsupportedRepositoryOperationException e) {
                throw new HstComponentException(e);
            } catch (RepositoryException e) {
                throw new HstComponentException(e);
            }
        }
        return obm;
    }
    
    /**
     * 
     * Facility method for sending a redirect to a SiteMapItemId.  
     * 
     * @param request the HstRequest
     * @param response the HstResponse
     * @param redirectToSiteMapItemId the sitemap item id to redirect to
     */
    // TODO Make use of the HstURLFactory : HSTTWO-474
    public void sendRedirect(HstRequest request, HstResponse response, String redirectToSiteMapItemId) {
        HstLinkCreator linkCreator = request.getRequestContext().getHstLinkCreator();
        HstSiteMap siteMap = request.getRequestContext().getResolvedSiteMapItem().getHstSiteMapItem().getHstSiteMap();
        HstLink link = linkCreator.create(siteMap.getSiteMapItemById(redirectToSiteMapItemId));

        StringBuffer url = new StringBuffer();
        for (String elem : link.getPathElements()) {
            String enc = response.encodeURL(elem);
            url.append("/").append(enc);
        }

        url.insert(0, request.getContextPath() + request.getServletPath());
        try {
            response.sendRedirect(url.toString());
        } catch (IOException e) {
            throw new HstComponentException("Could not redirect. ",e);
        }
    }
    
    private synchronized void initBeansObjects() {
        if (!this.beansInitialized) {
            this.objectConverter = getObjectConverter();
            this.queryManager = new HstQueryManagerImpl(this.objectConverter);
            this.beansInitialized = true;
        }
    }

    protected ObjectConverter getObjectConverter() {
        // builds ordered mapping from jcrPrimaryNodeType to class or interface(s).
        List<KeyValue<String, Class[]>> jcrPrimaryNodeTypeClassPairs = new TreeList();
        List<Class> annotatedClasses = getAnnotatedClassNames();
        for (Class c : annotatedClasses) {
            addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, c);
        }
        // below the default present mapped mappings
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoDocument.class);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFolder.class);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFacetSearch.class);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFacetSelect.class);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoDirectory.class);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFixedDirectory.class);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoHtml.class);
        // builds a fallback jcrPrimaryNodeType array.
        String[] fallBackJcrPrimaryNodeTypes = new String[] { "hippo:facetselect", "hippo:document" };
        ObjectConverter objectConverter = new ObjectConverterImpl(jcrPrimaryNodeTypeClassPairs,
                fallBackJcrPrimaryNodeTypes);
        return objectConverter;
    }

    private static void addJcrPrimaryNodeTypeClassPair(List<KeyValue<String, Class[]>> jcrPrimaryNodeTypeClassPairs,
            Class clazz) {
        String jcrPrimaryNodeType = null;

        if (clazz.isAnnotationPresent(Node.class)) {
            Node anno = (Node) clazz.getAnnotation(Node.class);
            jcrPrimaryNodeType = anno.jcrType();
        }

        if (jcrPrimaryNodeType == null) {
            throw new IllegalArgumentException("There's no annotation for jcrType in the class: " + clazz);
        }

        jcrPrimaryNodeTypeClassPairs.add(new DefaultKeyValue(jcrPrimaryNodeType, new Class[] { clazz }, true));
    }

    private List<Class> getAnnotatedClassNames() {
        List<String> classNames = new ArrayList<String>();
        List<Class> annotatedClasses = new ArrayList<Class>();

        String param = getServletConfig().getInitParameter(BEANS_ANNOTATED_CLASSES_CONF_PARAM);
        String ocmAnnotatedClassesResourcePath = (param != null ? param : DEFAULT_BEANS_ANNOTATED_CLASSES_CONF);
        InputStream in = null;

        try {
            in = new BufferedInputStream(getServletConfig().getServletContext().getResourceAsStream(
                    ocmAnnotatedClassesResourcePath));

            Digester digester = new Digester();
            digester.setValidating(false);
            digester.push(classNames);
            digester.addCallMethod("hst-content-beans/annotated-class", "add", 1);
            digester.addCallParam("hst-content-beans/annotated-class", 0);

            digester.parse(in);
        } catch (IOException e) {
            if (log.isWarnEnabled()) {
                log.warn("No ocm annotated classes configuration available: " + ocmAnnotatedClassesResourcePath);
            }
        } catch (SAXException e) {
            if (log.isWarnEnabled()) {
                log.warn("ocm annotated classes configuration is not valid: " + ocmAnnotatedClassesResourcePath);
            }
        } finally {
            if (in != null)
                try {
                    in.close();
                } catch (Exception ce) {
                }
        }

        for (String className : classNames) {
            Class clazz = null;
            try {
                clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
                annotatedClasses.add(clazz);
            } catch (Exception e) {
                if (log.isWarnEnabled()) {
                    log.warn("Skipped class registration into the mapper. Cannot load class: " + className);
                }
            }
        }
        return annotatedClasses;
    }
}
