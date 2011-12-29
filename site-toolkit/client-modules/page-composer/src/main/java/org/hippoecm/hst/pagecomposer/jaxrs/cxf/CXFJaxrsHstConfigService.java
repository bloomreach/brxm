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
package org.hippoecm.hst.pagecomposer.jaxrs.cxf;

import java.util.Map;
import java.util.UUID;

import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.jaxrs.JAXRSService;
import org.hippoecm.hst.jaxrs.cxf.CXFJaxrsService;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id: CXFJaxrsContentService.java 24556 2010-10-22 01:10:59Z wko $
 *
 */
public class CXFJaxrsHstConfigService extends CXFJaxrsService {
    
    private static Logger log = LoggerFactory.getLogger(CXFJaxrsHstConfigService.class);
 
    public final static String REQUEST_CONFIG_NODE_IDENTIFIER = "org.hippoecm.hst.pagecomposer.jaxrs.cxf.contentNode.identifier";
    public final static String REQUEST_ERROR_MESSAGE_ATTRIBUTE = "org.hippoecm.hst.pagecomposer.jaxrs.cxf.exception.message";
    
	public CXFJaxrsHstConfigService(String serviceName) {
		super(serviceName);
	}
	
	public CXFJaxrsHstConfigService(String serviceName, Map<String, String> jaxrsConfigParameters) {
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
		String uuid = PathUtils.normalizePath(requestContext.getBaseURL().getPathInfo());
	    if(uuid == null) {
	        throw new ContainerException("CXFJaxrsHstConfigService expects a 'uuid' as pathInfo but pathInfo was null. Cannot process REST call"); 
	    }
		try {
		    UUID.fromString(uuid); 
		} catch(IllegalArgumentException e) {
		    throw new ContainerException("CXFJaxrsHstConfigService expects a 'uuid' as pathInfo but was '"+uuid+"'. Cannot process REST call");
		}
		
		Node node = null;
		String resourceType = "";
		
        try {
        	Session jcrSession = requestContext.getSession();
        	// we explicitly call a refresh here. Normally, the sessionstateful jcr session is already refreshed. However, due to asychronous
        	// jcr event dispatching, there might be changes in the repository, but not yet a jcr event was sent that triggers a jcr session refresh. Hence, here
        	// we explicitly refresh the jcr session again. 
        	jcrSession.refresh(false);
        	node = jcrSession.getNodeByIdentifier(uuid);
        	resourceType = node.getPrimaryNodeType().getName();
          
        } catch (PathNotFoundException e) {
           if(log.isDebugEnabled()) { 
               log.warn("PathNotFoundException ", e);
           } else {
              log.warn("PathNotFoundException {}", e.toString());
           }
           return setErrorMessageAndReturn(requestContext, request, e.toString());
        }  catch (ItemNotFoundException e) {
            if(log.isDebugEnabled()) { 
                log.warn("ItemNotFoundException ", e);
            } else {
               log.warn("ItemNotFoundException {}", e.toString());
            }
            return  setErrorMessageAndReturn(requestContext, request, e.toString());
        } catch (LoginException e) {
            if(log.isDebugEnabled()) { 
                log.warn("LoginException ", e);
            } else {
               log.warn("LoginException {}", e.toString());
            }
            return  setErrorMessageAndReturn(requestContext, request, e.toString());
		} catch (RepositoryException e) {
		    if(log.isDebugEnabled()) { 
	           log.warn("RepositoryException ", e);
	        } else {
	          log.warn("RepositoryException {}", e.toString());
	        }
		    return setErrorMessageAndReturn(requestContext, request, e.toString());
		} 

        requestContext.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, uuid);
    	
        // use JAX-RS service endpoint url-template: /{resourceType}/{suffix}
        StringBuilder jaxrsEndpointRequestPath = new StringBuilder("/").append(resourceType).append("/");
    	if (requestContext.getPathSuffix() != null) {
    		jaxrsEndpointRequestPath.append(requestContext.getPathSuffix());
    	}
    	if (log.isDebugEnabled()) {
    		log.debug("Invoking JAX-RS endpoint {}: {} for uuid {}", new Object[]{request.getMethod(), jaxrsEndpointRequestPath.toString(), uuid});
    	}
    	return new PathsAdjustedHttpServletRequestWrapper(request, getJaxrsServletPath(requestContext), jaxrsEndpointRequestPath.toString());
	}
	
	private HttpServletRequest setErrorMessageAndReturn(HstRequestContext requestContext, HttpServletRequest request, String message) throws ContainerException {
	    request.setAttribute(REQUEST_ERROR_MESSAGE_ATTRIBUTE, message);
	    String jaxrsEndpointRequestPath = "/hst:exception/";
	    return new PathsAdjustedHttpServletRequestWrapper(request, getJaxrsServletPath(requestContext), jaxrsEndpointRequestPath);
    }

    @Override
	public void invoke(HstRequestContext requestContext, HttpServletRequest request, HttpServletResponse response) throws ContainerException {
		super.invoke(requestContext, request, response);
	}
}
