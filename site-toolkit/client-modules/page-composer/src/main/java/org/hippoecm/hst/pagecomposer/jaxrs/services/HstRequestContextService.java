/*
 * Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService;

public class HstRequestContextService {

    public HstRequestContext getRequestContext() {
        return RequestContextProvider.get();
    }

    public String getRequestConfigIdentifier() {
        return (String) getRequestContext().getAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER);
    }

    public String getRenderingMountId() {
        final String renderingMountId = (String)getRequestContext().getServletRequest().getSession(true).getAttribute(ContainerConstants.CMS_REQUEST_RENDERING_MOUNT_ID);
        if (renderingMountId == null) {
            throw new IllegalStateException("Cound not find rendering mount id on request session.");
        }
        return renderingMountId;
    }

    public HstSite getEditingPreviewSite() {
        final Mount liveMount = getEditingMount();
        return castToContextualizableMount(liveMount).getPreviewHstSite();
    }

    public Mount getEditingMount() {
        final HstRequestContext requestContext = getRequestContext();
        final String renderingMountId = getRenderingMountId();
        Mount mount = requestContext.getVirtualHost().getVirtualHosts().getMountByIdentifier(renderingMountId);
        if (mount == null) {
            throw new IllegalStateException("Cound not find a Mount for identifier + '"+renderingMountId+"'");
        }
        return mount;
    }


    private ContextualizableMount castToContextualizableMount(Mount liveMount) throws IllegalStateException{
        if (liveMount instanceof  ContextualizableMount) {
            return ContextualizableMount.class.cast(liveMount);
        } else {
            throw new IllegalStateException("Expected a mount of type "+ContextualizableMount.class.getName()+"" +
                " but found '"+liveMount.toString()+"' which is of type " + liveMount.getClass().getName());
        }
    }
}
