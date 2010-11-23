/*
 *  Copyright 2010 Hippo.
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

import java.lang.reflect.Type;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.UUID;

import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.NotFoundException;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.PostRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

//TODO When HSTTWO-1368 is fixed, we can remove the UndeclaredThrowableException

@Path("/hst:containercomponent/")
public class ContainerComponentResource extends AbstractConfigResource {
    private static Logger log = LoggerFactory.getLogger(ContainerComponentResource.class);

    @POST
    @Path("/create/{itemUUID}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createContainerItem(@Context HttpServletRequest servletRequest,
                                        @Context HttpServletResponse servletResponse,
                                        @PathParam("itemUUID") String itemUUID) throws ContainerException {

        if (itemUUID == null) {
            throw new ContainerException("There must be a uuid of the containeritem to copy from ");
        }
        try {
            UUID.fromString(itemUUID);
        } catch (IllegalArgumentException e) {
            throw new ContainerException("There must be a valid uuid of the containeritem to copy from");
        }

        HstRequestContext requestContext = getRequestContext(servletRequest);
        try {
            Session session = requestContext.getSession();
            Node containerItem;
            try {
                containerItem = session.getNodeByIdentifier(itemUUID);
            } catch (ItemNotFoundException e) {
                return error("ItemNotFoundException: unknown uuid '"+itemUUID+"'. Cannot create item");
            } catch (UndeclaredThrowableException e) {
                return error("ItemNotFoundException: unknown uuid '"+itemUUID+"'. Cannot create item");
            }
            
            if (!containerItem.isNodeType(HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT)) {
                throw new ContainerException("Need a container item");
            }

            Node containerNode = getRequestConfigNode(requestContext);

            // now we have the containerItem that contains 'how' to create the new containerItem and we have the
            // containerNode. Find a correct newName and create a new node.
            String newItemNodeName = findNewName(containerItem.getName(), containerNode);

            session.getWorkspace().copy(containerItem.getPath(), containerNode.getPath() + "/" + newItemNodeName);
            Node newItem = containerNode.getNode(newItemNodeName);
            // now copy all the other child nodes from containerNode to newItem
            Workspace workspace = session.getWorkspace();
            NodeIterator childs = containerItem.getNodes();
            while (childs.hasNext()) {
                Node child = childs.nextNode();
                if (child == null) {
                    throw new ContainerException("Error during iterating child nodes of container item");
                }
                workspace.copy(child.getPath(), newItem.getPath());
            }

            // now save the container node
            session.save();

            ContainerItemRepresentation item = new ContainerItemRepresentation().represent(newItem);
            return ok("Successfully create item " + newItem.getName() + " with path " + newItem.getPath(), item);

        } catch (LoginException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return error("Exception during creating new container item");
    }

    @POST
    @Path("/update/{itemUUID}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateContainer(@Context HttpServletRequest servletRequest,
                                    @Context HttpServletResponse servletResponse,
                                    @PathParam("itemUUID") String itemUUID, String json) {

        Type type = new TypeToken<PostRepresentation<ContainerRepresentation>>() {}.getType();
        PostRepresentation<ContainerRepresentation> pr = new Gson().fromJson(json, type);
        ContainerRepresentation container = pr.getData();

        HstRequestContext requestContext = getRequestContext(servletRequest);
        try {
            Session session = requestContext.getSession();
            Node containerNode = getRequestConfigNode(requestContext);
            List<String> children = container.getChildren();
            int childCount = (children != null ? children.size() : 0);
            if (childCount > 0) {
                try {
                    for (String childId : children) {
                        checkIfMoveIntended(containerNode, childId, session);
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
                    return error("ItemNotFoundException: Cannot update item '"+itemUUID+"'");
                } catch (UndeclaredThrowableException e) {
                    return error("ItemNotFoundException: Cannot update item '"+itemUUID+"'");
                }
            }
            session.save();
            return ok("Item order for container[" + container.getId() + "] has been updated.", container);

        } catch (RepositoryException e) {
            return error(e.getMessage(), container);
        }
    }


    @GET
    @Path("/delete/{itemUUID}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteContainerItem(@Context HttpServletRequest servletRequest,
                                        @Context HttpServletResponse servletResponse,
                                        @PathParam("itemUUID") String itemUUID) {
        HstRequestContext requestContext = getRequestContext(servletRequest);
        try {
            Session session = requestContext.getSession();
            Node node;
            try {
                node = session.getNodeByIdentifier(itemUUID);
            } catch (ItemNotFoundException e) {
                return error("ItemNotFoundException: unknown uuid '"+itemUUID+"'. Cannot delete item");
            } catch (UndeclaredThrowableException e) {
                return error("ItemNotFoundException: unknown uuid '"+itemUUID+"'. Cannot delete item");
            }
            node.remove();
            session.save();
        } catch (RepositoryException e) {
            log.warn("Failed to delete node with id {} but returning OK anyway.", itemUUID);
        }
        return ok("Successfully removed node with UUID: " + itemUUID);
    }

    private String findNewName(String base, Node parent) throws RepositoryException {
        String newName = base;
        int counter = 0;
        while (parent.hasNode(newName)) {
            newName = base + ++counter;
        }
        return newName;
    }


    private void checkIfMoveIntended(Node parent, String childId, Session session) throws RepositoryException, NotFoundException, UndeclaredThrowableException {
        String parentPath = parent.getPath();
        Node childNode = session.getNodeByIdentifier(childId);
        String childPath = childNode.getPath();
        String childParentPath = childPath.substring(0, childPath.lastIndexOf('/'));
        if (!parentPath.equals(childParentPath)) {
            String name = childPath.substring(childPath.lastIndexOf('/') + 1);
            name = findNewName(name, parent);
            String newChildPath = parentPath + "/" + name;
            session.move(childPath, newChildPath);
        }
    }

}
