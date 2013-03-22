/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.component.HstResourceResponseImpl;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * ResourceServingValve
 */
public class ResourceServingValve extends AbstractBaseOrderableValve {
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HstRequestContext requestContext = context.getRequestContext();
        HstContainerURL baseURL = requestContext.getBaseURL();
        String resourceWindowRef = baseURL.getResourceWindowReferenceNamespace();

        if (resourceWindowRef == null) {
            // not a resource request, so skip it..
            context.invokeNext();
            return;
        }

        if (context.getServletResponse().isCommitted()) {
            log.warn("Stopping resource serving. The response is already committed.");
            context.invokeNext();
            return;
        }

        HttpServletRequest servletRequest = context.getServletRequest();
        HttpServletResponse servletResponse = context.getServletResponse();

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

        HstRequest request = new HstRequestImpl(servletRequest, requestContext, window, HstRequest.RESOURCE_PHASE);
        HstResponse response = new HstResourceResponseImpl(servletResponse, requestContext, window);

        HstComponentInvoker invoker = getComponentInvoker();

        invoker.invokeBeforeServeResource(context.getRequestContainerConfig(), request, response);

        // page error handling...
        PageErrors pageErrors = getPageErrors(new HstComponentWindow [] { window }, true);

        if (pageErrors != null) {
            handleComponentExceptions(pageErrors, context.getRequestContainerConfig(), window, request, response);
        }

        invoker.invokeServeResource(context.getRequestContainerConfig(), request, response);

        // page error handling...
        pageErrors = getPageErrors(new HstComponentWindow [] { window }, true);

        if (pageErrors != null) {
            handleComponentExceptions(pageErrors, context.getRequestContainerConfig(), window, request, response);
        }
    }
}
