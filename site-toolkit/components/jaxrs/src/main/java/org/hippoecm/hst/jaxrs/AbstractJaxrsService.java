/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.jaxrs;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.jaxrs.util.AnnotatedContentBeanClassesScanner;
import org.hippoecm.hst.util.GenericHttpServletRequestWrapper;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.hst.util.ObjectConverterUtils;
import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 *
 */
public abstract class AbstractJaxrsService implements JAXRSService {

    private static final Logger log = LoggerFactory.getLogger(AbstractJaxrsService.class);
    
	private Map<String,String> jaxrsConfigParameters;
	private String serviceName;
	private String servletPath = "";
	

    public static final String BEANS_ANNOTATED_CLASSES_CONF_PARAM = "hst-beans-annotated-classes";
    private String annotatedClassesResourcePath;
    private List<Class<? extends HippoBean>> annotatedClasses;
    private ObjectConverter objectConverter;
    
	
	protected AbstractJaxrsService(String serviceName, Map<String,String> jaxrsConfigParameters) {
		this.serviceName = serviceName;
		this.jaxrsConfigParameters = jaxrsConfigParameters;
	}
	
	public String getServletPath() {
		return servletPath;
	}
	
    public void setServletPath(String servletPath) {
    	this.servletPath = servletPath;
    }
    
    public String getAnnotatedClassesResourcePath() {
        return annotatedClassesResourcePath;
    }
    
    public void setAnnotatedClassesResourcePath(String annotatedClassesResourcePath) {
        this.annotatedClassesResourcePath = annotatedClassesResourcePath;
    }

    public void setAnnotatedClasses(List<Class<? extends HippoBean>> annotatedClasses) {
        this.annotatedClasses = annotatedClasses;
    }
    
    public void setObjectConverter(ObjectConverter objectConverter) {
        this.objectConverter = objectConverter;
    }
    
    protected List<Class<? extends HippoBean>> getAnnotatedClasses(HstRequestContext requestContext) {
        if (annotatedClasses == null) {
            String annoClassPathResourcePath = getAnnotatedClassesResourcePath();
            
            if (StringUtils.isBlank(annoClassPathResourcePath)) {
                annoClassPathResourcePath = requestContext.getServletContext().getInitParameter(BEANS_ANNOTATED_CLASSES_CONF_PARAM);
            }
            
            annotatedClasses = AnnotatedContentBeanClassesScanner.scanAnnotatedContentBeanClasses(requestContext, annoClassPathResourcePath);
        }
        
        return annotatedClasses;
    }
    
    protected ObjectConverter getObjectConverter(HstRequestContext requestContext) {
        if (objectConverter == null) {
            List<Class<? extends HippoBean>> annotatedClasses = getAnnotatedClasses(requestContext);
            objectConverter = ObjectConverterUtils.createObjectConverter(annotatedClasses);
        }
        return objectConverter;
    }
    
    
	public abstract void invoke(HstRequestContext requestContext, HttpServletRequest request, HttpServletResponse response) throws ContainerException;

    protected ServletConfig getJaxrsServletConfig(ServletContext servletContext) {
    	return new ServletConfigImpl(serviceName, servletContext, jaxrsConfigParameters);
    }
    
    protected String getJaxrsServletPath(HstRequestContext requestContext) throws ContainerException {
        ResolvedMount resolvedMount = requestContext.getResolvedMount();
        return new StringBuilder(resolvedMount.getResolvedMountPath()).append(getServletPath()).toString();
    }
    
    /**
     * Concrete implementations must implement this method to get the jaxrs pathInfo. This one is most likely different than 
     * {@link HstRequestContext#getBaseURL()#getPathInfo()} because the baseURL has a pathInfo which has been stripped from matrix parameters
     * @param requestContext
     * @param request
     * @return
     * @throws ContainerException
     */
    abstract protected String getJaxrsPathInfo(HstRequestContext requestContext, HttpServletRequest request) throws ContainerException;
    
