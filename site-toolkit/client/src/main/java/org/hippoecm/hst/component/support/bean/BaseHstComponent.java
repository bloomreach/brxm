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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.servlet.ServletConfig;

import org.apache.commons.digester.Digester;
import org.hippoecm.hst.configuration.HstSite;
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

    public String getParameter(String name, HstRequest request) {
        return (String)this.getComponentConfiguration().getParameter(name, request.getRequestContext().getResolvedSiteMapItem());
    }
    
    public  Map<String,String> getParameters(HstRequest request){
        return this.getComponentConfiguration().getParameters(request.getRequestContext().getResolvedSiteMapItem());
    }
    
    public String getPublicRequestParameter(HstRequest request, String parameterName){
        Map<String, Object> namespaceLessParameters = request.getParameterMap("");
        Object o = namespaceLessParameters.get(parameterName);
        if(o instanceof String) {
            return (String)o;
        }
        return null;
    }
    

    public HstSite getHstSite(HstRequest request){
        return request.getRequestContext().getResolvedSiteMapItem().getHstSiteMapItem().getHstSiteMap().getSite();
    }
    
    public String getSiteContentBasePath(HstRequest request){
        return PathUtils.normalizePath(getHstSite(request).getContentPath());
    }
    
    
    /**
     * Use {@link BaseHstComponent#getContentBean(HstRequest)}
     */
    @Deprecated
    public HippoBean getContentNode(HstRequest request) {
        return this.getContentBean(request);
    }
    
    
    /**
     * When the <code>{@link ResolvedSiteMapItem}</code> belonging to the current requestUri has a relativeContentPath that points to an
     * existing jcr Node, a HippoBean wrapping this node is returned. When there is no relativeContentPath or the location does not exist,
     * <code>null</code> is returned
     * @param request
     * @return A <code>HippoBean</code> or <code>null</code> when there cannot be created a content bean for the resolvedSiteMapItem belonging to the current request
     */
    public HippoBean getContentBean(HstRequest request) {
        ResolvedSiteMapItem resolvedSiteMapItem = request.getRequestContext().getResolvedSiteMapItem();
        return this.getBeanForResolvedSiteMapItem(request, resolvedSiteMapItem);
    }
    
    /**
     * Use {@link BaseHstComponent#getSiteContentBaseBean(HstRequest)}
     */
    @Deprecated
    public HippoBean getSiteContentBaseNode(HstRequest request) {
        return this.getSiteContentBaseBean(request);
    }
    
    public HippoBean getSiteContentBaseBean(HstRequest request) {
        String base = getSiteContentBasePath(request);
        try {
            return (HippoBean) getObjectBeanManager(request).getObject("/"+base);
        } catch (ObjectBeanManagerException e) {
            log.error("ObjectBeanManagerException. Return null : {}", e);
        }
        return null;
    }
    
    /**
     * Return a <code>HippoBean</code> when it can be found for the relativeContentPath for the <code>{@link ResolvedSiteMapItem}</code>. If there is no
     * relativeContentPath available in the <code>{@link ResolvedSiteMapItem}</code>, or when the relativeContentPath does not point to an existing jcr node,
     * <code>null</code> will be returned
     * @param request
     * @param resolvedSiteMapItem
     * @return A <code>HippoBean</code> or <code>null</code> when there cannot be created a content bean for this resolvedSiteMapItem
     */
    public HippoBean getBeanForResolvedSiteMapItem(HstRequest request, ResolvedSiteMapItem resolvedSiteMapItem) {
        String base = getSiteContentBasePath(request);
        String relPath = PathUtils.normalizePath(resolvedSiteMapItem.getRelativeContentPath());
        if(relPath == null) {
            log.debug("Cannot return a content bean for relative path null for resolvedSitemapItem belonging to '{}'. Return null", resolvedSiteMapItem.getHstSiteMapItem().getId());
            return null;
        }
        try {
            if("".equals(relPath)) {
                return (HippoBean) getObjectBeanManager(request).getObject("/"+base);
            } else {
                return (HippoBean) getObjectBeanManager(request).getObject("/"+base+ "/" + relPath);
            }
        } catch (ObjectBeanManagerException e) {
            log.error("ObjectBeanManagerException. Return null : {}", e);
        }
        return null;
        
    }
    
    public HstQueryManager getQueryManager(){
       return this.queryManager;
    }
    
    public ObjectBeanManager getObjectBeanManager(HstRequest request) {
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

    public ObjectConverter getObjectConverter() {
        // builds ordered mapping from jcrPrimaryNodeType to class or interface(s).
        Map<String, Class> jcrPrimaryNodeTypeClassPairs = new HashMap<String, Class>();
        List<Class> annotatedClasses = getAnnotatedClassNames();
        for (Class c : annotatedClasses) {
            addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, c, false);
        }
        // below the default present mapped mappings
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoDocument.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFolder.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFacetSearch.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFacetSelect.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoDirectory.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFixedDirectory.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoHtml.class, true);
        // builds a fallback jcrPrimaryNodeType array.
        String[] fallBackJcrNodeTypes = getFallBackJcrNodeTypes();
        ObjectConverter objectConverter = new ObjectConverterImpl(jcrPrimaryNodeTypeClassPairs,
                fallBackJcrNodeTypes);
        return objectConverter;
    }

    /**
     * If you want other fallbacktypes, override this method. Note, that the fallback types are tried in order that the
     * array is. A fallback type is suited for creating a bean for the node if: node.isNodeType(fallBackJcrNodeType) returns true.
     * @return String array containing the fallback types
     */
    protected String[] getFallBackJcrNodeTypes(){
        return new String[] { "hippo:facetselect", "hippo:document" };
    }
    
    private static void addJcrPrimaryNodeTypeClassPair(Map<String, Class> jcrPrimaryNodeTypeClassPairs,
            Class clazz, boolean builtinType) {
        String jcrPrimaryNodeType = null;

        if (clazz.isAnnotationPresent(Node.class)) {
            Node anno = (Node) clazz.getAnnotation(Node.class);
            jcrPrimaryNodeType = anno.jcrType();
        }

        if(jcrPrimaryNodeTypeClassPairs.get(jcrPrimaryNodeType) != null) {
            if(builtinType) {
                log.debug("Builtin annotated class for primary type '{}' is overridden. Builtin version is ignored", jcrPrimaryNodeType);
            } else {
                log.warn("Annotated class for primarytype '{}' is duplicate. Skipping this one.", jcrPrimaryNodeType);
            }
            return;
        }
        
        if (jcrPrimaryNodeType == null) {
            throw new IllegalArgumentException("There's no annotation for jcrType in the class: " + clazz);
        }

        jcrPrimaryNodeTypeClassPairs.put(jcrPrimaryNodeType,clazz);
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
