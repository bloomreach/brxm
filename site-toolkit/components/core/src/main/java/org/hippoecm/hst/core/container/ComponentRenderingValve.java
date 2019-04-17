/*
 *  Copyright 2011-2019 Hippo B.V. (http://www.onehippo.com)
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

import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * ComponentRenderingValve
 */
public class ComponentRenderingValve extends AbstractBaseOrderableValve {

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        final HstRequestContext requestContext = context.getRequestContext();
        final String componentRenderingWindowReferenceNamespace = requestContext.getBaseURL().getComponentRenderingWindowReferenceNamespace();

        if (componentRenderingWindowReferenceNamespace == null) {
            // not a compoment rendering request, so skip it..
            context.invokeNext();
            return;
        }
        final HstComponentWindow window = context.getRootComponentWindow();
        final HttpServletResponse servletResponse = requestContext.getServletResponse();

        if (requestContext.isChannelManagerPreviewRequest() || requestContext.isPreview()) {
            setNoCacheHeaders(servletResponse);
        }

        if (!window.getComponentInfo().isStandalone()) {
            // set the rendering window firsst
            context.setRootComponentRenderingWindow(window);
            // set the sitemap item root window as the root window because the backing componentInfo is standalone
            HstComponentWindow root = window;
            while(root.getParentWindow() != null) {
                root = root.getParentWindow();
            }
            context.setRootComponentWindow(root);
        }
        context.invokeNext();
    }

    private static void setNoCacheHeaders(final HttpServletResponse response) {
        response.setDateHeader("Expires", -1);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
    }

}
