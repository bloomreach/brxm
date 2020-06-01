/*
 * Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.util.GenericHttpServletRequestWrapper;
import org.hippoecm.hst.util.HstRequestUtils;
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

    /**
     * @deprecated since 2.28.00 not used any more. If custom annotated classes are needed, inject a
     * custom object converter in the HstRequestContext through ContentBeansTool. This method does not do
     * anything any more
     */
    @Deprecated
    public String getAnnotatedClassesResourcePath() {
        log.warn("AbstractJaxrsService#getAnnotatedClassesResourcePath is deprecated and does not do anything any more.");
        return null;
    }

    /**
     * @deprecated since 2.28.00 not used any more.  If custom annotated classes are needed, inject a
     * custom object converter in the HstRequestContext through ContentBeansTool. This method does not do
     * anything any more
     */
    @Deprecated
    public void setAnnotatedClassesResourcePath(String annotatedClassesResourcePath) {
        log.warn("AbstractJaxrsService#setAnnotatedClassesResourcePath is deprecated and does not do anything any more.");
    }

    /**
     * @deprecated since 2.28.00 not used any more.  If custom annotated classes are needed, inject a
     * custom object converter in the HstRequestContext through ContentBeansTool. This method does not do
     * anything any more
     */
    @Deprecated
    public void setAnnotatedClasses(List<Class<? extends HippoBean>> annotatedClasses) {
        log.warn("AbstractJaxrsService#setAnnotatedClasses is deprecated and does not do anything any more.");
    }

    /**
     * @deprecated since 2.28.00 not used any more.  If custom object converter is needed, inject a
     * custom object converter in the HstRequestContext through ContentBeansTool. This method does not do
     * anything any more
     */
    @Deprecated
    public void setObjectConverter(ObjectConverter objectConverter) {
        log.warn("AbstractJaxrsService#setObjectConverter is deprecated and does not do anything any more.");
    }

    /**
     * @deprecated since 2.28.00 not used any more. If custom annotated classes are needed, inject a
     * custom object converter in the HstRequestContext through ContentBeansTool. This method does not do
     * anything any more
     */
    @Deprecated
    protected List<Class<? extends HippoBean>> getAnnotatedClasses(HstRequestContext requestContext) {
        log.warn("AbstractJaxrsService#getAnnotatedClasses is deprecated and does not do anything any more.");
        return Collections.emptyList();
    }

    protected ObjectConverter getObjectConverter(HstRequestContext requestContext) {
        return requestContext.getContentBeansTool().getObjectConverter();
    }
    
    public void initialize() throws ContainerException {
    }

    public abstract void invoke(HstRequestContext requestContext, HttpServletRequest request, HttpServletResponse response) throws ContainerException;

    public void destroy() {
    }

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

        if(!session.nodeExists(path)) {
            log.info("Cannot get object for path '{}' '", path);
            return null;
        }
        Node node = session.getNode(path);
        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
            // if its a handle, we want the child node. If the child node is not present,
            // this node can be ignored
            if(node.hasNode(node.getName())) {
                return node.getNode(node.getName());
            } 
            else {
                log.info("Cannot get object for path '{}''", path);
                return null;
            }
        } 
        else {
            return node;
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
        T bean = requestContext.getContentBean(beanMappingClass);
        if(bean == null) {
            return null;
        }
        requestContext.setAttribute(JAXRSService.REQUEST_CONTENT_PATH_KEY, bean.getPath());
        return bean;
    }

    /**
     * Returns the content HippoBean for the current request.
     * @param requestContext
     * @return the HippoBean where the relative contentpath of the sitemap item points to or <code>null</code> when not found, or no relative content path is present, or no resolved sitemap item
     */
    public HippoBean getRequestContentBean(HstRequestContext requestContext) {
       return getRequestContentBean(requestContext, HippoBean.class);
    }

    /**
     * 
     * @param requestContext
     * @return the siteContentBaseBean if it can be found and <code>null</code> otherwise
     */
    public HippoFolderBean getSiteContentBaseBean(HstRequestContext requestContext) {
        return (HippoFolderBean)requestContext.getSiteContentBaseBean();
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
