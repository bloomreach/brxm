/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.portlet.MimeResponse;
import javax.portlet.PortletRequest;
import javax.portlet.PortletURL;
import javax.portlet.ResourceURL;

import org.hippoecm.hst.container.HstContainerPortlet;
import org.hippoecm.hst.core.request.HstPortletRequestContext;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * HstEmbeddedPortletContainerURLProviderImpl
 * 
 * @version $Id$
 */
public class HstEmbeddedPortletContainerURLWriter {
    
    public String toURLString(HstContainerURLProviderImpl urlProvider, HstContainerURL containerURL, HstRequestContext requestContext, String contextPath) throws UnsupportedEncodingException, ContainerException {
        String urlString = "";
        
        HstPortletRequestContext prc = (HstPortletRequestContext)requestContext;
        String lifecycle = (String)prc.getPortletRequest().getAttribute(PortletRequest.LIFECYCLE_PHASE);
        
        if (PortletRequest.RESOURCE_PHASE.equals(lifecycle) || PortletRequest.RENDER_PHASE.equals(lifecycle)) {
            String resourceWindowReferenceNamespace = containerURL.getResourceWindowReferenceNamespace();
            String actionWindowReferenceNamespace = containerURL.getActionWindowReferenceNamespace();
            boolean hstContainerResource = ContainerConstants.CONTAINER_REFERENCE_NAMESPACE.equals(resourceWindowReferenceNamespace);
            
            StringBuilder path = new StringBuilder(100);
            
            String pathInfo = null;
            
            if (hstContainerResource) {
                String oldPathInfo = containerURL.getPathInfo();
                String resourcePath = containerURL.getResourceId();
                Map<String, String[]> oldParamMap = containerURL.getParameterMap();
                
                try {
                    containerURL.setResourceWindowReferenceNamespace(null);
                    ((HstContainerURLImpl) containerURL).setPathInfo(resourcePath);
                    ((HstContainerURLImpl) containerURL).setParameters(null);
                    pathInfo = urlProvider.buildHstURLPath(containerURL, requestContext);
                } finally {
                    containerURL.setResourceWindowReferenceNamespace(resourceWindowReferenceNamespace);
                    ((HstContainerURLImpl) containerURL).setPathInfo(oldPathInfo);
                    ((HstContainerURLImpl) containerURL).setParameters(oldParamMap);
                }
                
                if (!urlProvider.isPortletResourceURLEnabled()) {
                    if(contextPath != null) {
                        path.append(contextPath);
                    } 
                    else if (requestContext.getResolvedMount().getMount().isContextPathInUrl()) {
                        path.append(containerURL.getContextPath());
                    }
                }
                path.append(pathInfo);
                urlString = path.toString();
            }
            else {
                pathInfo = urlProvider.buildHstURLPath(containerURL, requestContext);
                if (actionWindowReferenceNamespace != null) {
                    PortletURL url = ((MimeResponse)prc.getPortletResponse()).createActionURL();
                    url.setParameter(HstContainerPortlet.HST_PATH_PARAM_NAME, pathInfo);
                    urlString = url.toString();
                } else if (resourceWindowReferenceNamespace != null) {
                	if (urlProvider.isPortletResourceURLEnabled()) {
                        ResourceURL url = ((MimeResponse)prc.getPortletResponse()).createResourceURL();
                        url.setResourceID(pathInfo);
                        urlString = url.toString();
                	}
                	else {
                        if(contextPath != null) {
                            path.append(contextPath);
                        } 
                        else if (requestContext.getResolvedMount().getMount().isContextPathInUrl()) {
                            path.append(containerURL.getContextPath());
                        }
                        path.append(containerURL.getResolvedMountPath());
                        path.append(pathInfo);
                        urlString = path.toString();
                	}
                } else {
                    // Embedded render URL
                	path.append(prc.getEmbeddingContextPath());
                	path.append(prc.getResolvedEmbeddingMount().getResolvedMountPath());
                	path.append(pathInfo);
                    urlString = path.toString();
                }
            }
            
        } else {
            // should not be allowed to come here: throw IllegalStateException?
        } 
        return urlString;
    }
}
