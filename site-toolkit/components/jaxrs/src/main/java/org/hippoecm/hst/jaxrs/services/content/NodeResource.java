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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.jaxrs.model.content.HippoDocumentRepresentation;
import org.hippoecm.hst.jaxrs.model.content.HippoDocumentRepresentationDataset;
import org.hippoecm.hst.jaxrs.model.content.HippoFolderRepresentation;
import org.hippoecm.hst.jaxrs.model.content.HippoFolderRepresentationDataset;
import org.hippoecm.hst.jaxrs.model.content.NodeProperty;
import org.hippoecm.hst.jaxrs.model.content.NodeRepresentation;
import org.hippoecm.hst.jaxrs.model.content.PropertyValue;
import org.hippoecm.hst.jaxrs.util.NodePropertyUtils;
import org.hippoecm.hst.util.PropertyDefinitionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
@Path("/")
public class NodeResource extends AbstractNodeResource {

    private static Logger log = LoggerFactory.getLogger(NodeResource.class);
    
    @GET
    @Path("/")
    public NodeRepresentation getResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @MatrixParam("pf") Set<String> pf) {
        
    	HstRequestContext requestContext = getRequestContext(servletRequest);    	
    	Node requestContentNode = getRequestContentNode(requestContext);
    	NodeRepresentation representation = null;
    	
    	try {
        	HippoBean hippoBean = (HippoBean) getObjectConverter().getObject(requestContentNode);
        	
            if (hippoBean instanceof HippoDocumentBean) {
                representation = new HippoDocumentRepresentation().represent(hippoBean, pf);
            } else if (hippoBean instanceof HippoFolderBean) {
                representation = new HippoFolderRepresentation().represent(hippoBean, pf);
            } else {
                representation = new NodeRepresentation().represent(hippoBean, pf);
            }
        } 
    	catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } 
            else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
    	return representation;
    }
    
    @GET
    @Path("/folders")
    public HippoFolderRepresentationDataset getFolders(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @MatrixParam("sorted") boolean sorted, @MatrixParam("pf") Set<String> pf) {
        
        HippoFolderRepresentationDataset dataset = new HippoFolderRepresentationDataset();
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(getRequestContext(servletRequest));
        List<NodeRepresentation> folderNodes = new ArrayList<NodeRepresentation>();
        dataset.setNodeRepresentations(folderNodes);
        
        try {
            for (HippoFolderBean subFolderBean : hippoFolderBean.getFolders(sorted)) {
                folderNodes.add(new HippoFolderRepresentation().represent(subFolderBean, pf));
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
        
        return dataset;
    }
    
    @POST
    @Path("/folders")
    public void createFolder(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @QueryParam("name") String folderName) {
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(getRequestContext(servletRequest));
        createContentNode(servletRequest, hippoFolderBean, "hippostd:folder", folderName);
    }
    
    @DELETE
    @Path("/folders")
    public void deleteFolder(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @QueryParam("name") String folderName) {
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(getRequestContext(servletRequest));
        deleteContentNode(servletRequest, hippoFolderBean, folderName);
    }
    
    @GET
    @Path("/documents")
    public HippoDocumentRepresentationDataset getDocuments(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @MatrixParam("sorted") boolean sorted, @MatrixParam("pf") Set<String> pf) {
        
        HippoDocumentRepresentationDataset dataset = new HippoDocumentRepresentationDataset();
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(getRequestContext(servletRequest));
        List<NodeRepresentation> documentNodes = new ArrayList<NodeRepresentation>();
        dataset.setNodeRepresentations(documentNodes);
        
        try {
            for (HippoDocumentBean documentBean : hippoFolderBean.getDocuments(sorted)) {
                documentNodes.add(new HippoDocumentRepresentation().represent(documentBean, pf));
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
        
        return dataset;
    }
    
    @POST
    @Path("/documents")
    public void createDocument(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @QueryParam("type") String nodeTypeName, @QueryParam("name") String documentName) {
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(getRequestContext(servletRequest));
        createContentNode(servletRequest, hippoFolderBean, nodeTypeName, documentName);
    }
    
    @DELETE
    @Path("/documents")
    public void deleteDocument(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @QueryParam("name") String documentName) {
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(getRequestContext(servletRequest));
        deleteContentNode(servletRequest, hippoFolderBean, documentName);
    }
    
    @GET
    @Path("/property/{propertyName}")
    public NodeProperty getNodeProperty(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("propertyName") String propertyName) {
        
        NodeRepresentation representation = getResource(servletRequest, servletResponse, uriInfo, null);
        return representation.getProperty(propertyName);
    }
    
    @POST
    @Path("/property/{propertyName}")
    public NodeProperty setNodeProperty(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("propertyName") String propertyName, @FormParam("pv") List<String> propertyValues) {
        
        HstRequestContext requestContext = getRequestContext(servletRequest);
        Node requestContentNode = getRequestContentNode(requestContext);
        PropertyDefinition propDef = null;
        
        try {
            propDef = PropertyDefinitionUtils.getPropertyDefinition(requestContentNode, propertyName);
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve property definition.", e);
            } else {
                log.warn("Failed to retrieve property definition. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
        
        if (propDef == null) {
            if (log.isWarnEnabled()) {
                log.warn("Failed to retrieve property definition: " + propertyName);
            }
            throw new WebApplicationException(new IllegalArgumentException("No property definition found: " + propertyName));
        }
        
        NodeProperty nodeProperty = new NodeProperty(propertyName);
        nodeProperty.setType(propDef.getRequiredType());
        nodeProperty.setMultiple(propDef.isMultiple());
        PropertyValue [] values = null;
        if (propertyValues != null) {
            values = new PropertyValue[propertyValues.size()];
            int index = 0;
            for (String pv : propertyValues) {
                values[index++] = new PropertyValue(pv);
            }
        }
        nodeProperty.setValues(values);
        
        try {
            NodePropertyUtils.setProperty(requestContentNode, nodeProperty);
            requestContentNode.save();
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to set property.", e);
            } else {
                log.warn("Failed to set property. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
        
        return nodeProperty;
    }
}
