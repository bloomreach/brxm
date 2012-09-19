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

import java.io.IOException;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.component.HstResourceResponseImpl;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * ResourceServingValve
 * 
 * @version $Id$
 */
public class ResourceServingValve extends AbstractValve {
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {
        ServletRequest servletRequest = context.getServletRequest();
        HttpServletResponse servletResponse = context.getServletResponse();
        HstRequestContext requestContext = context.getRequestContext();

        if (!context.getServletResponse().isCommitted() && requestContext.getBaseURL().getResourceWindowReferenceNamespace() != null) {
            HstContainerURL baseURL = requestContext.getBaseURL();
            String resourceWindowRef = baseURL.getResourceWindowReferenceNamespace();
            HstComponentWindow window = findComponentWindow(context.getRootComponentWindow(), resourceWindowRef);

            if (window == null) {
                log.warn("Illegal request for resource URL found because there is no component for id '{}' for matched " +
                        "sitemap item '{}'. Set 404 on response.", resourceWindowRef, requestContext.getResolvedSiteMapItem().getHstSiteMapItem().getId());
                try {
                    servletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
                } catch (IOException e) {
                    throw new ContainerException("Unable to set 404 on response after invalid resource path.", e);
                }
                return;
            }

            HstRequest request = new HstRequestImpl((HttpServletRequest) servletRequest, requestContext, window, HstRequest.RESOURCE_PHASE);
            HstResponse response = new HstResourceResponseImpl(servletResponse, window);

            HstComponentInvoker invoker = getComponentInvoker();

            invoker.invokeBeforeServeResource(context.getRequestContainerConfig(), request, response);

            // page error handling...
            PageErrors pageErrors = getPageErrors(new HstComponentWindow [] { window }, true);
            if (pageErrors != null) {
                PageErrorHandler.Status handled = handleComponentExceptions(pageErrors, context.getRequestContainerConfig(), window, request, response);
                if (handled == PageErrorHandler.Status.HANDLED_TO_STOP) {
                    context.invokeNext();
                    return;
                }
            }

            invoker.invokeServeResource(context.getRequestContainerConfig(), request, response);

            // page error handling...
            pageErrors = getPageErrors(new HstComponentWindow [] { window }, true);
            if (pageErrors != null) {
                PageErrorHandler.Status handled = handleComponentExceptions(pageErrors, context.getRequestContainerConfig(), window, request, response);
                if (handled == PageErrorHandler.Status.HANDLED_TO_STOP) {
                    context.invokeNext();
                    return;
                }
            }

        }
        
        // continue
        context.invokeNext();
    }
}
