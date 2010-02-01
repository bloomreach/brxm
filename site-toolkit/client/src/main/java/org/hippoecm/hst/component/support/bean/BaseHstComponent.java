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

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.servlet.ServletConfig;

import org.apache.commons.digester.Digester;
import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.container.HstContainerServlet;
import org.hippoecm.hst.content.beans.ContentNodeBinder;
import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.manager.ObjectConverterImpl;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManager;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManagerImpl;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.standard.HippoAsset;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDirectory;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.content.beans.standard.HippoFacetSelect;
import org.hippoecm.hst.content.beans.standard.HippoFixedDirectory;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.hippoecm.hst.content.beans.standard.HippoImage;
import org.hippoecm.hst.content.beans.standard.HippoMirror;
import org.hippoecm.hst.content.beans.standard.HippoRequest;
import org.hippoecm.hst.content.beans.standard.HippoResource;
import org.hippoecm.hst.content.beans.standard.facetnavigation.HippoFacetNavigation;
import org.hippoecm.hst.content.beans.standard.facetnavigation.HippoFacetResult;
import org.hippoecm.hst.content.beans.standard.facetnavigation.HippoFacetSearch;
import org.hippoecm.hst.content.beans.standard.facetnavigation.HippoFacetSubNavigation;
import org.hippoecm.hst.content.beans.standard.facetnavigation.HippoFacetsAvailableNavigation;
import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstComponentFatalException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.HstResponseUtils;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * A base HstComponent implementation to provide some facility methods for accessing content node POJO objects,
 * {@link ObjectBeanManager}, request parameters, query manager, etc.
 * <P>
 * This implementation enables developers to make use of HST Content Bean's {@link ObjectBeanManager}
 * which provides a simple object-content mapping solution.
 * To use {@link ObjectBeanManager}, you can invoke {@link #getObjectBeanManager(HstRequest)}, which retrieves
 * a JCR session from {@link HstRequestContext#getSession()} internally.
 * </P>
 * 
 * <P>
 * When you need to persist beans through ecm workflow, you can use {@link #getWorkflowPersistenceManager(Session)}. Make
 * sure that the jcr session you use as parameter is obtained through {@link #getPersistableSession(HstRequest)} or 
 * {@link #getPersistableSession(HstRequest, Credentials)}.
 * </P>
 * 
 * 
 * @version $Id$
 */
public class BaseHstComponent extends GenericHstComponent {

    public static final String IS_PREVIEW_ATTRIBUTE = BaseHstComponent.class.getName() + ".isPreview";

    private static Logger log = LoggerFactory.getLogger(BaseHstComponent.class);

    public static final String BEANS_ANNOTATED_CLASSES_CONF_PARAM = "beans-annotated-classes";
    public static final String DEFAULT_BEANS_ANNOTATED_CLASSES_CONF = "/WEB-INF/beans-annotated-classes.xml";

    public static final String DEFAULT_WRITABLE_USERNAME_PROPERTY = "writable.repository.user.name";
    public static final String DEFAULT_WRITABLE_PASSWORD_PROPERTY = "writable.repository.password";
    

    protected boolean beansInitialized;
    protected ObjectConverter objectConverter;
    protected HstQueryManager queryManager;

    public void init(ServletConfig servletConfig, ComponentConfiguration componentConfig) throws HstComponentException {
        super.init(servletConfig, componentConfig);
        if (!this.beansInitialized) {
            initBeansObjects() ;
        }
    }

    /**
     * Returns resolved parameter from HstComponentConfiguration : resolved means that possible property placeholders like
     * ${1} or ${year}, where the first refers to the first wildcard matcher in a resolved sitemap item, and the latter
     * to a resolved parameter in the resolved HstSiteMapItem
     * 
     * The parameter map used has inherited parameters from ancestor components, which have precedence over child components) 
     * 
     * @param name
     * @param request
     * @return the resolved parameter value for this name, or <code>null</null> if not present
     */
    public String getParameter(String name, HstRequest request) {
        return (String)this.getComponentConfiguration().getParameter(name, request.getRequestContext().getResolvedSiteMapItem());
    }
    
    /**
     * See {@link #getParameter(String, HstRequest)}, where we now return all resolved parameters (thus with inheritance of 
     * ancestor components)
     * @param request
     * @return Map of all parameters, and when no parameters present, return empty map.
     */
    public  Map<String,String> getParameters(HstRequest request){
        return this.getComponentConfiguration().getParameters(request.getRequestContext().getResolvedSiteMapItem());
    }
    
    /**
     * See {@link #getParameter(String, HstRequest)}, but now, only resolved parameters directly on the HstComponent are taken into
     * acoount: in other words, no inheritance of parameters is applied
     * @param name
     * @param request
     * @return the resolved parameter value for this name, or <code>null</null> if not present
     */
    public String getLocalParameter(String name, HstRequest request) {
        return (String)this.getComponentConfiguration().getLocalParameter(name, request.getRequestContext().getResolvedSiteMapItem());
    }
    
    /**
     * See {@link #getParameters(HstRequest)}, but now, only resolved parameter map of parameters is returned that is directly on
     * the HstComponenConfiguration, thus, no inheritance is applied
     * @param request
     * @return Map of all parameters, and when no parameters present, return empty map.
     */
    public  Map<String,String> getLocalParameters(HstRequest request){
        return this.getComponentConfiguration().getLocalParameters(request.getRequestContext().getResolvedSiteMapItem());
    }
    
    public String getPublicRequestParameter(HstRequest request, String parameterName) {
        String contextNamespaceReference = request.getRequestContext().getContextNamespace();
        
        if (contextNamespaceReference == null) {
            contextNamespaceReference = "";
        }
        
        Map<String, String []> namespaceLessParameters = request.getParameterMap(contextNamespaceReference);
        String [] paramValues = namespaceLessParameters.get(parameterName);
        
        if (paramValues != null && paramValues.length > 0) {
            return paramValues[0];
        }
        
        return null;
    }
    

    public HstSite getHstSite(HstRequest request){
        return request.getRequestContext().getResolvedSiteMapItem().getHstSiteMapItem().getHstSiteMap().getSite();
    }
    
    /**
     * @param request
     * @return the jcr path relative to the root (not starting with / thus)
     */
    public String getSiteContentBasePath(HstRequest request){
        return PathUtils.normalizePath(getHstSite(request).getContentPath());
    }
    
    public boolean isPreview(HstRequest request) {
        HstRequestContext hstRequestContext = request.getRequestContext();
        Boolean isPreview = (Boolean) hstRequestContext.getAttribute(IS_PREVIEW_ATTRIBUTE);
        
        if (isPreview == null) {
            String previewRepositoryEntryPath = request.getRequestContext().getContainerConfiguration().getString(ContainerConstants.PREVIEW_REPOSITORY_ENTRY_PATH, "");
            isPreview = (getSiteContentBasePath(request).startsWith(previewRepositoryEntryPath) ? Boolean.TRUE : Boolean.FALSE);
            hstRequestContext.setAttribute(IS_PREVIEW_ATTRIBUTE, isPreview);
        }
        
        return isPreview.booleanValue();
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
     * @see {@link #getContentBean(HstRequest)} but only returns the bean if the found content bean is of type {@code beanMappingClass}. When the bean cannot be found, or is not of type 
     * {@code beanMappingClass}, <code>null</code> is returned 
     * @param request
     * @param beanMappingClass the class of the bean that you expect
     * @return A HippoBean of {@code beanMappingClass} or <code>null</code> if bean cannot be found or is of a different class
     */
    public <T extends HippoBean> T getContentBean(HstRequest request, Class<T> beanMappingClass) {
        ResolvedSiteMapItem resolvedSiteMapItem = request.getRequestContext().getResolvedSiteMapItem();
        HippoBean bean = this.getBeanForResolvedSiteMapItem(request, resolvedSiteMapItem);
        if(bean == null) {
            return null;
        }
        if(!beanMappingClass.isAssignableFrom(bean.getClass())) {
            log.debug("Expected bean of type '{}' but found of type '{}'. Return null.", beanMappingClass.getName(), bean.getClass().getName());
            return null;
        }
        return (T)bean;
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
     * @param request
     * @return the root gallery HippoFolderBean at <code>/content/gallery</code> and <code>null</code> if it does not exist 
     */
    public HippoFolderBean getGalleryBaseBean(HstRequest request){
        try {
            HippoBean gallery = (HippoBean)this.getObjectBeanManager(request).getObject("/content/gallery");
            if(gallery instanceof HippoFolderBean) {
                return (HippoFolderBean)gallery;
            } else {
                log.warn("Gallery base folder not of type folder. Cannot return folder bean for it. Return null");
            }
        } catch (ObjectBeanManagerException e) {
           log.warn("Cannot find the root Gallery folder. Return null");
        }
        return null;
    }
    
    /**
     * @param request
     * @return the root asset HippoFolderBean at <code>/content/assets</code> and null if it does not exist 
     */
    public HippoFolderBean getAssetBaseBean(HstRequest request){
        try {
            HippoBean assets = (HippoBean)this.getObjectBeanManager(request).getObject("/content/assets");
            if(assets instanceof HippoFolderBean) {
                return (HippoFolderBean)assets;
            } else {
                log.warn("Assets base folder not of type folder. Cannot return folder bean for it. Return null");
            }
        } catch (ObjectBeanManagerException e) {
           log.warn("Cannot find the root Asset folder. Return null");
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
        try {
            HstRequestContext requestContext = request.getRequestContext();
            return new ObjectBeanManagerImpl(requestContext.getSession(), this.objectConverter);
        } catch (UnsupportedRepositoryOperationException e) {
            throw new HstComponentException(e);
        } catch (RepositoryException e) {
            throw new HstComponentException(e);
        }
    }
    
    /**
     * This returns the client ComponentManager if one is configured with its default name. If set on the context with a different
     * attribute name, you need to fetch it yourself with a different attr name
     * @return the client ComponentManager or <code>null</code> if none configured 
     */
    public ComponentManager getDefaultClientComponentManager(){
        ComponentManager clientComponentManager = HstContainerServlet.getClientComponentManager(getServletConfig());
        if(clientComponentManager == null) {
            log.warn("Cannot get a client component manager from servlet context for attr name '{}'", HstContainerServlet.CLIENT_COMPONENT_MANANGER_DEFAULT_CONTEXT_ATTRIBUTE_NAME);
        }
        return  clientComponentManager;
    }
    
    /**
     * 
     * Facility method for sending a redirect to a SiteMapItemId.  
     * 
     * @param request the HstRequest
     * @param response the HstResponse
     * @param redirectToSiteMapItemId the sitemap item id to redirect to
     */
     /**
     * Use {@link sendRedirect(String, HstRequest, HstResponse) }
     */
    @Deprecated
    public void sendRedirect(HstRequest request, HstResponse response, String redirectToSiteMapItemId) {
        HstLinkCreator linkCreator = request.getRequestContext().getHstLinkCreator();
        HstSiteMap siteMap = request.getRequestContext().getResolvedSiteMapItem().getHstSiteMapItem().getHstSiteMap();
        HstLink link = linkCreator.create(siteMap.getSiteMapItemById(redirectToSiteMapItemId));

        if(link == null) {
            throw new HstComponentException("Can not redirect.");
        }
        String urlString = null;
        urlString = link.toUrlForm(request, response, false);
        
        if(urlString == null) {
            throw new HstComponentException("Can not redirect.");
        }
        
        try {
            response.sendRedirect(urlString);
        } catch (IOException e) {
            throw new HstComponentException("Could not redirect. ",e);
        }
    }
    
    /**
     * 
     * Facility method for sending a redirect to a SiteMapItemId.  
     * 
     * @param path the sitemap path you want to redirect to 
     * @param request the HstRequest
     * @param response the HstResponse
     */
    public void sendRedirect(String path, HstRequest request, HstResponse response) {
        HstResponseUtils.sendRedirect(request, response, path);
    }
    
    /**
     * 
     * Facility method for sending a redirect to a SiteMapItemId.  
     * 
     * @param path the sitemap path you want to redirect to 
     * @param request the HstRequest
     * @param response the HstResponse
     */
    public void sendRedirect(String path, HstRequest request, HstResponse response, Map<String, String []> queryParams) {
        HstResponseUtils.sendRedirect(request, response, path, queryParams);
    }
    
    /**
     * 
     * Facility method for sending a redirect to a SiteMapItemId.  
     * 
     * @param path the sitemap path you want to redirect to 
     * @param request the HstRequest
     * @param response the HstResponse
     * @param characterEncoding character encoding for query parameters
     */
    public void sendRedirect(String path, HstRequest request, HstResponse response, Map<String, String []> queryParams, String characterEncoding) {
        HstResponseUtils.sendRedirect(request, response, path, queryParams, characterEncoding);
    }
    
    private synchronized void initBeansObjects() throws HstComponentException{
        if (!this.beansInitialized) {
            this.objectConverter = getObjectConverter();
            
            ComponentManager compMngr = HstServices.getComponentManager();
            HstQueryManagerFactory hstQueryManagerFactory = (HstQueryManagerFactory)compMngr.getComponent(HstQueryManagerFactory.class.getName());
            this.queryManager = hstQueryManagerFactory.createQueryManager(this.objectConverter);
            this.beansInitialized = true;
        }
    }

    public ObjectConverter getObjectConverter() throws HstComponentException {
        // builds ordered mapping from jcrPrimaryNodeType to class or interface(s).
        Map<String, Class<? extends HippoBean>> jcrPrimaryNodeTypeClassPairs = new HashMap<String, Class<? extends HippoBean>>();
        List<Class<? extends HippoBean>> annotatedClasses = getAnnotatedClassNames();
        for (Class<? extends HippoBean> c : annotatedClasses) {
            addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, c, false) ;
        }
        // below the default present mapped mappings
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoDocument.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFolder.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoMirror.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFacetSelect.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoDirectory.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFixedDirectory.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoHtml.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoResource.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoRequest.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoAsset.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoImage.class, true);
        
        // facet navigation parts:
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFacetSearch.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFacetNavigation.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFacetsAvailableNavigation.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFacetSubNavigation.class, true);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFacetResult.class, true);
        
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
        return new String[] { "hippo:facetselect","hippo:mirror","hippostd:directory","hippostd:folder" , "hippo:resource", "hippo:request", "hippostd:html", "hippo:document" };
    }
    
    private static void addJcrPrimaryNodeTypeClassPair(Map<String, Class<? extends HippoBean>> jcrPrimaryNodeTypeClassPairs,
            Class clazz, boolean builtinType) throws HstComponentException {
        String jcrPrimaryNodeType = null;

        if (clazz.isAnnotationPresent(Node.class)) {
            Node anno = (Node) clazz.getAnnotation(Node.class);
            jcrPrimaryNodeType = anno.jcrType();
        }

        if(jcrPrimaryNodeTypeClassPairs.get(jcrPrimaryNodeType) != null) {
            if(builtinType) {
                log.debug("Builtin annotated class for primary type '{}' is overridden. Builtin version is ignored", jcrPrimaryNodeType);
            } else {
                throw new HstComponentFatalException("Annotated class for primarytype '"+jcrPrimaryNodeType+"' is duplicate. " +
                		"You might have configured a bean in 'beans-annotated-classes.xml' that does not have a annotation for the jcrType and" +
                		"inherits the jcrType from the bean it extends, resulting in 2 beans with the same jcrType. Correct your beans.");
            }
            return;
        }
        
        if (jcrPrimaryNodeType == null) {
            throw new IllegalArgumentException("There's no annotation for jcrType in the class: " + clazz);
        }

        jcrPrimaryNodeTypeClassPairs.put(jcrPrimaryNodeType,clazz);
    }

    /**
     * when you want to inject specific component only custom annotated classes override this method
     * 
     * This method is only called during the init() phase of a component     
	 * @return List of annotated classes, and if there are none, return an empty list
     */
    protected List<Class<? extends HippoBean>> getLocalAnnotatedClasses() {
        return new ArrayList<Class<? extends HippoBean>>();
    }
    
    private List<Class<? extends HippoBean>> getAnnotatedClassNames() {
        List<String> classNames = new ArrayList<String>();
        List<Class<? extends HippoBean>> annotatedClasses = new ArrayList<Class<? extends HippoBean>>();

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
        
        List<Class<? extends HippoBean>> localAnnotatedClasses = getLocalAnnotatedClasses();
        if(localAnnotatedClasses != null) {
            for(Class<? extends HippoBean> localClass : localAnnotatedClasses) {
                if(classNames.contains(localClass.getName())) {
                    log.warn("local added class '{}' already present. Skipping", localClass.getName());
                } 
                else {
                    annotatedClasses.add(localClass); 
                }
            }
        }
        return annotatedClasses;
    }
    
    /**
     * Creates a persistable JCR session with the default credentials
     * <P>
     * <EM>Note: The client should invoke <CODE>logout()</CODE> method on the session after use.</EM>
     * </P>
     * <P>
     * Internally, {@link javax.jcr.Session#impersonate(Credentials)} method will be used to create a
     * persistable JCR session. The method is invoked on the session from the session pooling repository.
     * </P>
     * @param request
     * @return
     */
    protected Session getPersistableSession(HstRequest request) throws RepositoryException {
        ContainerConfiguration config = request.getRequestContext().getContainerConfiguration();
        String username = config.getString(DEFAULT_WRITABLE_USERNAME_PROPERTY);
        String password = config.getString(DEFAULT_WRITABLE_PASSWORD_PROPERTY);
        return getPersistableSession(request, new SimpleCredentials(username, password.toCharArray()));
    }
    
    /**
     * Creates a persistable JCR session with provided credentials.
     * <P>
     * <EM>Note: The client should invoke <CODE>logout()</CODE> method on the session after use.</EM>
     * </P>
     * <P>
     * Internally, {@link javax.jcr.Session#impersonate(Credentials)} method will be used to create a
     * persistable JCR session. The method is invoked on the session from the session pooling repository.
     * </P>
     * @param request
     * @return
     */
    protected Session getPersistableSession(HstRequest request, Credentials credentials) throws RepositoryException {
        return request.getRequestContext().getSession().impersonate(credentials);
    }
    
    /**
     * Returns a <CODE>WorkflowPersistenceManager</CODE> instance.
     * @param session make sure this is a persistable session, obtained through {@link #getPersistableSession(HstRequest) or {@link #getPersistableSession(HstRequest, Credentials)}}
     * @return
     */
    protected WorkflowPersistenceManager getWorkflowPersistenceManager(Session session) {
        return getWorkflowPersistenceManager(session, null);
    }
    
    /**
     * Returns a <CODE>WorkflowPersistenceManager</CODE> instance with custom binders map.
     * @param session make sure this is a persistable session, obtained through {@link #getPersistableSession(HstRequest) or {@link #getPersistableSession(HstRequest, Credentials)}}
     * @param contentNodeBinders
     * @return
     */
    protected WorkflowPersistenceManager getWorkflowPersistenceManager(Session session, Map<String, ContentNodeBinder> contentNodeBinders) {
        WorkflowPersistenceManagerImpl wpm = new WorkflowPersistenceManagerImpl(session, this.objectConverter, contentNodeBinders);
        return wpm;
    }
}
