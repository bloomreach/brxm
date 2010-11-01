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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.jackrabbit.uuid.UUID;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.PostRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.lang.reflect.Type;


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
            Node containerItem = session.getNodeByUUID(itemUUID);
            if (!containerItem.isNodeType(HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT)) {
                throw new ContainerException("Need a container item");
            }

            Node containerNode = getRequestConfigNode(requestContext);

            // now we have the containerItem that contains 'how' to create the new containerItem and we have the
            // containerNode. Find a correct newName and create a new node.
            String newItemNodeName = findNewName(containerItem.getName(), containerNode);
            Node newItem = containerNode.addNode(newItemNodeName, HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT);

            // now we only copy the template, xtype and component class name properties
            if(containerItem.hasProperty(HstNodeTypes.COMPONENT_PROPERTY_TEMPLATE_)) {
                newItem.setProperty(HstNodeTypes.COMPONENT_PROPERTY_TEMPLATE_,
                    containerItem.getProperty(HstNodeTypes.COMPONENT_PROPERTY_TEMPLATE_).getString());
            }
            newItem.setProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME,
                    containerItem.getProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME).getString());
            newItem.setProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE,
                    containerItem.getProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE).getString());
            if(containerItem.hasProperty(HstNodeTypes.COMPONENT_PROPERTY_SAMPLE_CONTENT)) {
                newItem.setProperty(HstNodeTypes.COMPONENT_PROPERTY_SAMPLE_CONTENT,
                        containerItem.getProperty(HstNodeTypes.COMPONENT_PROPERTY_SAMPLE_CONTENT).getString());
            }
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
            String[] children = container.getChildren();
            for (String childId : children) {
                checkIfMoveIntended(containerNode, childId, session);
            }

            int index = children.length - 1;
            while (index > -1) {
                String childId = children[index];
                Node childNode = session.getNodeByUUID(childId);
                String nodeName = childNode.getName();

                int next = index + 1;
                if (next == children.length) {
                    containerNode.orderBefore(nodeName, null);
                } else {
                    Node nextChildNode = session.getNodeByUUID(children[next]);
                    containerNode.orderBefore(nodeName, nextChildNode.getName());
                }
                --index;
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
            Node node = session.getNodeByUUID(itemUUID);
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


    private void checkIfMoveIntended(Node parent, String childId, Session session) throws RepositoryException {
        String parentPath = parent.getPath();
        Node childNode = session.getNodeByUUID(childId);
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
