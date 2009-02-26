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
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstResponseImpl;
import org.hippoecm.hst.core.component.HstResponseState;
import org.hippoecm.hst.core.request.HstRequestContext;

public class ResourceServingValve extends AbstractValve {
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {
        ServletRequest servletRequest = context.getServletRequest();
        ServletResponse servletResponse = context.getServletResponse();
        HstRequestContext requestContext = (HstRequestContext) servletRequest.getAttribute(HstRequestContext.class.getName());

        if (!context.getServletResponse().isCommitted() && requestContext.getBaseURL().getResourceWindowReferenceNamespace() != null) {
            HstContainerURL baseURL = requestContext.getBaseURL();
            HstComponentWindow window = findResourceServingWindow(context.getRootComponentWindow(), baseURL.getResourceWindowReferenceNamespace());
            
            if (window != null) {
                HstRequest request = new HstRequestImpl((HttpServletRequest) servletRequest, requestContext, window);
                HstResponseState responseState = new HstResponseState((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse);
                HstResponse response = new HstResponseImpl((HttpServletResponse) servletResponse, requestContext, window, responseState);
                ((HstComponentWindowImpl) window).setResponseState(responseState);
                
                HstComponentInvoker invoker = getComponentInvoker();
                invoker.invokeBeforeServeResource(context.getServletConfig(), request, response);
                invoker.invokeServeResource(context.getServletConfig(), request, response);

                if (window.hasComponentExceptions() && log.isWarnEnabled()) {
                    for (HstComponentException hce : window.getComponentExceptions()) {
                        log.warn("Component exceptions found: " + hce.getMessage(), hce);
                    }
                    window.clearComponentExceptions();
                }
            }
        }
        
        // continue
        context.invokeNext();
    }

    protected HstComponentWindow findResourceServingWindow(HstComponentWindow rootWindow, String resourceServingWindowReferenceNamespace) {
        HstComponentWindow resourceServingWindow = null;
        
        String rootReferenceNamespace = rootWindow.getReferenceNamespace();
        
        if (rootReferenceNamespace.equals(resourceServingWindowReferenceNamespace)) {
            resourceServingWindow = rootWindow;
        } else {
            String [] referenceNamespaces = resourceServingWindowReferenceNamespace.split(getComponentWindowFactory().getReferenceNameSeparator());
            int start = ((referenceNamespaces.length > 0 && rootReferenceNamespace.equals(referenceNamespaces[0])) ? 1 : 0);
            
            HstComponentWindow tempWindow = rootWindow;
            int index = start;
            for ( ; index < referenceNamespaces.length; index++) {
                if (tempWindow != null) {
                    tempWindow = tempWindow.getChildWindowByReferenceName(referenceNamespaces[index]);
                } else {
                    break;
                }
            }
            
            if (index == referenceNamespaces.length) {
                resourceServingWindow = tempWindow;
            }
        }
        
        return resourceServingWindow;
    }
}
