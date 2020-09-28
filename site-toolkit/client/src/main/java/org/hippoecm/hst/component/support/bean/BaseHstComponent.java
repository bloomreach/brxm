/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Map;
import java.util.Optional;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletContext;

import org.apache.commons.lang3.ArrayUtils;
import org.hippoecm.hst.configuration.components.DynamicParameter;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.ContentNodeBinder;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManager;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManagerImpl;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.HstResponseUtils;
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

    private ServletContext servletContext;

    public void init(ServletContext servletContext, ComponentConfiguration componentConfig) throws HstComponentException {
        super.init(servletContext, componentConfig);
        this.servletContext = servletContext;
    }


    /**
     * Returns resolved parameter from HstComponentConfiguration : resolved means that possible property placeholders like
     * ${1} or ${year}, where the first refers to the first wildcard matcher in a resolved sitemap item, and the latter
     * to a resolved parameter in the resolved HstSiteMapItem
     *
     * The parameter map used has inherited parameters from ancestor components, which have precedence over child components)
     * <b>Note that the returned value does not take into account that there are for example prefixed parameters for targeting
     * or param values which should be overridden by a query param. In general, always better to use
     * BaseHstComponent#getComponentParametersInfo(org.hippoecm.hst.core.component.HstRequest)</b>
     * @param name
     * @return the resolved parameter value for this name, or <code>null</null> if not present
     */
    public String getComponentParameter(String name) {
        return this.getComponentConfiguration().getParameter(name, RequestContextProvider.get().getResolvedSiteMapItem());
    }

    /**
     * See {@link #getComponentParameter(String)}, where we now return all resolved parameters (thus with inheritance of
     * ancestor components)
     * @return Map of all parameters, and when no parameters present, return empty map.
     */
    public  Map<String,String> getComponentParameters(){
        return this.getComponentConfiguration().getParameters(RequestContextProvider.get().getResolvedSiteMapItem());
    }

    public Optional<DynamicParameter> getDynamicComponentParameter(String name) {
        return this.getComponentConfiguration().getDynamicComponentParameter(name);
    }
    
    /**
     * See {@link #getComponentParameter(String)}, but now, only resolved parameters directly on the HstComponent are taken into
     * account: in other words, no inheritance of parameters is applied
     * @param name
     * @return the resolved parameter value for this name, or <code>null</null> if not present
     */
    public String getComponentLocalParameter(String name) {
        return (String)this.getComponentConfiguration().getLocalParameter(name, RequestContextProvider.get().getResolvedSiteMapItem());
    }

    /**
     * See {@link #getComponentParameters()}, but now, only resolved parameter map of parameters is returned that is directly on
     * the HstComponenConfiguration, thus, no inheritance is applied
     * @return Map of all parameters, and when no parameters present, return empty map.
     */
    public  Map<String,String> getComponentLocalParameters(){
        return this.getComponentConfiguration().getLocalParameters(RequestContextProvider.get().getResolvedSiteMapItem());
    }


    /**
     * A public request parameter is a {@link javax.servlet.http.HttpServletRequest} parameter that is not namespaced. Thus for example ?foo=bar. Typically,
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

        Map<String, String []> namespaceLessParameters = request.getParameterMap("");
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
        Map<String, String []> namespaceLessParameters = request.getParameterMap("");
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
     * @return <code>true</code> when this request is matched to a preview site
     * @see Mount#isPreview()
     */
    public boolean isPreview(HstRequest request) {
    	return request.getRequestContext().isPreview();
    }

    /**
     * @param request
     * @return the root gallery HippoFolderBean at <code>/content/gallery</code> and <code>null</code> if it does not exist 
     */
    public HippoFolderBean getGalleryBaseBean(HstRequest request){
        try {
            HippoBean gallery = (HippoBean)request.getRequestContext().getObjectBeanManager().getObject("/content/gallery");
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
            HippoBean assets = (HippoBean)request.getRequestContext().getObjectBeanManager().getObject("/content/assets");
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
        final HstRequestContext requestContext = request.getRequestContext();
        String base = requestContext.getSiteContentBasePath();
        String relPath = PathUtils.normalizePath(resolvedSiteMapItem.getRelativeContentPath());
        if(relPath == null) {
            log.debug("Cannot return a content bean for relative path null for resolvedSitemapItem belonging to '{}'. Return null", resolvedSiteMapItem.getHstSiteMapItem().getId());
            return null;
        }
        try {
            if("".equals(relPath)) {
                return (HippoBean) requestContext.getObjectBeanManager().getObject("/"+base);
            } else {
                return (HippoBean) requestContext.getObjectBeanManager().getObject("/"+base+ "/" + relPath);
            }
        } catch (ObjectBeanManagerException e) {
            log.error("ObjectBeanManagerException. Return null : {}", e);
        }
        return null;

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
        WorkflowPersistenceManagerImpl wpm = new WorkflowPersistenceManagerImpl(session, RequestContextProvider.get().getContentBeansTool().getObjectConverter(), contentNodeBinders);
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
    protected <T> T getComponentParametersInfo(final HstRequest request) {
        return (T) ParameterUtils.getParametersInfo(this, getComponentConfiguration(), request);
    }
}
