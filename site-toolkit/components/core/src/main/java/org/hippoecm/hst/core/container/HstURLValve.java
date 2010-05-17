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

import javax.servlet.ServletRequest;

import org.hippoecm.hst.core.request.ResolvedSiteMount;
import org.hippoecm.hst.site.request.HstRequestContextImpl;

/**
 * HstURLValve
 * 
 * @version $Id$
 */
public class HstURLValve extends AbstractValve {
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {

        ServletRequest servletRequest = context.getServletRequest();
        HstRequestContextImpl requestContext = (HstRequestContextImpl) servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);

        String contextNamespace = (String) servletRequest.getAttribute(ContainerConstants.CONTEXT_NAMESPACE_ATTRIBUTE);
        
        if (contextNamespace != null) {
            requestContext.setContextNamespace(contextNamespace);
        }
        
        requestContext.setURLFactory(getUrlFactory());
        requestContext.setLinkCreator(getLinkCreator());
        requestContext.setSiteMapMatcher(getSiteMapMatcher());
        requestContext.setHstQueryManagerFactory(getHstQueryManagerFactory());
        
        ResolvedSiteMount resolvedSiteMount = (ResolvedSiteMount) servletRequest.getAttribute(ContainerConstants.RESOLVED_SITEMOUNT);
        
        if (resolvedSiteMount != null) {
            requestContext.setResolvedSiteMount(resolvedSiteMount);
        }
        
        HstContainerURL baseURL = (HstContainerURL)servletRequest.getAttribute(HstContainerURL.class.getName());
        
        if(baseURL == null) {
            baseURL = requestContext.getContainerURLProvider().parseURL(servletRequest, context.getServletResponse(), requestContext);
        }
        
        requestContext.setBaseURL(baseURL);
        // continue
        context.invokeNext();
    }
}
