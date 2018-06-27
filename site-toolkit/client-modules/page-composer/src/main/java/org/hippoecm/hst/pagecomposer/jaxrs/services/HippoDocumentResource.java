/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.services;


import java.util.concurrent.Callable;

import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.pagecomposer.jaxrs.model.treepicker.AbstractTreePickerRepresentation;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;

import static org.hippoecm.hst.pagecomposer.jaxrs.model.treepicker.DocumentTreePickerRepresentation.representExpandedParentTree;
import static org.hippoecm.hst.pagecomposer.jaxrs.model.treepicker.DocumentTreePickerRepresentation.representRequestContentNode;

@Path("/"+ HippoNodeType.NT_DOCUMENT+"/")
@Produces(MediaType.APPLICATION_JSON)
public class HippoDocumentResource extends AbstractConfigResource {


    @GET
    @Path("/picker")
    public Response getRoot() {
        return tryGet(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final AbstractTreePickerRepresentation representation;
                representation =  representRequestContentNode(getPageComposerContextService());
                return ok("Folder loaded successfully", representation);
            }
        });
    }

    /**
     * @param siteMapItemRefIdOrPath
     * @return the rest response to create the client tree for <code>siteMapPathInfo</code> : the response contains all
     * ancestor nodes + their siblings up to the 'channel content root node' plus the siblings for <code>siteMapPathInfo</code>
     * plus its direct children.
     */
    @GET
    @Path("/picker/{siteMapItemRefIdOrPath: .*}")
    public Response get(final @PathParam("siteMapItemRefIdOrPath") String siteMapItemRefIdOrPath) {
        return tryGet(new Callable<Response>() {
            @Override
            public Response call() throws Exception {

                final AbstractTreePickerRepresentation representation;
                if (StringUtils.isEmpty(siteMapItemRefIdOrPath)) {
                    representation = representRequestContentNode(getPageComposerContextService());
                } else {
                    // find first the mount for current request
                    HttpSession session = getPageComposerContextService().getRequestContext().getServletRequest().getSession();
                    // TODO HSTTWO-4374 can we share this information cleaner between platform webapp and site webapps?
                    final CmsSessionContext cmsSessionContext = CmsSessionContext.getContext(session);
                    final String renderingHost = (String)cmsSessionContext.getContextPayload().get(ContainerConstants.RENDERING_HOST);
                    final VirtualHost virtualHost = getPageComposerContextService().getRequestContext().getResolvedMount().getMount().getVirtualHost();
                    final Mount editingMount = getPageComposerContextService().getEditingMount();
                    final ResolvedMount resolvedMount = virtualHost.getVirtualHosts().matchMount(renderingHost, null, editingMount.getMountPath());
                    representation = representExpandedParentTree(getPageComposerContextService(),resolvedMount,  siteMapItemRefIdOrPath);
                }
                return ok("Folder loaded successfully", representation);
            }
        });
    }
}
