/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.NotFoundException;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.PostRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstConfigurationUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/hst:containercomponent/")
public class ContainerComponentResource extends AbstractConfigResource {
    private static Logger log = LoggerFactory.getLogger(ContainerComponentResource.class);

    private ContainerHelper containerHelper;

    public void setContainerHelper(final ContainerHelper containerHelper) {
        this.containerHelper = containerHelper;
    }

    @POST
    @Path("/create/{itemUUID}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createContainerItem(final @PathParam("itemUUID") String itemUUID,
                                        final @QueryParam("versionStamp") long versionStamp) throws ContainerException {

        if (itemUUID == null) {
            throw new ContainerException("There must be a uuid of the containeritem to copy from ");
        }
        try {
            UUID.fromString(itemUUID);
        } catch (IllegalArgumentException e) {
            throw new ContainerException("There must be a valid uuid of the containeritem to copy from");
        }

        HstRequestContext requestContext = getPageComposerContextService().getRequestContext();
        try {
            Session session = requestContext.getSession();
            Node containerItem;
            try {
                containerItem = session.getNodeByIdentifier(itemUUID);
            } catch (ItemNotFoundException e) {
                log.warn("ItemNotFoundException: unknown uuid '{}'. Cannot create item", itemUUID);
                return error("ItemNotFoundException: unknown uuid '"+itemUUID+"'. Cannot create item");
            }
            if (!containerItem.isNodeType(HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT)) {
                log.warn("The container component where the item should be created in is not of the correct type. Cannot create item '{}'", itemUUID);
                return error("The container component where the item should be created in is not of the correct type. Cannot create item '"+itemUUID+"'");
            }

            Node containerNode = getPageComposerContextService().getRequestConfigNode(HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT);
            if (containerNode == null) {
                log.warn("Exception during creating new container item : Could not find container node to add item to.");
                return error("Exception during creating new container item : Could not find container node to add item to.");
            }

            try {
                // the acquireLock also checks all ancestors whether they are not locked by someone else
                containerHelper.acquireLock(containerNode, versionStamp);
            } catch (ClientException e) {
                log.info("Exception while trying to lock '" + containerNode.getPath() + "': ", e);
                return error(e.getMessage());
            }

            // now we have the containerItem that contains 'how' to create the new containerItem and we have the
            // containerNode. Find a correct newName and create a new node.
            String newItemNodeName = findNewName(containerItem.getName(), containerNode);

            JcrUtils.copy(session, containerItem.getPath(), containerNode.getPath() + "/" + newItemNodeName);
            Node newItem = containerNode.getNode(newItemNodeName);
            HstConfigurationUtils.persistChanges(session);
            final long newVersionStamp;
            if (containerNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED)) {
                newVersionStamp = containerNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED).getDate().getTimeInMillis();
            } else {
                newVersionStamp = 0;
            }
            ContainerItemRepresentation item = new ContainerItemRepresentation().represent(newItem, newVersionStamp);
            log.info("Successfully created item '{}' with path '{}'" , newItem.getName(), newItem.getPath());
            return ok("Successfully created item " + newItem.getName() + " with path " + newItem.getPath(), item);
        } catch (RepositoryException e) {
            log.warn("Exception during creating new container item: {}", e);
            return error("Exception during creating new container item : " + e.getMessage());
        }
    }


    @POST
    @Path("/update/{itemUUID}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateContainer(final @PathParam("itemUUID") String itemUUID,
                                    final @QueryParam("versionStamp") long versionStamp,
                                    final PostRepresentation<ContainerRepresentation> post) {

        ContainerRepresentation container = post.getData();

        HstRequestContext requestContext = getPageComposerContextService().getRequestContext();
        try {
            Session session = requestContext.getSession();
            Node containerNode = getPageComposerContextService().getRequestConfigNode(HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT);
            try {
                // the acquireLock also checks all ancestors whether they are not locked by someone else
                containerHelper.acquireLock(containerNode, versionStamp);
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
                    log.warn("ItemNotFoundException: Cannot update item '{}' for containerNode '{}'.",itemUUID, containerNode.getPath());
                    return error("ItemNotFoundException: Cannot update item '"+itemUUID+"'");
                }
            }
            HstConfigurationUtils.persistChanges(session);
            log.info("Item order for container[{}] has been updated.", container.getId());
            return ok("Item order for container[" + container.getId() + "] has been updated.", container);

        } catch (RepositoryException e) {
            log.warn("Exception during updating container item: {}", e);
            return error("Exception during updating container item: " + e.getMessage(), container);
        }
    }

    @GET
    @Path("/delete/{itemUUID}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteContainerItem(final @PathParam("itemUUID") String itemUUID,
                                        final @QueryParam("versionStamp") long versionStamp) {
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
            if (!containerItem.isNodeType(HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT)) {
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
        String childPath = childNode.getPath();
        String childParentPath = childPath.substring(0, childPath.lastIndexOf('/'));
        if (!parentPath.equals(childParentPath)) {
            String name = childPath.substring(childPath.lastIndexOf('/') + 1);
            name = findNewName(name, parent);
            String newChildPath = parentPath + "/" + name;
            log.debug("Move needed from '{}' to '{}'.", childPath, newChildPath);
            session.move(childPath, newChildPath);
        } else {
            log.debug("No Move needed for '{}' below '{}'", childId, parentPath);
        }
    }

}
