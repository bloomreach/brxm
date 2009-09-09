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

import org.hippoecm.hst.site.request.HstRequestContextImpl;

public class HstURLValve extends AbstractValve {
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {

        ServletRequest servletRequest = context.getServletRequest();
        HstRequestContextImpl requestContext = (HstRequestContextImpl) servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);

        String contextNamespace = (String) servletRequest.getAttribute(ContainerConstants.CONTEXT_NAMESPACE_ATTRIBUTE);
        
        if (contextNamespace != null) {
            requestContext.setContextNamespace(contextNamespace);
        }
        
        String containerPathInfo = (String) servletRequest.getAttribute(ContainerConstants.HST_CONTAINER_PATH_INFO);
        HstContainerURL baseURL = null;

        // TODO: if requestContext.getBaseURL() != null, is there a reason or condition why we need to parse again?
        if (containerPathInfo != null) {
            baseURL = getUrlFactory().getServletUrlProvider().parseURL(context.getServletRequest(), context.getServletResponse(), requestContext, containerPathInfo);
        } else {
            baseURL = getUrlFactory().getServletUrlProvider().parseURL(context.getServletRequest(), context.getServletResponse(), requestContext);
        }
        
        requestContext.setBaseURL(baseURL);
        requestContext.setURLFactory(getUrlFactory());
        requestContext.setLinkCreator(getLinkCreator());
        requestContext.setSiteMapMatcher(getSiteMapMatcher());
        requestContext.setHstQueryManagerFactory(getHstQueryManagerFactory());
        
        // continue
        context.invokeNext();
    }
}
