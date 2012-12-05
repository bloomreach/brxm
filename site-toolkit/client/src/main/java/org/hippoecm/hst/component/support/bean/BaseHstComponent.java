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

import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.servlet.ServletContext;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.component.support.spring.util.MetadataReaderClasspathResourceScanner;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.container.HstFilter;
import org.hippoecm.hst.content.beans.ContentNodeBinder;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManager;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManagerImpl;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.ClasspathResourceScanner;
import org.hippoecm.hst.util.HstResponseUtils;
import org.hippoecm.hst.util.ObjectConverterUtils;
import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.hst.utils.ParameterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    

    private static Logger log = LoggerFactory.getLogger(BaseHstComponent.class);

    public static final String BEANS_ANNOTATED_CLASSES_CONF_PARAM = "hst-beans-annotated-classes";
    public static final String DEFAULT_BEANS_ANNOTATED_CLASSES_CONF = "/WEB-INF/beans-annotated-classes.xml";
    public static final String OBJECT_CONVERTER_CONTEXT_ATTRIBUTE = BaseHstComponent.class.getName() + ".objectConverter";
    
    private static final String BEANS_ANNOTATED_CLASSES_CONF_PARAM_ERROR_MSG = 
        "Please check HST-2 Content Beans Annotation configuration as servlet context parameter.\n" +
        "You can set a servlet context parameter named '" + BEANS_ANNOTATED_CLASSES_CONF_PARAM + "' with xml or classes location filter.\n" +
        "For example, '" + DEFAULT_BEANS_ANNOTATED_CLASSES_CONF + "' or 'classpath*:org/examples/beans/**/*.class'";

    protected boolean beansInitialized;
    protected ObjectConverter objectConverter;
    protected HstQueryManagerFactory hstQueryManagerFactory;
    
    private ServletContext servletContext;

    public void init(ServletContext servletContext, ComponentConfiguration componentConfig) throws HstComponentException {
        super.init(servletContext, componentConfig);
        this.servletContext = servletContext;
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
    
    /**
     * A public request parameter is a request parameter that is not namespaced. Thus for example ?foo=bar. Typically,
     * a namespaced request parameter for example looks like ?r1_r4:foo=bar. 
     * Public request parameters are used when some parameter from some hst component needs to be readable by another hst 
     * component. For example when you have a search box in the top of your webpage. The input value there should be
     * readable by the center content block displaying the search results. In that case, this method can be used
     * to fetch the public request parameter. Also see {@link #getPublicRequestParameters(HstRequest, String)}
     * @param request
     * @param parameterName
     * @return The public request parameter for parameterName. If there are multiple values, the first one is returned. If no value, <code>null</code> is returned
     */
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
    
    /**
     * Also see {@link #getPublicRequestParameter(HstRequest, String)}. 
     * 
     * @param request
     * @param parameterName
     * @return The public request parameters String array for parameterName. If no values for parameterName found, <code>new String[0]</code> is returned
     */
    public String[] getPublicRequestParameters(HstRequest request, String parameterName) {
        String contextNamespaceReference = request.getRequestContext().getContextNamespace();
        
        if (contextNamespaceReference == null) {
            contextNamespaceReference = "";
        }
        
        Map<String, String []> namespaceLessParameters = request.getParameterMap(contextNamespaceReference);
        String [] paramValues = namespaceLessParameters.get(parameterName);
        
        if (paramValues != null && paramValues.length > 0) {
            return paramValues;
        }
        return ArrayUtils.EMPTY_STRING_ARRAY;
    }
    

    public HstSite getHstSite(HstRequest request){
        return request.getRequestContext().getResolvedSiteMapItem().getHstSiteMapItem().getHstSiteMap().getSite();
    }

    public ResolvedMount getResolvedMount(HstRequest request){
        return request.getRequestContext().getResolvedMount();
    }
    
    public Mount getMount(HstRequest request){
        return getResolvedMount(request).getMount();
    }
    
    /**
     * @param request
     * @return the jcr path relative to the root (not starting with / thus)
     */
    public String getSiteContentBasePath(HstRequest request){
        return PathUtils.normalizePath(request.getRequestContext().getResolvedMount().getMount().getContentPath());
    }
    
    /**
     * @return <code>true</code> when this request is matched to a preview site
     * @see Mount#isPreview()
     */
    public boolean isPreview(HstRequest request) {
    	return request.getRequestContext().isPreview();
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

    /**
     * @param request the {@link HstRequest}
     * @return the {@link HstQueryManager}
     * @see {@link #getQueryManager(HstRequestContext)} and {@link #getQueryManager(Session)}
     */
    public HstQueryManager getQueryManager(HstRequest request){
        return getQueryManager(request.getRequestContext());
    }
    
    /**
     * 
     * @param  ctx the {@link HstRequestContext}
     * @return the {@link HstQueryManager}
     * @see  {@link #getQueryManager(HstRequest)} and {@link #getQueryManager(Session)}
     */
    public HstQueryManager getQueryManager(HstRequestContext ctx) {
       try {
            return getQueryManager(ctx.getSession());
        } catch (RepositoryException e) {
            log.error("Unable to get a queryManager", e);
        }
        return null;
    }
    
    /**
     * @param  session the {@link Session}
     * @return the {@link HstQueryManager}
     * @see {@link #getQueryManager(HstRequestContext)} and {@link #getQueryManager(HstRequest)}
     */
    public HstQueryManager getQueryManager(Session session) {
        HstQueryManager queryManager = null;
        queryManager = hstQueryManagerFactory.createQueryManager(session, this.objectConverter);
        return queryManager;
    }
    
    public ObjectBeanManager getObjectBeanManager(HstRequest request) {
        try {
            HstRequestContext requestContext = request.getRequestContext();
            return new ObjectBeanManagerImpl(requestContext.getSession(), getObjectConverter());
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
        ComponentManager clientComponentManager = HstFilter.getClientComponentManager(servletContext);
        if(clientComponentManager == null) {
            log.warn("Cannot get a client component manager from servlet context for attr name '{}'", HstFilter.CLIENT_COMPONENT_MANANGER_DEFAULT_CONTEXT_ATTRIBUTE_NAME);
        }
        return  clientComponentManager;
    }
    
    /**
     * <p>
     * Facility method for sending a redirect to a sitemap path. You do not have to take into account the context path or {@link Mount} path
     * </p>
     * 
     * @see HstResponse#sendRedirect(String)
     * @param path the sitemap path you want to redirect to 
     * @param request the HstRequest
     * @param response the HstResponse
     */
    public void sendRedirect(String path, HstRequest request, HstResponse response) {
        HstResponseUtils.sendRedirect(request, response, path);
    }
    
    /**
     * <p>
     * Facility method for sending a redirect to a sitemap path including query params. You do not have to take into account the context path or {@link Mount} path
     * </p>
     * @see HstResponse#sendRedirect(String)
     * @param path the sitemap path you want to redirect to 
     * @param request the HstRequest
     * @param queryParams query parameters to append to the redirection url
     * @param response the HstResponse
     */
    public void sendRedirect(String path, HstRequest request, HstResponse response, Map<String, String []> queryParams) {
        HstResponseUtils.sendRedirect(request, response, path, queryParams);
    }
    
    /**
    * <p>
     * Facility method for sending a redirect to a sitemap path including query params and characterEncoding. You do not have to take into account the context path or {@link Mount} path
     * </p>
     * 
     * @see HstResponse#sendRedirect(String)
     * @param path the sitemap path you want to redirect to 
     * @param request the HstRequest
     * @param queryParams query parameters to append to the redirection url
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
            if (compMngr != null) {
                hstQueryManagerFactory = (HstQueryManagerFactory)compMngr.getComponent(HstQueryManagerFactory.class.getName());
            }
            
            this.beansInitialized = true;
        }
    }
    
    public ObjectConverter getObjectConverter() throws HstComponentException {
        // builds ordered mapping from jcrPrimaryNodeType to class or interface(s).
        if (objectConverter == null) {
            
            List<Class<? extends HippoBean>> localAnnotatedClasses = getLocalAnnotatedClasses();
            
            // 
            // When local annotated classes are not empty, it means the specific component
            // wants its own object converter with some additional annotated bean classes
            // which were manullay added by its overriding method, #getLocalAnnotatedClasses().
            //
            // On the other hand, if local annotated classes are empty, it means each component
            // can share one globally shared object converter with the same annotated classes.
            // In this case, the object converter is stored into servlet context attribute.
            // 
            
            boolean objectConverterSharable = (localAnnotatedClasses == null || localAnnotatedClasses.isEmpty());
            
            if (objectConverterSharable) {
                
                objectConverter = (ObjectConverter) servletContext.getAttribute(OBJECT_CONVERTER_CONTEXT_ATTRIBUTE);
                
                if (objectConverter == null) {
                    List<Class<? extends HippoBean>> annotatedClasses = getAnnotatedClasses();
                    objectConverter = ObjectConverterUtils.createObjectConverter(annotatedClasses);
                    servletContext.setAttribute(OBJECT_CONVERTER_CONTEXT_ATTRIBUTE, objectConverter);
                }
                
            } else {
                
                List<Class<? extends HippoBean>> annotatedClasses = getAnnotatedClasses();
                
                for (Class<? extends HippoBean> localClass : localAnnotatedClasses) {
                    if (annotatedClasses.contains(localClass)) {
                        log.debug("local added class '{}' already present. Skipping", localClass.getName());
                    } else {
                        annotatedClasses.add(localClass); 
                    }
                }
                
                objectConverter = ObjectConverterUtils.createObjectConverter(annotatedClasses);
                
            }
        }
        
        return objectConverter;
    }

    /**
     * when you want to inject specific component only custom annotated classes override this method
     * 
     * This method is only called during the init() phase of a component     
	 * @return List of annotated classes, and if there are none, return an empty list
     */
    protected List<Class<? extends HippoBean>> getLocalAnnotatedClasses() {
        return null;
    }
    
    private List<Class<? extends HippoBean>> getAnnotatedClasses() {
        List<Class<? extends HippoBean>> annotatedClasses = null;
        
        String param = servletContext.getInitParameter(BEANS_ANNOTATED_CLASSES_CONF_PARAM);
        String ocmAnnotatedClassesResourcePath = (param != null ? param : DEFAULT_BEANS_ANNOTATED_CLASSES_CONF);
        
        try {
            if (ocmAnnotatedClassesResourcePath.startsWith("classpath*:")) {
                ClasspathResourceScanner scanner = MetadataReaderClasspathResourceScanner.newInstance(servletContext);
                annotatedClasses = ObjectConverterUtils.getAnnotatedClasses(scanner, StringUtils.split(ocmAnnotatedClassesResourcePath, ", \t\r\n"));
            } else {
                URL xmlConfURL = servletContext.getResource(ocmAnnotatedClassesResourcePath);
                if (xmlConfURL == null) {
                    throw new IllegalStateException(BEANS_ANNOTATED_CLASSES_CONF_PARAM_ERROR_MSG);
                }
                annotatedClasses = ObjectConverterUtils.getAnnotatedClasses(xmlConfURL);
            }
        } catch (Exception e) {
            throw new HstComponentException(e);
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
        HstRequestContext requestContext = request.getRequestContext();
        Credentials credentials = requestContext.getContextCredentialsProvider().getWritableCredentials(requestContext);
        return getPersistableSession(request, credentials);
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
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName());
        return repository.login(credentials);
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
    
    /**
     * Returns a proxy ParametersInfo object which resolves parameter from HstComponentConfiguration : resolved means that possible property placeholders like
     * ${1} or ${year}, where the first refers to the first wildcard matcher in a resolved sitemap item, and the latter
     * to a resolved parameter in the resolved HstSiteMapItem
     * <P>
     * <EM>NOTE: Because the returned ParametersInfo proxy instance is bound to the current request,
     * you MUST NOT store the returned object in a member variable or session. You should retrieve that per request.</EM>
     * </P>
     * 
     * The parameter map used has inherited parameters from ancestor components, which have precedence over child components) 
     * 
     * @param request the HST request
     * @return the resolved parameter value for this name, or <code>null</null> if not present
     */
    protected <T> T getParametersInfo(final HstRequest request) {
        return (T) ParameterUtils.getParametersInfo(this, getComponentConfiguration(), request);
    }
}
