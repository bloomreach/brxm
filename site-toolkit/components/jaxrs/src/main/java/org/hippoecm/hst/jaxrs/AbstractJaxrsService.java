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
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.configuration.hosting.SiteMount;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMount;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.hippoecm.hst.util.GenericHttpServletRequestWrapper;
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
	private String basePath = "";
	
	protected AbstractJaxrsService(String serviceName, Map<String,String> jaxrsConfigParameters) {
		this.serviceName = serviceName;
		this.jaxrsConfigParameters = jaxrsConfigParameters;
	}
	
	public String getBasePath() {
		return basePath;
	}
	
    public void setServletPath(String servletPath) {
    	this.basePath = servletPath;
    }
    
	public abstract void invoke(HstRequestContext requestContext, HttpServletRequest request, HttpServletResponse response) throws ContainerException;

    protected ServletConfig getJaxrsServletConfig(ServletContext servletContext) {
    	return new ServletConfigImpl(serviceName, servletContext, jaxrsConfigParameters);
    }
    
    protected String getJaxrsServletPath(HstRequestContext requestContext) throws ContainerException {
        ResolvedSiteMount resolvedSiteMount = requestContext.getResolvedSiteMount();
        return new StringBuilder(resolvedSiteMount.getResolvedMountPath()).append(getBasePath()).toString();
    }
    
    protected String getJaxrsPathInfo(HstRequestContext requestContext, HttpServletRequest request) throws ContainerException {
        return requestContext.getBaseURL().getPathInfo();
    }
    
    protected HttpServletRequest getJaxrsRequest(HstRequestContext requestContext, HttpServletRequest request) throws ContainerException {
    	return new PathsAdjustedHttpServletRequestWrapper(requestContext, request, getJaxrsServletPath(requestContext), getJaxrsPathInfo(requestContext, request));
    }
    
    protected String getMountContentPath(HstRequestContext requestContext) {
        return requestContext.getResolvedSiteMount().getSiteMount().getContentPath();
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
        private HstRequestContext requestContext;
        private String matrixParamsSuffix;
        
        public PathsAdjustedHttpServletRequestWrapper(HstRequestContext requestContext, HttpServletRequest request, String servletPath, String requestPath) {
            super(request);
            setServletPath(servletPath);
            
            String tempPathInfo = null;
            String tempMatrixParamsSuffix = null;
            
            if (requestPath != null) {
                int offset = requestPath.indexOf(';');
                if (offset == -1) {
                    tempPathInfo = requestPath;
                } else {
                    tempPathInfo = requestPath.substring(0, offset);
                    tempMatrixParamsSuffix = requestPath.substring(offset);
                }
            }
            
            setPathInfo(tempPathInfo);
            this.matrixParamsSuffix = tempMatrixParamsSuffix;
            
            this.requestContext = requestContext;
        }
        
		@Override
		public String getRequestURI() {
			if (requestURI == null) {
				StringBuilder sbTemp = new StringBuilder(getContextPath()).append(getServletPath());
                String pathInfo = getPathInfo();
				if (pathInfo != null) {
				    sbTemp.append(pathInfo);
				}
				if (matrixParamsSuffix != null) {
				    sbTemp.append(matrixParamsSuffix);
				}
				requestURI = sbTemp.toString();
				
				if (requestURI.length() == 0) {
					requestURI = "/";
				}
			}
			
			return requestURI;
		}

		@Override
		public StringBuffer getRequestURL() {
			if (requestURL == null) {
				ResolvedVirtualHost host = requestContext.getResolvedSiteMount().getResolvedVirtualHost();
				StringBuilder sbTemp = new StringBuilder(super.getScheme()).append("://").append(host.getResolvedHostName()).append(":").append(host.getPortNumber()).append(getRequestURI());
				requestURL = sbTemp.toString();
			}
			
			return new StringBuffer(requestURL);
		}
    }
}
