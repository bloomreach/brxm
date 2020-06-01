/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

    	log.debug("Invoking JAX-RS endpoint {}: {} for contentPath {}", new Object[]{request.getMethod(), jaxrsEndpointRequestPath.toString(), bean.getPath()});
    	return new PathsAdjustedHttpServletRequestWrapper(request, getJaxrsServletPath(requestContext), jaxrsEndpointRequestPath.toString());
	}
}
