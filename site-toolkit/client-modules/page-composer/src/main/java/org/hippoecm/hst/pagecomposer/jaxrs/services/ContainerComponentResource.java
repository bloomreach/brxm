/*
 *  Copyright 2010-2016 Hippo B.V. (http://www.onehippo.com)
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

import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ErrorStatus;
import org.hippoecm.hst.pagecomposer.jaxrs.services.ContainerComponentService.ContainerItem;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/hst:containercomponent/")
public class ContainerComponentResource extends AbstractConfigResource {
    private static Logger log = LoggerFactory.getLogger(ContainerComponentResource.class);

    private ContainerComponentService containerComponentService;

    public void setContainerComponentService(ContainerComponentService containerComponentService) {
        this.containerComponentService = containerComponentService;
    }

    @FunctionalInterface
    interface ContainerAction<Response> {
        Response apply() throws ClientException, RepositoryException;
    }

    @POST
    @Path("/{itemUUID}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createContainerItem(final @PathParam("itemUUID") String itemUUID,
                                        final @QueryParam("lastModifiedTimestamp") long versionStamp) throws ContainerException {
        if (StringUtils.isEmpty(itemUUID)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("There must be a uuid of the containeritem to copy from")
                    .build();
        }
        try {
            UUID.fromString(itemUUID);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("There must be a valid uuid of the containeritem to copy from")
                    .build();
        }

        final ContainerAction<Response> createContainerItem = () -> {
            final ContainerItem newContainerItem = containerComponentService.createContainerItem(getSession(), itemUUID, versionStamp);

            final Node newNode = newContainerItem.getContainerItem();
            final ContainerItemRepresentation containerItemRepresentation = new ContainerItemRepresentation().represent(newNode, newContainerItem.getTimeStamp());

            log.info("Successfully created containerItemRepresentation '{}' with path '{}'" , newNode.getName(), newNode.getPath());
            return Response.status(Response.Status.CREATED)
                    .entity(containerItemRepresentation)
                    .build();
        };

        return handleAction(createContainerItem);
    }


    @PUT
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateContainer(final ContainerRepresentation container) {
        final ContainerAction<Response> updateContainer = () -> {
            containerComponentService.updateContainer(getSession(), container);
            return Response.status(Response.Status.OK).entity(container).build();
        };

        return handleAction(updateContainer);
    }

    @DELETE
    @Path("/{itemUUID}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteContainerItem(final @PathParam("itemUUID") String itemUUID,
                                        final @QueryParam("lastModifiedTimestamp") long versionStamp) {
        final ContainerAction<Response> deleteContainerItem = () -> {
            containerComponentService.deleteContainerItem(getSession(), itemUUID, versionStamp);
            return Response.status(Response.Status.OK).build();
        };

        return handleAction(deleteContainerItem);
    }

    private Session getSession() throws RepositoryException {
        return getPageComposerContextService().getRequestContext().getSession();
    }

    private Response handleAction(final ContainerAction<Response> action) {
        final Response.Status httpStatusCode;
        final ErrorStatus errorStatus;

        try {
            return action.apply();
        } catch (ClientException e) {
            errorStatus = e.getErrorStatus();
            httpStatusCode = Response.Status.BAD_REQUEST;
        } catch (RepositoryException | IllegalArgumentException e) {
            errorStatus = ErrorStatus.unknown(e.getMessage());
            httpStatusCode = Response.Status.INTERNAL_SERVER_ERROR;
        }
        return createErrorResponse(httpStatusCode, errorStatus);
    }

    private Response createErrorResponse(final Response.Status httpStatusCode, final ErrorStatus errorStatus) {
        return Response.status(httpStatusCode).entity(errorStatus).build();
    }
}
