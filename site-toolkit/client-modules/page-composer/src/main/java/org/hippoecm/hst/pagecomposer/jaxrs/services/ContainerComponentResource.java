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

import java.util.List;
import java.util.UUID;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.NotFoundException;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.PostRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.ContainerComponentService.ContainerItem;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ErrorStatus;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT;

@Path("/hst:containercomponent/")
public class ContainerComponentResource extends AbstractConfigResource {
    private static Logger log = LoggerFactory.getLogger(ContainerComponentResource.class);

    private ContainerComponentService containerComponentService;

    public void setContainerComponentService(ContainerComponentService containerComponentService) {
        this.containerComponentService = containerComponentService;
    }

    private ContainerHelper containerHelper;

    public void setContainerHelper(final ContainerHelper containerHelper) {
        this.containerHelper = containerHelper;
    }

    @POST
    @Path("/create/{itemUUID}")
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

        final Response.Status httpStatusCode;
        final ErrorStatus errorStatus;
        try {
            final Session session = getPageComposerContextService().getRequestContext().getSession();
            final ContainerItem newContainerItem = containerComponentService.createContainerItem(session, itemUUID, versionStamp);

            final Node newNode = newContainerItem.getContainerItem();
            final ContainerItemRepresentation containerItemRepresentation = new ContainerItemRepresentation().represent(newNode, newContainerItem.getTimeStamp());

            log.info("Successfully created containerItemRepresentation '{}' with path '{}'" , newNode.getName(), newNode.getPath());
            return Response.status(Response.Status.CREATED)
                    .entity(containerItemRepresentation)
                    .build();
        } catch (ClientException e) {
            errorStatus = e.getErrorStatus();
            httpStatusCode = Response.Status.BAD_REQUEST;
        } catch (RepositoryException e) {
            log.warn("Exception during creating new container item: {}", e);
            errorStatus = ErrorStatus.unknown(e.getMessage());
            httpStatusCode = Response.Status.INTERNAL_SERVER_ERROR;
        }
        return createErrorResponse(httpStatusCode, errorStatus);
    }


    @POST
    @Path("/update/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateContainer(final PostRepresentation<ContainerRepresentation> post) {

        ContainerRepresentation container = post.getData();

        HstRequestContext requestContext = getPageComposerContextService().getRequestContext();
        try {
            Session session = requestContext.getSession();
            Node containerNode = getPageComposerContextService().getRequestConfigNode(HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT);
            try {
                // the acquireLock also checks all ancestors whether they are not locked by someone else
                containerHelper.acquireLock(containerNode, container.getLastModifiedTimestamp());
            } catch (ClientException e) {
                log.info("Exception while trying to lock '" + containerNode.getPath() + "': ", e);
                return error(e.getMessage());
            }
            List<String> children = container.getChildren();
            int childCount = (children != null ? children.size() : 0);
            if (childCount > 0) {
                try {
                    for (String childId : children) {
                        moveIfNeeded(containerNode, childId, session);
                    }
                    int index = childCount - 1;

                    while (index > -1) {
                        String childId = children.get(index);
                        Node childNode = session.getNodeByIdentifier(childId);
                        String nodeName = childNode.getName();

                        int next = index + 1;
                        if (next == childCount) {
                            containerNode.orderBefore(nodeName, null);
                        } else {
                            Node nextChildNode = session.getNodeByIdentifier(children.get(next));
                            containerNode.orderBefore(nodeName, nextChildNode.getName());
                        }
                        --index;
                    }
                } catch (ItemNotFoundException e) {
                    log.warn("ItemNotFoundException: Cannot update containerNode '{}'.", containerNode.getPath(), e);
                    return error("ItemNotFoundException: Cannot update containerNode '" + containerNode.getPath() + "'");
                }
            }
            HstConfigurationUtils.persistChanges(session);
            log.info("Item order for container[{}] has been updated.", container.getId());
            return ok("Item order for container[" + container.getId() + "] has been updated.", container);

        } catch (RepositoryException | IllegalArgumentException e) {
            log.warn("Exception during updating container item: {}", e);
            return error("Exception during updating container item: " + e.getMessage(), container);
        }
    }

    @GET
    @Path("/delete/{itemUUID}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteContainerItem(final @PathParam("itemUUID") String itemUUID,
                                        final @QueryParam("lastModifiedTimestamp") long versionStamp) {
        HstRequestContext requestContext = getPageComposerContextService().getRequestContext();
        try {
            Session session = requestContext.getSession();
            Node containerItem;
            try {
                containerItem = session.getNodeByIdentifier(itemUUID);
            } catch (ItemNotFoundException e) {
                log.warn("ItemNotFoundException: unknown uuid '{}'. Cannot delete item", itemUUID);
                return error("ItemNotFoundException: unknown uuid '"+itemUUID+"'. Cannot delete item");
            }
            if (!containerItem.isNodeType(NODETYPE_HST_CONTAINERITEMCOMPONENT)) {
                log.warn("The item to be deleted is not of the correct type. Cannot delete item '{}'", itemUUID);
                return error("The item to be deleted is not of the correct type. Cannot delete item '"+itemUUID+"'");
            }
            Node containerNode = containerItem.getParent();
            if (!containerNode.isNodeType(HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT)) {
                log.warn("The item to be deleted is not a child of a container component. Cannot delete item '{}'", itemUUID);
                return error("The item to be deleted is not a child of a container component. Cannot delete item '"+itemUUID+"'");
            }
            try {
                // the acquireLock also checks all ancestors whether they are not locked by someone else
                containerHelper.acquireLock(containerNode, versionStamp);
            } catch (ClientException e) {
                log.info("Exception while trying to lock '" + containerNode.getPath() + "': ", e);
                return error(e.getMessage());
            }
            containerItem.remove();
            HstConfigurationUtils.persistChanges(session);
        } catch (RepositoryException e) {
            log.warn("Exception during delete container item: {}", e);
            log.warn("Failed to delete node with id {}.", itemUUID);
            return error("Failed  to delete node with id '"+itemUUID+"': " + e.getMessage());
        }
        log.info("Successfully removed node with UUID: {}", itemUUID);
        return ok("Successfully removed node with UUID: " + itemUUID);
    }

    private String findNewName(String base, Node parent) throws RepositoryException {
        String newName = base;
        int counter = 0;
        while (parent.hasNode(newName)) {
            newName = base + ++counter;
        }
        log.debug("New child name '{}' for parent '{}'", newName, parent.getPath());
        return newName;
    }


    /**
     * Move the node identified by {@code childId} to node {@code parent} if it has a different parent.
     */
    private void moveIfNeeded(final Node parent,
                              final String childId,
                              final Session session) throws RepositoryException, NotFoundException {
        String parentPath = parent.getPath();
        Node childNode = session.getNodeByIdentifier(childId);
        if (!childNode.isNodeType(NODETYPE_HST_CONTAINERITEMCOMPONENT)) {
            final String msg = String.format("Expected a move of a node of type '{}' but was '{}'.", NODETYPE_HST_CONTAINERITEMCOMPONENT,
                    childNode.getPrimaryNodeType().getName());
            throw new IllegalArgumentException(msg);
        }
        String childPath = childNode.getPath();
        String childParentPath = childPath.substring(0, childPath.lastIndexOf('/'));
        if (!parentPath.equals(childParentPath)) {
            // lock the container from which the node gets removed
            // note that the 'timestamp' check must not be the timestamp of the 'target' container
            // since this one can be different. We do not need a 'source' timestamp check, since, if the source
            // has changed it is either locked, or if the child item does not exist any more on the server, another
            // error occurs already
            containerHelper.acquireLock(childNode.getParent(), 0);
            String name = childPath.substring(childPath.lastIndexOf('/') + 1);
            name = findNewName(name, parent);
            String newChildPath = parentPath + "/" + name;
            log.debug("Move needed from '{}' to '{}'.", childPath, newChildPath);
            session.move(childPath, newChildPath);
        } else {
            log.debug("No Move needed for '{}' below '{}'", childId, parentPath);
        }
    }

    private Response createErrorResponse(final Response.Status httpStatusCode, final ErrorStatus errorStatus) {
        return Response.status(httpStatusCode).entity(errorStatus).build();
    }
}
