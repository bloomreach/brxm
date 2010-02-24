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

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.hosting.VirtualHosts;
import org.hippoecm.hst.core.request.MatchedMapping;
import org.hippoecm.hst.site.request.HstRequestContextImpl;
import org.hippoecm.hst.util.HstRequestUtils;

/**
 * SiteResolvingValve
 * 
 * @version $Id$
 */
public class SiteResolvingValve extends AbstractValve {
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HstRequestContextImpl requestContext = (HstRequestContextImpl) context.getServletRequest().getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
        HstContainerURL baseURL = requestContext.getBaseURL();

        MatchedMapping matchedMapping = requestContext.getMatchedMapping();
        
        if (matchedMapping == null) {
            String hostName = HstRequestUtils.getRequestServerName(context.getServletRequest());
            
            VirtualHosts vHosts = this.virtualHostsManager.getVirtualHosts();
            
            if (vHosts == null) {
                throw new ContainerException("Hosts are not properly initialized");
            }
            
            String contextRelativePath = baseURL.getServletPath() + baseURL.getPathInfo();
            matchedMapping = vHosts.findMapping(hostName, contextRelativePath);
            
            if (matchedMapping == null && vHosts.getDefaultHostName() != null) {
                matchedMapping = vHosts.findMapping(vHosts.getDefaultHostName(), contextRelativePath);
            }
            
            if (matchedMapping == null) {
                throw new ContainerException("No proper configuration found for host : " + hostName);
            }
            
            requestContext.setMatchedMapping(matchedMapping);
        }
        
        if (StringUtils.isEmpty(matchedMapping.getSiteName())) {
            throw new ContainerException("No siteName found for matchedMapping. Configure one in your virtual hosting.");
        }
        
        // temp solution to know whether we are in preview
        if (StringUtils.equals(baseURL.getServletPath(), getContainerConfiguration().getString("preview.servlet.path"))) {
            requestContext.setAttribute(ContainerConstants.IS_PREVIEW, Boolean.TRUE);
        } else {
            requestContext.setAttribute(ContainerConstants.IS_PREVIEW, Boolean.FALSE);
        }
        
        // continue
        context.invokeNext();
    }
    
}
