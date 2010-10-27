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
package org.hippoecm.hst.pagecomposer.rest;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
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

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.services.support.jaxrs.content.BaseHstContentService;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */

@Path("/PageModelService/")
public class PageModelService extends BaseHstContentService {

    private static Logger log = LoggerFactory.getLogger(PageModelService.class);

    private static final String NEW_NODE_NAME = "newNode";

    @GET
    @Path("/read")
    @Produces(MediaType.APPLICATION_JSON)
    public Response load(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse) {

        List<BaseModel> list = new LinkedList<BaseModel>();

        String containersRaw = servletRequest.getParameter("containers");
        if(StringUtils.isBlank(containersRaw)) {
            return ok("No containers found");
        }

        try {
            Session session = getJcrSession(servletRequest);
            for (String uuid : containersRaw.trim().split(",")) {
                Node containerNode = session.getNodeByUUID(uuid);
                ContainerModel container = new ContainerModel(containerNode);
                list.add(container);

                NodeIterator it = containerNode.getNodes();
                String[] childNames = new String[(int) it.getSize()];
                while (it.hasNext()) {
                    Node child = it.nextNode();
                    ContainerItemModel item = new ContainerItemModel(child);
                    list.add(item);
                    childNames[((int) it.getPosition()) - 1] = item.getId();
                }
                container.setChildren(childNames);
            }
            return ok("Data loaded successfully", list);
        } catch (RepositoryException e) {
            log.error("Error reading container(item) data", e);
            return error(e.getMessage(), list);
        }
    }

    /**
     * Destroy a node referenced by ID parameter.
     * Because of issues with batch-operations in the EXT-store I've chosen to always return a success response
     * for now.
     *
     * @param servletRequest
     * @param servletResponse
     * @param id
     * @return
     */
    @GET
    @Path("/destroy/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response destroy(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @PathParam("id") String id) {
        try {
            Session session = getJcrSession(servletRequest);
            Node node = session.getNodeByUUID(id);
            node.remove();
            session.save();
        } catch (Exception e) {
            log.warn("Failed to delete node with id {} but returning OK anyway.", id);
        }
        return ok("Successfully removed node with UUID: " + id);
    }

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, String json) {

        Type listType = new TypeToken<WrapperModel<ContainerItemModel>>() {}.getType();
        WrapperModel<ContainerItemModel> wrapper = new Gson().fromJson(json, listType);
        ContainerItemModel model = wrapper.getData();

        try {
            Session session = getJcrSession(servletRequest);
            Node parent = session.getNodeByUUID(model.getParentId());
            String name = findNewName(model.getName(), parent);

            Node newNode = parent.addNode(name, HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT);
            newNode.setProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME, model.getComponentClassName());
            newNode.setProperty(HstNodeTypes.COMPONENT_PROPERTY_TEMPLATE_, model.getTemplate());
            newNode.setProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE, model.getXtype());

            session.save();
            session.refresh(true);

            String newPath = newNode.getPath();

            model.setId(newNode.getUUID());
            model.setName(newNode.getName());
            model.setPath(newPath);

            return ok("Successfully create node " + newNode + " at path " + newPath, model);
        } catch (RepositoryException e) {
            return error(e.getMessage(), model);
        }
    }

    private String findNewName(String base, Node parent) throws RepositoryException {
        if (base == null) {
            base = NEW_NODE_NAME;
        }
        String name = base;
        int count = 1;
        while (parent.hasNode(name)) {
            name = base + count++;
        }
        return name;
    }

    @POST
    @Path("/update/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @PathParam("id") String id, String json) {

        Type listType = new TypeToken<WrapperModel<ContainerModel>>() {}.getType();
        WrapperModel<ContainerModel> wrapperModel = new Gson().fromJson(json, listType);
        ContainerModel model = wrapperModel.getData();

        try {
            Session session = getJcrSession(servletRequest);
            Node containerNode = session.getNodeByUUID(id);

            String[] children = model.getChildren();
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
            return ok("Item order for container[" + model.getId() + "] has been updated.", model);

        } catch (RepositoryException e) {
            return error(e.getMessage(), model);
        }
    }

    private Response ok(String msg) {
        return ok(msg, new String[0]);
    }

    private Response ok(String msg, Object data) {
        ExtResult result = new ExtResult(data);
        result.setMessage(msg);
        result.setSuccess(true);
        return Response.ok().entity(result).build();
    }

    private Response error(String msg, Object data) {
        ExtResult result = new ExtResult(data);
        result.setMessage(msg);
        result.setSuccess(false);
        return Response.serverError().entity(result).build();
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