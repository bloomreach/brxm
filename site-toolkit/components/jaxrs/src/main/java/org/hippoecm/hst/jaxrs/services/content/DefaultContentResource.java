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
package org.hippoecm.hst.jaxrs.services.content;

import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.jaxrs.model.content.HippoDocumentRepresentation;
import org.hippoecm.hst.jaxrs.model.content.HippoFolderRepresentation;
import org.hippoecm.hst.jaxrs.model.content.NodeProperty;
import org.hippoecm.hst.jaxrs.model.content.NodeRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
@Path("/{resourceType}/")
public class DefaultContentResource extends AbstractContentResource {
    
    private static Logger log = LoggerFactory.getLogger(DefaultContentResource.class);
    
    @GET
    @Path("/")
    public NodeRepresentation getContentResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @MatrixParam("pf") Set<String> propertyFilters) {
    	HstRequestContext requestContext = getRequestContext(servletRequest);    	
    	Node requestContentNode = getRequestContentNode(requestContext);
        NodeRepresentation nodeRepresentation = null;
        
        try {
            HippoBean hippoBean = (HippoBean) getObjectConverter().getObject(requestContentNode);
            
            if (hippoBean instanceof HippoDocumentBean) {
                nodeRepresentation = new HippoDocumentRepresentation().represent(hippoBean, propertyFilters);
            } else if (hippoBean instanceof HippoFolderBean) {
                nodeRepresentation = new HippoFolderRepresentation().represent(hippoBean, propertyFilters);
            } else {
                nodeRepresentation = new NodeRepresentation().represent(hippoBean, propertyFilters);
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } 
            else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
        
        return nodeRepresentation;
    }
    
    @POST
    @Path("/property/{propertyName}/")
    public NodeProperty setContentResourceProperty(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("propertyName") String propertyName, @FormParam("pv") List<String> propertyValues) {
        HstRequestContext requestContext = getRequestContext(servletRequest);
        Node requestContentNode = getRequestContentNode(requestContext);
        NodeProperty nodeProperty = null; 
        
        try {
            nodeProperty = setResourceNodeProperty(requestContentNode, propertyName, propertyValues);
            requestContentNode.save();
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to save node.", e);
            } else {
                log.warn("Failed to save node. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
        
        return nodeProperty;
    }
    
    @DELETE
    @Path("/")
    public void deleteContentResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        try {
            HippoBean hippoBean = getRequestContentBean(getRequestContext(servletRequest));
            deleteContentBean(servletRequest, hippoBean);
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to delete content resource.", e);
            } else {
                log.warn("Failed to delete content resource. {}", e.toString());
            }
            throw new WebApplicationException(e);
        } catch (ObjectBeanManagerException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to convert content node to content bean.", e);
            } else {
                log.warn("Failed to convert content node to content bean. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
    }
}
