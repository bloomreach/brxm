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
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.services.support.jaxrs.content.BaseHstContentService;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@Path("/PageModelService/")
public class PageModelService extends BaseHstContentService {
    final static String SVN_ID = "$Id$";

    private static final String NEW_NODE_NAME = "newNode";

    @GET
    @Path("/read")
    @Produces(MediaType.APPLICATION_JSON)
    public Response load(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse) {
        HttpSession http = servletRequest.getSession();

        ExtResult<List<BaseModel>> result = new ExtResult<List<BaseModel>>();

        String containersRaw = servletRequest.getParameter("containers");
        if (containersRaw != null) {
            try {
                Node root = getJcrSession(servletRequest).getRootNode();
                Session session = getJcrSession(servletRequest);
                List<BaseModel> list = new LinkedList<BaseModel>();
                for (String uuid : containersRaw.trim().split(",")) {
                    Node containerNode = session.getNodeByUUID(uuid);
                    ContainerModel container = new ContainerModel(containerNode, http);
                    list.add(container);

                    IdUtil.savePath(container.getId(), container.getPath(), servletRequest.getSession());

                    NodeIterator it = containerNode.getNodes();
                    String[] childNames = new String[(int) it.getSize()];
                    while (it.hasNext()) {
                        Node child = it.nextNode();
                        ContainerItemModel item = new ContainerItemModel(child, http);
                        list.add(item);
                        //String id = IdUtil.getId(item.getPath(), servletRequest.getSession());
                        //item.setId(id);
                        childNames[((int) it.getPosition()) - 1] = item.getId();
                    }
                    container.setChildren(childNames);
                }
                result.setSuccess(true);
                result.setMessage("Data loaded successfully");
                result.setData(list);

                return Response.ok().entity(result).build();

            } catch (RepositoryException e) {
                e.printStackTrace();
                result.setSuccess(false);
                result.setMessage(e.getMessage());
            }
        }

        return Response.serverError().entity(result).build();
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
        ExtResult<String[]> result = new ExtResult<String[]>();
        result.setData(new String[0]);

        try {
            HttpSession http = servletRequest.getSession();
            if (!IdUtil.containsPath(id, http)) {
                throw new WebApplicationException(new IllegalArgumentException("No path found for item id " + id), Response.Status.NOT_FOUND);
            }
            String path = IdUtil.getPath(id, http);

            Session session = getJcrSession(servletRequest);
            if (!session.itemExists(path)) {
                throw new WebApplicationException(new IllegalArgumentException("No item found for path " + path), Response.Status.NOT_FOUND);
            }

            session.getItem(path).remove();
            session.save();

            IdUtil.remove(id, http);
            result.setSuccess(true);
            result.setMessage("Successfully removed node at path " + path);
        } catch (Exception e) {
            result.setMessage(e.toString());
            result.setSuccess(true);
            //e.printStackTrace();
        }
        return Response.ok().entity(result).build();
        //return Response.serverError().entity(result).build();
    }

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, String json) {
        HttpSession http = servletRequest.getSession();

        Type listType = new TypeToken<WrapperModel<ContainerItemModel>>() {}.getType();
        WrapperModel<ContainerItemModel> wrapper = new Gson().fromJson(json, listType);
        ContainerItemModel model = wrapper.getData();

        ExtResult<ContainerItemModel> result = new ExtResult<ContainerItemModel>();
        result.setData(model);
        String parentId = model.getParentId();
        if (IdUtil.containsPath(parentId, http)) {
            String path = IdUtil.getPath(parentId, http);

            try {
                Session session = getJcrSession(servletRequest);
                Node parent = session.getRootNode().getNode(path.substring(1));
                String name = findNewName(model.getName(), parent);

                Node newNode = parent.addNode(name, "hst:component");
                newNode.setProperty("hst:componentclassname", model.getComponentClassName());
                newNode.setProperty("hst:template", model.getTemplate());

                session.save();
                session.refresh(true);

                String newPath = newNode.getPath();
                String id = IdUtil.getId(newPath, http);

                model.setId(id);
                model.setName(newNode.getName());
                model.setPath(newPath);

                result.setSuccess(true);
                result.setMessage("Successfully create node " + newNode + " at path " + path);

                return Response.ok().entity(result).build();

            } catch (RepositoryException e) {
                result.setMessage(e.toString());
            }
        } else {
            result.setMessage("No path found for id " + parentId);
        }
        return Response.serverError().entity(result).build();
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
        HttpSession http = servletRequest.getSession();

        Type listType = new TypeToken<WrapperModel<ContainerModel>>() {
        }.getType();
        WrapperModel<ContainerModel> wrapperModel = new Gson().fromJson(json, listType);
        ContainerModel model = wrapperModel.getData();

        ExtResult<ContainerModel> result = new ExtResult<ContainerModel>();
        result.setData(model);

        if (IdUtil.containsPath(id, http)) {

            try {
                Session session = getJcrSession(servletRequest);

                String path = IdUtil.getPath(id, http);
                Node containerNode = session.getRootNode().getNode(path.substring(1));

                String[] children = model.getChildren();
                //first check if a move is needed
                for (String childId : children) {
                    checkIfMoveNeeded(containerNode, childId, session, http);
                }

                int index = children.length - 1;
                while (index > -1) {
                    String childId = children[index];
                    String nodeName = getNodeName(childId, http);

                    int next = index + 1;
                    if (next == children.length) {
                        containerNode.orderBefore(nodeName, null);
                    } else {
                        containerNode.orderBefore(nodeName, getNodeName(children[next], http));
                    }
                    --index;
                }
                session.save();
                result.setSuccess(true);
                result.setMessage("Container item order updated.");

                return Response.ok().entity(result).build();

            } catch (RepositoryException e) {
                result.setMessage(e.toString());
            }
        } else {
            result.setMessage("No path found for id " + id);
        }
        return Response.serverError().entity(result).build();
    }

    private String getNodeName(String childId, HttpSession session) {
        String childPath = IdUtil.getPath(childId, session);
        return childPath.substring(childPath.lastIndexOf('/') + 1);
    }

    private void checkIfMoveNeeded(Node parent, String childId, Session session, HttpSession http) throws RepositoryException {
        String parentPath = parent.getPath();
        String childPath = IdUtil.getPath(childId, http);
        String childParentPath = childPath.substring(0, childPath.lastIndexOf('/'));
        if (!parentPath.equals(childParentPath)) {
            String name = childPath.substring(childPath.lastIndexOf('/') + 1);
            name = findNewName(name, parent);
            String newChildPath = parentPath + "/" + name;
            session.move(childPath, newChildPath);
            IdUtil.savePath(childId, newChildPath, http);
        }
    }
}