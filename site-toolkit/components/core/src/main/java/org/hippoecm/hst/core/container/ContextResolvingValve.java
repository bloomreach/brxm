/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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

import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;

/**
 * ContextResolvingValve.
 */
public class ContextResolvingValve extends AbstractBaseOrderableValve {
    
    @Override
    public void invoke(ValveContext context) throws ContainerException
    {
        HstMutableRequestContext requestContext = (HstMutableRequestContext) context.getRequestContext();

        ResolvedSiteMapItem resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();
        
        if (resolvedSiteMapItem == null) {
            // if there is no ResolvedSiteMapItem on the request we cannot continue
            throw new ContainerException("No resolvedSiteMapItem found for this request. Cannot continue request processing");
        }

        HstComponentConfiguration rootComponentConfig = resolvedSiteMapItem.getHstComponentConfiguration();

        if (rootComponentConfig == null) {
            throw new ContainerNotFoundException(String.format("Resolved siteMapItem '%s' does not contain a ComponentConfiguration that can be resolved.",
                    resolvedSiteMapItem.getHstSiteMapItem().getQualifiedId()));
        }
        if (rootComponentConfig.isMarkedDeleted()) {
            throw new ContainerNotFoundException(String.format("Resolved siteMapItem '%s' points to a component that is marked for deletion.",
                    resolvedSiteMapItem.getHstSiteMapItem().getQualifiedId()));
        }

        try {
            HstComponentWindow rootComponentWindow = createRootComponentWindow(context, rootComponentConfig);

            final HstContainerURL baseURL = requestContext.getBaseURL();
            final String resourceWindowRef = baseURL.getResourceWindowReferenceNamespace();
            final String actionWindowReferenceNamespace = baseURL.getActionWindowReferenceNamespace();
            final String componentRenderingWindowReferenceNamespace = baseURL.getComponentRenderingWindowReferenceNamespace();

            if (resourceWindowRef != null) {
                rootComponentWindow = findComponentWindow(rootComponentWindow, resourceWindowRef);
                if (rootComponentWindow == null) {
                    notFound("resource", resourceWindowRef, context);
                    return;
                }
                log.info("Found action request '{}' targeting component '{}'.", context.getServletRequest(),
                        rootComponentWindow.getComponent().getComponentConfiguration());
            } else if (actionWindowReferenceNamespace != null) {
                rootComponentWindow = findComponentWindow(rootComponentWindow, actionWindowReferenceNamespace);
                if (rootComponentWindow == null) {
                    notFound("action", actionWindowReferenceNamespace, context);
                    return;
                }
                log.info("Found resource request '{}' targeting component '{}'.", context.getServletRequest(),
                        rootComponentWindow.getComponent().getComponentConfiguration());

            } else if (componentRenderingWindowReferenceNamespace != null) {
                rootComponentWindow = findComponentWindow(rootComponentWindow, componentRenderingWindowReferenceNamespace);
                if (rootComponentWindow == null) {
                    notFound("component rendering", componentRenderingWindowReferenceNamespace, context);
                    return;
                }
                log.info("Found component rendering request '{}' targeting component '{}'.", context.getServletRequest(),
                        rootComponentWindow.getComponent().getComponentConfiguration());
            }

            context.setRootComponentWindow(rootComponentWindow);

        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to create component windows", e);
            } else if (log.isWarnEnabled()) {
                log.warn("Failed to create component windows: {}", e.toString());
            }
            
            throw new ContainerException("Failed to create component window for the configuration: " + rootComponentConfig.getId(), e);
        }
        
        // continue
        context.invokeNext();
    }

    /**
     * Create root component window from {@code rootComponentConfig}.
     * @param context valve context
     * @param rootComponentConfig root component config
     * @return root component window from {@code rootComponentConfig}
     */
    protected HstComponentWindow createRootComponentWindow(ValveContext context,
            HstComponentConfiguration rootComponentConfig) {
        HstComponentWindow rootComponentWindow = getComponentWindowFactory().create(context.getRequestContainerConfig(),
                context.getRequestContext(), rootComponentConfig, getComponentFactory());
        return rootComponentWindow;
    }

    private void notFound(final String type, final String componentRenderingWindowReferenceNamespace, final ValveContext context) throws ContainerException {
        log.warn("Illegal request for {} URL found because there is no component for id '{}' for matched " +
                "sitemap item '{}'. Set 404 on response for request '{}'.", type, componentRenderingWindowReferenceNamespace,
                context.getRequestContext().getResolvedSiteMapItem().getHstSiteMapItem().getId(), context.getServletRequest());
        try {
            context.getServletResponse().sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (IOException e) {
            throw new ContainerException("Unable to set 404 on response after invalid resource path.", e);
        }
    }
}
