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

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.ResourceLifecycleManagement;
import org.hippoecm.hst.core.request.HstEmbeddedRequestContext;
import org.hippoecm.hst.core.request.MatchedMapping;
import org.hippoecm.hst.site.request.HstRequestContextImpl;

public class InitializationValve extends AbstractValve
{
    protected List<ResourceLifecycleManagement> resourceLifecycleManagements;
    
    public void setResourceLifecycleManagements(List<ResourceLifecycleManagement> resourceLifecycleManagements) {
        this.resourceLifecycleManagements = resourceLifecycleManagements;
    }
    
    @Override
    public void invoke(ValveContext context) throws ContainerException
    {
        HttpServletRequest servletRequest = context.getServletRequest();
        HstRequestContextImpl requestContext = (HstRequestContextImpl) servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);

        if (requestContext == null) {
            requestContext = (HstRequestContextImpl)getRequestContextComponent().create(servletRequest, context.getServletResponse(), getContainerConfiguration());
            servletRequest.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, requestContext);
        }
        HstEmbeddedRequestContext erc = (HstEmbeddedRequestContext)servletRequest.getAttribute(ContainerConstants.HST_EMBEDDED_REQUEST_CONTEXT);
        
        if (erc != null ) {
            requestContext.setEmbeddedRequestContext(erc);
        }
        
        // if there is a Mapping on the request, it is set on the HstRequestContext.
        MatchedMapping matchedMapping = (MatchedMapping) servletRequest.getAttribute(ContainerConstants.MATCHED_MAPPING);
        
        if (matchedMapping != null) {
            requestContext.setMatchedMapping(matchedMapping);
        }
        
        if (this.resourceLifecycleManagements != null) {
            for (ResourceLifecycleManagement resourceLifecycleManagement : this.resourceLifecycleManagements) {
                resourceLifecycleManagement.disposeAllResources();
                resourceLifecycleManagement.setActive(true);
            }
        }
        
        // continue
        context.invokeNext();
    }
}
