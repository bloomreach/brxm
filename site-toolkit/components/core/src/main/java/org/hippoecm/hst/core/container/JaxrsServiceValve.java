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
package org.hippoecm.hst.core.container;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMount;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.hippoecm.hst.jaxrs.JAXRSService;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JaxrsServiceValve
 * 
 * @version $Id$
 * @deprecated
 */
public class JaxrsServiceValve extends AbstractValve {
    
    private static final Logger log = LoggerFactory.getLogger(JaxrsServiceValve.class);
    
    private JAXRSService service;
    
    public JaxrsServiceValve(JAXRSService service) {
    	this.service = service;
    }
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {
        try {
        	HstRequestContext requestContext = context.getRequestContext();
            HttpServletRequest request = context.getServletRequest();
            ResolvedSiteMount resolvedSiteMount = requestContext.getResolvedSiteMount();
            String servletPath = new StringBuilder(resolvedSiteMount.getResolvedMountPath()).append(service.getBasePath()).toString();
            
            String pathInfo = HstRequestUtils.getPathInfo(resolvedSiteMount, request);
            
            service.invoke(requestContext, new PathsAdjustedHttpServletRequestWrapper(requestContext, request, servletPath, pathInfo), 
            		context.getServletResponse());
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.error("Failed to invoke jaxrs service.", e);
            } else {
                log.error("Failed to invoke jaxrs service. {}", e.toString());
            }
            throw new ContainerException(e);
        }
    }

    private static class PathsAdjustedHttpServletRequestWrapper extends HttpServletRequestWrapper {

    	private String requestURI;
    	private String requestURL;
        private String servletPath;
        private String pathInfo;
        private HstRequestContext requestContext;
        
        private PathsAdjustedHttpServletRequestWrapper(HstRequestContext requestContext, HttpServletRequest request, String servletPath, String pathInfo) {
            super(request);
            this.servletPath = servletPath;
            this.pathInfo = pathInfo;
            this.requestContext = requestContext;
        }
        
        @Override
        public String getServletPath() {
            return (servletPath != null ? servletPath : "");
        }
        
        @Override
        public String getPathInfo() {
            return pathInfo;
        }

        @Override
		public String getPathTranslated() {
			return null;
		}

		@Override
		public String getRequestURI() {
			if (requestURI == null) {
				String pathInfo = getPathInfo() == null ? "" : getPathInfo();
				requestURI = new StringBuilder(getContextPath()).append(getServletPath()).append(pathInfo).toString();
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
				requestURL = new StringBuilder(super.getScheme()).append("://").append(host.getResolvedHostName()).append(":").append(host.getPortNumber()).append(getRequestURI()).toString();
			}
			return new StringBuffer(requestURL);
		}
    }
}
