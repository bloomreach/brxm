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
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.jackrabbit.uuid.UUID;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.rest.ExtResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Path("/hst:containercomponent/")
public class ContainerComponentResource extends AbstractConfigResource {
private static Logger log = LoggerFactory.getLogger(ContainerComponentResource.class);
    
    @GET
    @Path("/create/{itemUUID}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createContainerItem(@Context HttpServletRequest servletRequest, 
            @Context HttpServletResponse servletResponse, 
            @PathParam("itemUUID") String itemUUID) throws ContainerException {
       
        if(itemUUID == null) {
            throw new ContainerException("There must be a uuid of the containeritem to copy from "); 
        }
        try {
            UUID.fromString(itemUUID); 
        } catch(IllegalArgumentException e) {
            throw new ContainerException("There must be a valid uuid of the containeritem to copy from");
        }
        
        HstRequestContext requestContext = getRequestContext(servletRequest);  
        try {
            Session session = requestContext.getSession();
            Node containerItem = session.getNodeByUUID(itemUUID);
            if(!containerItem.isNodeType(HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT)) {
                throw new ContainerException("Need a container item");
            }
            
            Node containerNode = getRequestConfigNode(requestContext);
            
            // now we have the containerItem that contains 'how' to create the new containerItem and we have the containerNode
            
            String newItemNodeName = containerItem.getName();
            String origName = newItemNodeName;
            int counter = 0;
            while(containerNode.hasNode(newItemNodeName)) {
                newItemNodeName = origName +  ++counter;
            }
            
            Node newItem = containerNode.addNode(newItemNodeName, HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT);
            // now we only copy the template and component class name
            newItem.setProperty(HstNodeTypes.COMPONENT_PROPERTY_TEMPLATE_, containerItem.getProperty(HstNodeTypes.COMPONENT_PROPERTY_TEMPLATE_).getString());
            newItem.setProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME, containerItem.getProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME).getString());
            newItem.setProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE, containerItem.getProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE).getString());
            
            // now copy all the other child nodes from containerNode to newItem
            
            Workspace workspace = session.getWorkspace();
            NodeIterator childs = containerItem.getNodes();
            while(childs.hasNext()) {
                Node child = childs.nextNode();
                if(child == null) {
                    throw new ContainerException("Error during iterating child nodes of container item");
                }
                workspace.copy(child.getPath(), newItem.getPath());
            }
           
            // now save the container node
            
            session.save();

            ExtResult result = new ExtResult(new String[0]);
            result.setMessage("Successfully create node " + newItem.getName() + " at path " + newItem.getPath());
            result.setSuccess(true);
            return Response.ok().entity(result).build();
            
        } catch (LoginException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        ExtResult result = new ExtResult(new String[0]);
        result.setMessage("Exception during creating new container item");
        result.setSuccess(false);
        return Response.serverError().entity(result).build();
    }

    @GET
    @Path("/delete/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteContainerItem(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse) {
       
        return null;
    }

}
