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

import org.hippoecm.hst.core.request.HstRequestContext;

public class HstContainerURLProviderImpl extends AbstractHstContainerURLProvider {
    
    @Override
    public String toURLString(HstContainerURL containerURL, HstRequestContext requestContext) throws UnsupportedEncodingException, ContainerException {
        return toURLString(containerURL, requestContext, null);
    }
    
    public String toURLString(HstContainerURL containerURL, HstRequestContext requestContext, String contextPath) throws UnsupportedEncodingException, ContainerException {
        String resourceWindowReferenceNamespace = containerURL.getResourceWindowReferenceNamespace();
        String path = null;
        
        if (ContainerConstants.CONTAINER_REFERENCE_NAMESPACE.equals(resourceWindowReferenceNamespace)) {
            String oldPathInfo = containerURL.getPathInfo();
            String resourcePath = containerURL.getResourceId();
            Map<String, String[]> oldParamMap = containerURL.getParameterMap();

            try {
                containerURL.setResourceWindowReferenceNamespace(null);
                ((HstContainerURLImpl) containerURL).setPathInfo(resourcePath);
                ((HstContainerURLImpl) containerURL).setParameters(null);
                path = buildHstURLPath(containerURL);
            } finally {
                containerURL.setResourceWindowReferenceNamespace(resourceWindowReferenceNamespace);
                ((HstContainerURLImpl) containerURL).setPathInfo(oldPathInfo);
                ((HstContainerURLImpl) containerURL).setParameters(oldParamMap);
            }
        } else {
            path = buildHstURLPath(containerURL);
        }
        
        return new StringBuilder(100)
        .append(contextPath != null ? contextPath : getVirtualizedContextPath(containerURL, requestContext, path))
        .append(getVirtualizedServletPath(containerURL, requestContext, path))
        .append(path)
        .toString();
    }
    
}
