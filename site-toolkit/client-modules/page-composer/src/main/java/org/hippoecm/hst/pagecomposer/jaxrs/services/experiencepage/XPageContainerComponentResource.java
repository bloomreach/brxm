/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage;

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

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.pagecomposer.jaxrs.api.annotation.PrivilegesAllowed;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ErrorStatus;
import org.hippoecm.hst.pagecomposer.jaxrs.services.ContainerComponentResourceInterface;
import org.hippoecm.hst.pagecomposer.jaxrs.services.ContainerComponentService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.ContainerComponentService.ContainerItem;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.pagecomposer.jaxrs.util.UUIDUtils.isValidUUID;
import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.XPAGE_REQUIRED_PRIVILEGE_NAME;

@Path("/experiencepage/hst:containercomponent/")
public class XPageContainerComponentResource implements ContainerComponentResourceInterface {
    private static Logger log = LoggerFactory.getLogger(XPageContainerComponentResource.class);

    private ContainerComponentService xPageContainerComponentService;

    public void setXPageContainerComponentService(ContainerComponentService xPageContainerComponentService) {
        this.xPageContainerComponentService = xPageContainerComponentService;
    }

    @FunctionalInterface
    interface ContainerAction<R> {
        R apply() throws RepositoryException;
    }

    @POST
    @Path("/{itemUUID}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PrivilegesAllowed(XPAGE_REQUIRED_PRIVILEGE_NAME)
    public Response createContainerItem(final @PathParam("itemUUID") String itemUUID,
                                        final @QueryParam("lastModifiedTimestamp") long versionStamp) {
        if (!isValidUUID(itemUUID)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(String.format("Value of path parameter itemUUID: '%s' is not a valid UUID", itemUUID))
                    .build();
        }
        final ContainerAction<Response> createContainerItem = () -> {
            final ContainerItem newContainerItem = xPageContainerComponentService.createContainerItem(getSession(), itemUUID, versionStamp);
            return respondNewContainerItemCreated(newContainerItem);
        };
        return handleAction(createContainerItem);
    }

    @POST
    @Path("/{itemUUID}/{siblingItemUUID}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PrivilegesAllowed(XPAGE_REQUIRED_PRIVILEGE_NAME)
    public Response createContainerItemAndAddBefore(final @PathParam("itemUUID") String itemUUID,
                                                    final @PathParam("siblingItemUUID") String siblingItemUUID,
                                                    final @QueryParam("lastModifiedTimestamp") long versionStamp) {
        if (!isValidUUID(itemUUID)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(String.format("Value of path parameter itemUUID: '%s' is not a valid UUID", itemUUID))
                    .build();
        }
        if (!isValidUUID(siblingItemUUID)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(String.format("Value of path parameter siblingItemUUID: '%s' is not a valid UUID", siblingItemUUID))
                    .build();
        }
        return handleAction(() -> {
            final ContainerItem newContainerItem = xPageContainerComponentService.createContainerItem(getSession(), itemUUID, siblingItemUUID, versionStamp);
            return respondNewContainerItemCreated(newContainerItem);
        });
    }


    @PUT
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PrivilegesAllowed(XPAGE_REQUIRED_PRIVILEGE_NAME)
    public Response updateContainer(final ContainerRepresentation container) {
        final ContainerAction<Response> updateContainer = () -> {
            xPageContainerComponentService.updateContainer(getSession(), container);
            return Response.status(Response.Status.OK).entity(container).build();
        };

        return handleAction(updateContainer);
    }

    @DELETE
    @Path("/{itemUUID}")
    @Produces(MediaType.APPLICATION_JSON)
    @PrivilegesAllowed(XPAGE_REQUIRED_PRIVILEGE_NAME)
    public Response deleteContainerItem(final @PathParam("itemUUID") String itemUUID,
                                        final @QueryParam("lastModifiedTimestamp") long versionStamp) {
        final ContainerAction<Response> deleteContainerItem = () -> {
            xPageContainerComponentService.deleteContainerItem(getSession(), itemUUID, versionStamp);
            return Response.status(Response.Status.OK).build();
        };

        return handleAction(deleteContainerItem);
    }

    private Session getSession() throws RepositoryException {
        return RequestContextProvider.get().getSession();
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

    private Response respondNewContainerItemCreated(ContainerItem newContainerItem) throws RepositoryException {
        final Node newNode = newContainerItem.getContainerItem();
        final ContainerItemRepresentation containerItemRepresentation = new ContainerItemRepresentation().represent(newNode, newContainerItem.getTimeStamp());

        log.info("Successfully created containerItemRepresentation '{}' with path '{}'", newNode.getName(), newNode.getPath());
        return Response.status(Response.Status.CREATED)
                .entity(containerItemRepresentation)
                .build();
    }
}
