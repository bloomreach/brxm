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

import java.util.List;
import java.util.Map;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.jaxrs.JAXRSService;
import org.hippoecm.hst.jaxrs.util.AnnotatedContentBeanClassesScanner;
import org.hippoecm.hst.util.ObjectConverterUtils;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 *
 */
public class CXFJaxrsContentService extends CXFJaxrsService {
    
    private static Logger log = LoggerFactory.getLogger(CXFJaxrsContentService.class);
    
    public static final String BEANS_ANNOTATED_CLASSES_CONF_PARAM = "hst-beans-annotated-classes";
    
    private String annotatedClassesResourcePath;
    private List<Class<? extends HippoBean>> annotatedClasses;
    private ObjectConverter objectConverter;
    
	public CXFJaxrsContentService(String serviceName) {
		super(serviceName);
	}
	
	public CXFJaxrsContentService(String serviceName, Map<String, String> jaxrsConfigParameters) {
		super(serviceName, jaxrsConfigParameters);
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
    
	@Override
	/*
	 * temporarily splitting off and saving suffix from pathInfo until this is generally handled with HSTTWO-1189
	 */
	protected String getJaxrsPathInfo(HstRequestContext requestContext, HttpServletRequest request) throws ContainerException {
		return requestContext.getPathSuffix();
	}

	@Override
	protected HttpServletRequest getJaxrsRequest( HstRequestContext requestContext, HttpServletRequest request) throws ContainerException {
		String contentPathInfo = null;
		
		ResolvedSiteMapItem resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();
		if (resolvedSiteMapItem == null) {
		    contentPathInfo = PathUtils.normalizePath(requestContext.getBaseURL().getPathInfo());
		} else {
		    contentPathInfo = resolvedSiteMapItem.getRelativeContentPath();
		}
		
		String requestContentPath = getMountPointContentPath(requestContext) + "/" + (contentPathInfo != null ? contentPathInfo : "");
		
		Node node = null;
		String resourceType = "";
		
        try {
        	Session jcrSession = requestContext.getSession();
        	node = getContentNode(jcrSession, requestContentPath);
        	if (node == null) {
                throw new ContainerException("Cannot find content node at '"+requestContentPath+"'",new WebApplicationException(Response.Status.NOT_FOUND));
        	}
        	resourceType = getObjectConverter(requestContext).getPrimaryObjectType(node);
            if (resourceType == null) {
                throw new ContainerException("Cannot find the resourceType for node '"+node.getPath()+"' with primary type '"+node.getPrimaryNodeType().getName()+"'",new WebApplicationException(Response.Status.NOT_FOUND));
            }
        } catch (PathNotFoundException pnf) {
            throw new ContainerException(new WebApplicationException(Response.Status.NOT_FOUND));
        } catch (LoginException e) {
            throw new ContainerException(e);
		} catch (RepositoryException e) {
            throw new ContainerException(e);
		} catch (ObjectBeanManagerException e) {
		    throw new ContainerException(e);
		}
		
		requestContext.setAttribute(JAXRSService.REQUEST_CONTENT_PATH_KEY, requestContentPath);
    	requestContext.setAttribute(JAXRSService.REQUEST_CONTENT_NODE_KEY, node);
    	
        // use JAX-RS service endpoint url-template: /{resourceType}/{suffix}
        StringBuilder jaxrsEndpointURL = new StringBuilder("/").append(resourceType).append("/");
    	if (requestContext.getPathSuffix() != null) {
    		jaxrsEndpointURL.append(requestContext.getPathSuffix());
    	}
    	if (log.isDebugEnabled()) {
    		log.debug("Invoking JAX-RS endpoint {}: {} for contentPath {}", new Object[]{request.getMethod(), jaxrsEndpointURL.toString(), requestContentPath});
    	}
    	return new PathsAdjustedHttpServletRequestWrapper(requestContext, request, getJaxrsServletPath(requestContext), jaxrsEndpointURL.toString());
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
