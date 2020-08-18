/*
 *  Copyright 2020 Bloomreach
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

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.api.annotation.PrivilegesAllowed;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapPageRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges;
import org.hippoecm.repository.api.HippoNodeType;

import static java.lang.String.format;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError.INVALID_NODE_TYPE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError.INVALID_UUID;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError.ITEM_NOT_FOUND;

@Path("/" + HippoNodeType.NT_HANDLE + "/")
@Produces(MediaType.APPLICATION_JSON)
public class HandleResource extends AbstractConfigResource {

    @GET
    @Path("/representation")
    @PrivilegesAllowed(ChannelManagerPrivileges.CHANNEL_VIEWER_PRIVILEGE_NAME)
    public Response getRepresentation() {
        return tryGet(() -> {
            final PageComposerContextService pageComposerContextService = getPageComposerContextService();
            final Mount editingMount = pageComposerContextService.getEditingMount();
            final HstRequestContext requestContext = pageComposerContextService.getRequestContext();

            try {
                final Node handle = requestContext.getSession().getNodeByIdentifier(
                        pageComposerContextService.getRequestConfigIdentifier());
                if (!handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                    throw new ClientException(format("Expected a handle at '%s'", handle.getPath()), INVALID_NODE_TYPE);
                }

                final HstLink hstLink = requestContext.getHstLinkCreator().create(handle, editingMount);
                if (hstLink.isNotFound()) {
                    throw new ClientException(format("Cannot create a link for item '%s'", handle.getPath()),
                            INVALID_UUID);
                }

                final SiteMapPageRepresentation xPageRepresentation = new SiteMapPageRepresentation().represent(hstLink,
                        handle);

                return ok("Folder loaded successfully", xPageRepresentation);
            } catch (ItemNotFoundException e) {
                throw new ClientException("Invalid item", ITEM_NOT_FOUND);
            }
        });
    }
}
