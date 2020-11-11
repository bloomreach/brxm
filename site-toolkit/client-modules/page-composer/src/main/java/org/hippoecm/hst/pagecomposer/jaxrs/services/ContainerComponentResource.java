/*
 *  Copyright 2010-2020 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.pagecomposer.jaxrs.api.annotation.PrivilegesAllowed;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItem;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ErrorStatus;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.platform.utils.UUIDUtils.isValidUUID;
import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.CHANNEL_WEBMASTER_PRIVILEGE_NAME;

@Path("/hst:containercomponent/")
public class ContainerComponentResource extends AbstractConfigResource implements ContainerComponentResourceInterface {
    private static Logger log = LoggerFactory.getLogger(ContainerComponentResource.class);

    private ContainerComponentService containerComponentService;

    public void setContainerComponentService(ContainerComponentService containerComponentService) {
        this.containerComponentService = containerComponentService;
    }

    @FunctionalInterface
    interface ContainerAction<R> {
        R apply() throws RepositoryException;
    }

    @POST
    @Path("/{itemUUID}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PrivilegesAllowed(CHANNEL_WEBMASTER_PRIVILEGE_NAME)
    @Override
    public Response createContainerItem(final @PathParam("itemUUID") String itemUUID,
                                        final @QueryParam("lastModifiedTimestamp") long versionStamp) {
        if (!isValidUUID(itemUUID)) {
            final String message = String.format("Value of path parameter itemUUID: '%s' is not a valid UUID", itemUUID);
            return clientError(message, new ErrorStatus(ClientError.INVALID_UUID));
        }
        final ContainerAction<Response> createContainerItem = () -> {
            final ContainerItem newContainerItem = containerComponentService.createContainerItem(getSession(), itemUUID, versionStamp);
            return respondContainerItem(newContainerItem, false, Response.Status.CREATED, "Successfully created item");
        };
        return handleAction(createContainerItem);
    }

    @POST
    @Path("/{itemUUID}/{siblingItemUUID}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PrivilegesAllowed(CHANNEL_WEBMASTER_PRIVILEGE_NAME)
    @Override
    public Response createContainerItemAndAddBefore(final @PathParam("itemUUID") String itemUUID,
                                                    final @PathParam("siblingItemUUID") String siblingItemUUID,
                                                    final @QueryParam("lastModifiedTimestamp") long versionStamp) {
        if (!isValidUUID(itemUUID)) {
            final String message = String.format("Value of path parameter itemUUID: '%s' is not a valid UUID", itemUUID);
            return clientError(message, new ErrorStatus(ClientError.INVALID_UUID));
        }
        if (siblingItemUUID != null && !isValidUUID(siblingItemUUID)) {
            final String message = String.format("Value of path parameter siblingItemUUID: '%s' is not a valid UUID", siblingItemUUID);
            return clientError(message, new ErrorStatus(ClientError.INVALID_UUID));
        }
        return handleAction(() -> {
            final ContainerItem newContainerItem = containerComponentService.createContainerItem(getSession(), itemUUID, siblingItemUUID, versionStamp);
            return respondContainerItem(newContainerItem, false, Response.Status.CREATED, "Successfully created item");
        });
    }


    @PUT
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PrivilegesAllowed(CHANNEL_WEBMASTER_PRIVILEGE_NAME)
    @Override
    public Response updateContainer(final ContainerRepresentation container) {
        final ContainerAction<Response> updateContainer = () -> {
            containerComponentService.updateContainer(getSession(), container);
            return ok(container.getId() + " updated", container);
        };

        return handleAction(updateContainer);
    }

    @DELETE
    @Path("/{itemUUID}")
    @Produces(MediaType.APPLICATION_JSON)
    @PrivilegesAllowed(CHANNEL_WEBMASTER_PRIVILEGE_NAME)
    public Response deleteContainerItem(final @PathParam("itemUUID") String itemUUID,
                                        final @QueryParam("lastModifiedTimestamp") long versionStamp) {
        final ContainerAction<Response> deleteContainerItem = () -> {
            containerComponentService.deleteContainerItem(getSession(), itemUUID, versionStamp);
            return ok(itemUUID + " deleted");
        };

        return handleAction(deleteContainerItem);
    }

    private Session getSession() throws RepositoryException {
        return getPageComposerContextService().getRequestContext().getSession();
    }

    private Response handleAction(final ContainerAction<Response> action) {
        try {
            return action.apply();
        } catch (ClientException e) {
            return clientError(e.getMessage(), e.getErrorStatus());
        } catch (Exception e) {
            return error(e.getMessage(), ErrorStatus.unknown(e.getMessage()));
        }
    }

}
