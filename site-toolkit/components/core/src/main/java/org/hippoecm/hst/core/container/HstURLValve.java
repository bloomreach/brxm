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

import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.request.HstRequestContextImpl;

public class HstURLValve extends AbstractValve {
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {

        ServletRequest servletRequest = context.getServletRequest();
        HstRequestContext requestContext = (HstRequestContext) servletRequest.getAttribute(HstRequestContext.class.getName());

        String contextNamespace = (String) servletRequest.getAttribute(ContainerConstants.CONTEXT_NAMESPACE_ATTRIBUTE);
        
        if (contextNamespace != null) {
            ((HstRequestContextImpl) requestContext).setContextNamespace(contextNamespace);
        }
        
        HstContainerURL baseURL = getUrlFactory().getServletUrlProvider().parseURL(context.getServletRequest(), context.getServletResponse());
        
        ((HstRequestContextImpl) requestContext).setBaseURL(baseURL);
        ((HstRequestContextImpl) requestContext).setURLFactory(getUrlFactory());
        ((HstRequestContextImpl) requestContext).setLinkCreator(getLinkCreator());
        ((HstRequestContextImpl) requestContext).setSiteMapMatcher(getSiteMapMatcher());
        ((HstRequestContextImpl) requestContext).setCtxWhereClauseComputer(getCtxWhereClauseComputer());
        
        // continue
        context.invokeNext();
    }

}