    protected HttpServletRequest getJaxrsRequest(HstRequestContext requestContext, HttpServletRequest request) throws ContainerException {
    	return new PathsAdjustedHttpServletRequestWrapper(request, getJaxrsServletPath(requestContext), getJaxrsPathInfo(requestContext, request));
    }
    
    protected String getMountContentPath(HstRequestContext requestContext) {
        return requestContext.getResolvedMount().getMount().getContentPath();
    }
    
    protected Node getContentNode(Session session, String path) throws RepositoryException {
        if(path == null || !path.startsWith("/")) {
            log.warn("Illegal argument for '{}' : not an absolute path", path);
            return null;
        }
        String relPath = path.substring(1);
        Node node = session.getRootNode();
        String nodePath = null;
        nodePath  = node.getPath();
        if(!node.hasNode(relPath)) {
            log.info("Cannot get object for node '{}' with relPath '{}'", nodePath , relPath);
            return null;
        }
        Node relNode = node.getNode(relPath);
        if (relNode.isNodeType(HippoNodeType.NT_HANDLE)) {
            // if its a handle, we want the child node. If the child node is not present,
            // this node can be ignored
            if(relNode.hasNode(relNode.getName())) {
                return relNode.getNode(relNode.getName());
            } 
            else {
                log.info("Cannot get object for node '{}' with relPath '{}'", nodePath, relPath);
                return null;
            }
        } 
        else {
            return relNode;
        }   
    }
	

