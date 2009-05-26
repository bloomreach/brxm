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

import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.MatchedMapping;

public class HstContainerURLProviderImpl extends AbstractHstContainerURLProvider {
    
    @Override
    public String toURLString(HstContainerURL containerURL, HstRequestContext requestContext) throws UnsupportedEncodingException, ContainerException {
        String resourceWindowReferenceNamespace = containerURL.getResourceWindowReferenceNamespace();
        String contextPath = containerURL.getContextPath();
        String servletPath = containerURL.getServletPath();
        String path = null;
        
        if (ContainerConstants.CONTAINER_REFERENCE_NAMESPACE.equals(resourceWindowReferenceNamespace)) {
            String oldPathInfo = containerURL.getPathInfo();
            try {
                containerURL.setResourceWindowReferenceNamespace(null);
                ((HstContainerURLImpl) containerURL).setPathInfo(containerURL.getResourceId());
                path = buildHstURLPath(containerURL);
            } finally {
                containerURL.setResourceWindowReferenceNamespace(resourceWindowReferenceNamespace);
                ((HstContainerURLImpl) containerURL).setPathInfo(oldPathInfo);
            }
        } else {
            path = buildHstURLPath(containerURL);
        }
        
        String externalContextPath = null;
        String externalServletPath = null;
        
        if(requestContext != null) {
            // do stuff
            if (requestContext != null) {
                HstContainerURL baseURL = requestContext.getBaseURL();

                if (baseURL != null) {
                    if (requestContext.getMatchedMapping() != null && path != null) {
                        MatchedMapping matchedMapping = requestContext.getMatchedMapping();
                        if (matchedMapping.getMapping() != null) {
                            if (matchedMapping.getMapping().getVirtualHost().isContextPathInUrl()) {
                                externalContextPath = baseURL.getContextPath();
                            } else {
                                externalContextPath = "";
                            }
                            
                            if(matchedMapping.getMapping().getVirtualHost().getVirtualHosts().isExcluded(path)) {
                                // if the path is an excluded path defined in virtual hosting (for example /binaries), we do not include
                                // a servletpath in the url
                                externalServletPath = "";
                            } else {
                                // as the external url is mapped, get the external 'fake' servletpath
                                externalServletPath = matchedMapping.getMapping().getUriPrefix();
                                if (externalServletPath == null) {
                                    externalServletPath = "";
                                }
                            }
                            if (externalServletPath.endsWith("/")) {
                                externalServletPath = externalServletPath.substring(0, externalServletPath.length() - 1);
                            }
                        }
                    }
                }
            }
        }
        
        StringBuilder url = new StringBuilder(100);
        
        if(externalContextPath == null) {
            url.append(contextPath);
        } else {
            url.append(externalContextPath);
        }
        
        if(externalServletPath == null) {
            url.append(servletPath);
        } else {
            url.append(externalServletPath);
        }
        
        url.append(path);
        
        return url.toString();
    }
    
}
