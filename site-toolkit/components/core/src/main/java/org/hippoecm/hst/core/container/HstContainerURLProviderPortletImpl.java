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

import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.portlet.BaseURL;
import javax.portlet.MimeResponse;
import javax.portlet.PortletResponse;
import javax.portlet.ResourceURL;

import org.hippoecm.hst.container.HstContainerPortlet;
import org.hippoecm.hst.container.HstContainerPortletContext;
import org.hippoecm.hst.core.hosting.VirtualHost;
import org.hippoecm.hst.core.request.HstRequestContext;

public class HstContainerURLProviderPortletImpl extends AbstractHstContainerURLProvider {
    
    protected boolean portletResourceURLEnabled;
    
    public void setPortletResourceURLEnabled(boolean portletResourceURLEnabled) {
        this.portletResourceURLEnabled = portletResourceURLEnabled;
    }
    
    @Override
    public String toURLString(HstContainerURL containerURL, HstRequestContext requestContext) throws UnsupportedEncodingException, ContainerException {
        String urlString = "";
        
        String resourceWindowReferenceNamespace = containerURL.getResourceWindowReferenceNamespace();
        boolean containerResource = false;
        
        if (!this.portletResourceURLEnabled) {
            containerResource = ContainerConstants.CONTAINER_REFERENCE_NAMESPACE.equals(resourceWindowReferenceNamespace);
        }
        
        StringBuilder path = new StringBuilder(100);
        String pathInfo = null;
        
        if (containerResource) {
            String oldPathInfo = containerURL.getPathInfo();
            String resourcePath = containerURL.getResourceId();
            Map<String, String[]> oldParamMap = containerURL.getParameterMap();
            
            try {
                containerURL.setResourceWindowReferenceNamespace(null);
                ((HstContainerURLImpl) containerURL).setPathInfo(resourcePath);
                ((HstContainerURLImpl) containerURL).setParameters(null);
                pathInfo = buildHstURLPath(containerURL);
            } finally {
                containerURL.setResourceWindowReferenceNamespace(resourceWindowReferenceNamespace);
                ((HstContainerURLImpl) containerURL).setPathInfo(oldPathInfo);
                ((HstContainerURLImpl) containerURL).setParameters(oldParamMap);
            }
            
            path.append(getVirtualizedServletPath(containerURL, requestContext, pathInfo));
        } else {
            pathInfo = buildHstURLPath(containerURL);
            
            if (!this.portletResourceURLEnabled) {
                VirtualHost virtualHost = requestContext.getVirtualHost();
                containerResource = (virtualHost != null && virtualHost.getVirtualHosts().isExcluded(pathInfo));
            }
            
            if (!containerResource) {
                path.append(containerURL.getServletPath());
            }
        }
        
        path.append(pathInfo);
        
        if (containerResource) {
            path.insert(0, getVirtualizedContextPath(containerURL, requestContext, pathInfo));
            urlString = path.toString();
        } else {
            urlString = path.toString();
            
            BaseURL url = null;
            PortletResponse response = HstContainerPortletContext.getCurrentResponse();
            
            if (response instanceof MimeResponse) {
                MimeResponse mimeResponse = (MimeResponse) response;
                
                if (containerURL.getActionWindowReferenceNamespace() != null) {
                    url = mimeResponse.createActionURL();
                } else if (resourceWindowReferenceNamespace != null) {
                    url = mimeResponse.createResourceURL();
                    ((ResourceURL) url).setResourceID(containerURL.getResourceId());
                } else {
                    url = mimeResponse.createRenderURL();
                }
                
                url.setParameter(HstContainerPortlet.HST_PATH_PARAM_NAME, path.toString());
                
                urlString = url.toString();
            }
        }
        
        return urlString;
    }
    
    public String toURLString(HstContainerURL containerURL, HstRequestContext requestContext, String contextPath) throws UnsupportedEncodingException, ContainerException {
        String urlString = "";
        
        String resourceWindowReferenceNamespace = containerURL.getResourceWindowReferenceNamespace();
        boolean containerResource = false;
        
        if (!this.portletResourceURLEnabled) {
            containerResource = ContainerConstants.CONTAINER_REFERENCE_NAMESPACE.equals(resourceWindowReferenceNamespace);
        }
        
        StringBuilder path = new StringBuilder(100);
        String pathInfo = null;
        
        if (containerResource) {
            String oldPathInfo = containerURL.getPathInfo();
            String resourcePath = containerURL.getResourceId();
            Map<String, String[]> oldParamMap = containerURL.getParameterMap();
            
            try {
                containerURL.setResourceWindowReferenceNamespace(null);
                ((HstContainerURLImpl) containerURL).setPathInfo(resourcePath);
                ((HstContainerURLImpl) containerURL).setParameters(null);
                pathInfo = buildHstURLPath(containerURL);
            } finally {
                containerURL.setResourceWindowReferenceNamespace(resourceWindowReferenceNamespace);
                ((HstContainerURLImpl) containerURL).setPathInfo(oldPathInfo);
                ((HstContainerURLImpl) containerURL).setParameters(oldParamMap);
            }
            
            path.append(getVirtualizedServletPath(containerURL, requestContext, pathInfo));
        } else {
            pathInfo = buildHstURLPath(containerURL);
            
            if (!this.portletResourceURLEnabled) {
                VirtualHost virtualHost = requestContext.getVirtualHost();
                containerResource = (virtualHost != null && virtualHost.getVirtualHosts().isExcluded(pathInfo));
            }
            
            if (!containerResource) {
                path.append(containerURL.getServletPath());
            }
        }
        
        path.append(pathInfo);
        
        if (containerResource) {
            path.insert(0, contextPath);
            urlString = path.toString();
        } else {
            urlString = path.toString();
            
            BaseURL url = null;
            PortletResponse response = HstContainerPortletContext.getCurrentResponse();
            
            if (response instanceof MimeResponse) {
                MimeResponse mimeResponse = (MimeResponse) response;
                
                if (containerURL.getActionWindowReferenceNamespace() != null) {
                    url = mimeResponse.createActionURL();
                } else if (resourceWindowReferenceNamespace != null) {
                    url = mimeResponse.createResourceURL();
                    ((ResourceURL) url).setResourceID(containerURL.getResourceId());
                } else {
                    url = mimeResponse.createRenderURL();
                }
                
                url.setParameter(HstContainerPortlet.HST_PATH_PARAM_NAME, path.toString());
                
                urlString = url.toString();
            }
        }
        
        return urlString;
    }

}