    /**
     * Returns the content HippoBean of type T for the current request. If there cannot be found a bean of type <code>beanMappingClass<code> for the relative content path of the
     * resolved sitemap item, <code>null</code> is returned.
     * If there is no resolved sitemap item, <code>null</code> is returned. 
     * @param <T>
     * @param requestContext
     * @param beanMappingClass
     * @return a bean of type T and <code>null</code> if there cannot be found a content bean for the requestContext or when the bean is not of type <code>beanMappingClass</code>
     */
    public <T extends HippoBean> T getRequestContentBean(HstRequestContext requestContext, Class<T> beanMappingClass) {
        HippoBean bean = getRequestContentBean(requestContext);
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
     * Returns the content HippoBean for the current request.
     * @param requestContext
     * @param beanMappingClass 
     * @return the HippoBean where the relative contentpath of the sitemap item points to or <code>null</code> when not found, or no relative content path is present, or no resolved sitemap item
     */
    public HippoBean getRequestContentBean(HstRequestContext requestContext) {
       
        String contentPathInfo = null;    
        // first check whehter we have a resolved mount that has isMapped = true. If not mapped, we take the 
        // contentPathInfo directly from the requestContext.getBaseURL().getPathInfo()
        if(requestContext.getResolvedMount().getMount().isMapped()) {
            ResolvedSiteMapItem resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();
            if (resolvedSiteMapItem == null) {
                log.debug("There is no resolved sitemap item for '{}' so no requestContentBean can be returned. Return null", requestContext.getBaseURL().getPathInfo());
                return null;
            } else {
                contentPathInfo = resolvedSiteMapItem.getRelativeContentPath();
            }
        } else {
            contentPathInfo = PathUtils.normalizePath(requestContext.getBaseURL().getPathInfo());
        }
        String requestContentPath = getMountContentPath(requestContext) + "/" + (contentPathInfo != null ? contentPathInfo : "");
        requestContext.setAttribute(JAXRSService.REQUEST_CONTENT_PATH_KEY, requestContentPath);
        
        try {
            HippoBean bean = (HippoBean) getObjectConverter(requestContext).getObject(requestContext.getSession(), requestContentPath);
            return bean;
        } catch (LoginException e) {
            log.warn("Login Exception during fetching bean. Return null" , e.toString());
        } catch (ObjectBeanManagerException e) {
            log.warn("ObjectBeanManagerException during fetching bean. Return null" , e.toString());
        } catch (RepositoryException e) {
            log.warn("RepositoryException during fetching bean. Return null" , e.toString());
        }
        
        return null;
    }

    /**
     * 
     * @param requestContext
     * @return the siteContentBaseBean if it can be found and <code>null</code> otherwise
     */
    public HippoFolderBean getSiteContentBaseBean(HstRequestContext requestContext) {
        String requestContentPath = getMountContentPath(requestContext);
        
        try {
            HippoFolderBean bean = (HippoFolderBean) getObjectConverter(requestContext).getObject(requestContext.getSession(), requestContentPath);
            return bean;
        } catch (LoginException e) {
            log.warn("Login Exception during fetching site content base bean. Return null" , e.toString());
        } catch (ObjectBeanManagerException e) {
            log.warn("ObjectBeanManagerException during fetching site content base  bean. Return null" , e.toString());
        } catch (RepositoryException e) {
            log.warn("RepositoryException during fetching site content base  bean. Return null" , e.toString());
        }
        
        return null;
    }
    
	protected static class ServletConfigImpl implements ServletConfig {
		
		private String servletName;
		private ServletContext context;
		private Map<String,String> initParams;
		
		public ServletConfigImpl(String servletName, ServletContext context, Map<String,String> initParams) {
			this.servletName = servletName;
			this.context = context;
			this.initParams = Collections.unmodifiableMap(initParams);
		}

		public String getInitParameter(String name) {
			return initParams.get(name);
		}

		@SuppressWarnings("rawtypes")
		public Enumeration getInitParameterNames() {
			return Collections.enumeration(initParams.keySet());
		}

		public ServletContext getServletContext() {
			return context;
		}

		public String getServletName() {
			return servletName;
		}
	}
	
    protected static class PathsAdjustedHttpServletRequestWrapper extends GenericHttpServletRequestWrapper {

    	private String requestURI;
    	private String requestURL;
        
    	/**
    	 * @deprecated use {@link PathsAdjustedHttpServletRequestWrapper(HttpServletRequest, String, String) instead} 
    	 */
    	@Deprecated
    	public PathsAdjustedHttpServletRequestWrapper(HstRequestContext requestContext, HttpServletRequest request, String servletPath, String requestPath) {
    	    this(request, servletPath, requestPath);
    	    log.warn("PathsAdjustedHttpServletRequestWrapper constructor with HstRequestContext is deprecated. Use PathsAdjustedHttpServletRequestWrapper(HttpServletRequest, String, String) instead");
    	}
    	
        public PathsAdjustedHttpServletRequestWrapper(HttpServletRequest request, String servletPath, String requestPath) {
            super(request);
            setServletPath(servletPath);
            
            if (requestPath != null) {
                setPathInfo(HstRequestUtils.removeAllMatrixParams(requestPath));
            }
            
            StringBuilder sbTemp = new StringBuilder(getContextPath()).append(getServletPath());
            if (requestPath != null) {
                sbTemp.append(requestPath);
            }
            requestURI = sbTemp.toString();
            
            if (requestURI.length() == 0) {
                requestURI = "/";
            }
        }
        
		@Override
		public String getRequestURI() {
			return requestURI;
		}

		@Override
		public StringBuffer getRequestURL() {
			if (requestURL == null) {
				String scheme = super.getScheme();
				String serverName = super.getServerName();
				int serverPort = super.getServerPort();
				StringBuilder sbTemp = new StringBuilder(100);
				sbTemp.append(scheme).append("://").append(serverName);
				if (serverPort > 0 && (("http".equals(scheme) && serverPort != 80) || ("https".equals(scheme) && serverPort != 443))) {
				    sbTemp.append(":").append(serverPort);
				}
				sbTemp.append(getRequestURI());
				requestURL = sbTemp.toString();
			}
			
			return new StringBuffer(requestURL);
		}
    }
}
