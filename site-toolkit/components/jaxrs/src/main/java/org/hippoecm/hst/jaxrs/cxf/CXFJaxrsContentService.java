/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.jaxrs.cxf;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.ContainerNotFoundException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 *
 */
public class CXFJaxrsContentService extends CXFJaxrsService {
    
    private static Logger log = LoggerFactory.getLogger(CXFJaxrsContentService.class);
    
    
    
	public CXFJaxrsContentService(String serviceName) {
		super(serviceName);
	}
	
	public CXFJaxrsContentService(String serviceName, Map<String, String> jaxrsConfigParameters) {
		super(serviceName, jaxrsConfigParameters);
	}
	
    
	@Override
	/*
	 * temporarily splitting off and saving suffix from pathInfo until this is generally handled with HSTTWO-1189
	 */
	protected String getJaxrsPathInfo(HstRequestContext requestContext, HttpServletRequest request) throws ContainerException {
		return requestContext.getPathSuffix();
	}

	@Override
	protected HttpServletRequest getJaxrsRequest(HstRequestContext requestContext, HttpServletRequest request) throws ContainerException {
		
	    HippoBean bean = getRequestContentBean(requestContext, HippoBean.class);
	    if(bean == null) {
	        throw new ContainerNotFoundException("Cannot find content node for '"+requestContext.getBaseURL().getRequestPath()+"'",new WebApplicationException(Response.Status.NOT_FOUND));
	    }
	    
	    org.hippoecm.hst.content.beans.Node annotation = bean.getClass().getAnnotation(org.hippoecm.hst.content.beans.Node.class);
	    String resourceType = annotation.jcrType();
	    
        // use JAX-RS service endpoint url-template: /{resourceType}/{suffix}
        StringBuilder jaxrsEndpointRequestPath = new StringBuilder("/").append(resourceType).append("/");
    	if (requestContext.getPathSuffix() != null) {
    		jaxrsEndpointRequestPath.append(requestContext.getPathSuffix());
    	}
    	if (log.isDebugEnabled()) {
    		log.debug("Invoking JAX-RS endpoint {}: {} for contentPath {}", new Object[]{request.getMethod(), jaxrsEndpointRequestPath.toString(), bean.getPath()});
    	}
    	return new PathsAdjustedHttpServletRequestWrapper(request, getJaxrsServletPath(requestContext), jaxrsEndpointRequestPath.toString());
	}
	
    @Override
	public void invoke(HstRequestContext requestContext, HttpServletRequest request, HttpServletResponse response) throws ContainerException {
		try {
			super.invoke(requestContext, request, response);
		}
		catch (ContainerException ce) {
			// TODO: preliminary hard-coded DELETE handling of a no (longer) existing content resource which might/should be ignorable?
			if (request.getMethod().equalsIgnoreCase("DELETE") && ce.getCause() != null && ce.getCause() instanceof WebApplicationException) {
				WebApplicationException we = (WebApplicationException)ce.getCause();
				if (we.getResponse().getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
					// TODO: determine the appropriate response status: SC_GONE, SC_OK ???
					response.setStatus(HttpServletResponse.SC_GONE);
					return;
				}
			}
			throw ce;
		}
	}
}
