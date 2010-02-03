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

import java.util.Collection;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.configuration.components.HstComponentInfo;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.component.HstResourceResponseImpl;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.KeyValue;

/**
 * ResourceServingValve
 * 
 * @version $Id$
 */
public class ResourceServingValve extends AbstractValve {
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {
        ServletRequest servletRequest = context.getServletRequest();
        ServletResponse servletResponse = context.getServletResponse();
        HstRequestContext requestContext = (HstRequestContext) servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);

        if (!context.getServletResponse().isCommitted() && requestContext.getBaseURL().getResourceWindowReferenceNamespace() != null) {
            HstContainerURL baseURL = requestContext.getBaseURL();
            String resourceWindowRef = baseURL.getResourceWindowReferenceNamespace();
            HstComponentWindow window = null;
            
            if (getContainerConfiguration().isDevelopmentMode() && resourceWindowRef.endsWith(getTraceToolComponentName())) {
                window = createTraceToolComponent(context, requestContext, null);
            }
            
            if (window == null) {
                window = findComponentWindow(context.getRootComponentWindow(), resourceWindowRef);
            }
            
            if (window != null) {
                HstRequest request = new HstRequestImpl((HttpServletRequest) servletRequest, requestContext, window, HstRequest.RESOURCE_PHASE);
                HstResponse response = new HstResourceResponseImpl((HttpServletResponse) servletResponse, window);
                
                HstComponentInvoker invoker = getComponentInvoker();
                
                invoker.invokeBeforeServeResource(context.getRequestContainerConfig(), request, response);
                
                // page error handling...
                Collection<KeyValue<HstComponentInfo, Collection<HstComponentException>>> componentExceptions = getComponentExceptions(new HstComponentWindow [] { window }, true);
                if (componentExceptions != null && !componentExceptions.isEmpty()) {
                    Object handled = handleComponentExceptions(componentExceptions, context.getRequestContainerConfig(), window, request, response);
                    if (handled == PageErrorHandler.HANDLED_TO_STOP) {
                        context.invokeNext();
                        return;
                    }
                }
                
                invoker.invokeServeResource(context.getRequestContainerConfig(), request, response);
                
                // page error handling...
                componentExceptions = getComponentExceptions(new HstComponentWindow [] { window }, true);
                if (componentExceptions != null && !componentExceptions.isEmpty()) {
                    Object handled = handleComponentExceptions(componentExceptions, context.getRequestContainerConfig(), window, request, response);
                    if (handled == PageErrorHandler.HANDLED_TO_STOP) {
                        context.invokeNext();
                        return;
                    }
                }
            }
        }
        
        // continue
        context.invokeNext();
    }
}
