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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
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
import org.hippoecm.hst.jaxrs.model.content.NodeRepresentationDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
@Path("/")
public class NodeResource extends AbstractNodeResource {
    
    private static Logger log = LoggerFactory.getLogger(NodeResource.class);
    
    @GET
    @Path("/{resourceType}/")
    public NodeRepresentation getContentNode(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @MatrixParam("pf") Set<String> propertyFilters) {
    	HstRequestContext requestContext = getRequestContext(servletRequest);    	
    	Node requestContentNode = getRequestContentNode(requestContext);
    	return getNodeRepresentation(requestContentNode, propertyFilters);
    }
    
    @POST
    @Path("/{resourceType}/property/{propertyName}/")
    public NodeProperty setContentNodeProperty(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @PathParam("propertyName") String propertyName, @FormParam("pv") List<String> propertyValues) {
        HstRequestContext requestContext = getRequestContext(servletRequest);
        Node requestContentNode = getRequestContentNode(requestContext);
        return setResourceNodeProperty(requestContentNode, propertyName, propertyValues);
    }
    
    @DELETE
    @Path("/{resourceType}/")
    public void deleteContentNode(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            @PathParam("resourceType") String resourceType) {
        HippoBean hippoBean = getRequestContentAsHippoBean(getRequestContext(servletRequest));
        deleteContentNode(servletRequest, hippoBean);
    }
    
    @GET
    @Path("/{resourceType}/folders/")
    public HippoFolderRepresentationDataset getFolders(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @MatrixParam("sorted") boolean sorted, @MatrixParam("pf") Set<String> propertyFilters) {
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(getRequestContext(servletRequest));
        List<NodeRepresentation> folderNodes = new ArrayList<NodeRepresentation>();
        HippoFolderRepresentationDataset dataset = new HippoFolderRepresentationDataset(folderNodes);
        
        try {
            for (HippoFolderBean childFolderBean : hippoFolderBean.getFolders(sorted)) {
                folderNodes.add(new HippoFolderRepresentation().represent(childFolderBean, propertyFilters));
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
    
    @GET
    @Path("/{resourceType}/folders/{folderName}/")
    public HippoFolderRepresentation getFolder(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @PathParam("folderName") String folderName, @MatrixParam("pf") Set<String> propertyFilters) {
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(getRequestContext(servletRequest));
        HippoFolderBean childFolderBean = hippoFolderBean.getBean(folderName, HippoFolderBean.class);
        
        if (childFolderBean == null) {
            if (log.isWarnEnabled()) {
                log.warn("Cannot find a folder named '{}'", folderName);
            }
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        
        try {
            return new HippoFolderRepresentation().represent(childFolderBean, propertyFilters);
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
    }
    
    @POST
    @Path("/{resourceType}/folders/{folderName}/")
    public void createFolder(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @PathParam("folderName") String folderName) {
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(getRequestContext(servletRequest));
        createContentNode(servletRequest, hippoFolderBean, "hippostd:folder", folderName);
    }
    
    @DELETE
    @Path("/{resourceType}/folders/{folderName}/")
    public void deleteFolder(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @PathParam("folderName") String folderName) {
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(getRequestContext(servletRequest));
        deleteContentNode(servletRequest, hippoFolderBean, folderName);
    }
    
    @GET
    @Path("/{resourceType}/documents/")
    public HippoDocumentRepresentationDataset getDocuments(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @MatrixParam("sorted") boolean sorted, @MatrixParam("pf") Set<String> propertyFilters) {
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(getRequestContext(servletRequest));
        List<NodeRepresentation> documentNodes = new ArrayList<NodeRepresentation>();
        HippoDocumentRepresentationDataset dataset = new HippoDocumentRepresentationDataset(documentNodes);
        
        try {
            for (HippoDocumentBean documentBean : hippoFolderBean.getDocuments(sorted)) {
                documentNodes.add(new HippoDocumentRepresentation().represent(documentBean, propertyFilters));
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
    
    @GET
    @Path("/{resourceType}/documents/{documentName}/")
    public HippoDocumentRepresentation getDocument(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @PathParam("documentName") String documentName, @MatrixParam("pf") Set<String> propertyFilters) {
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(getRequestContext(servletRequest));
        HippoDocumentBean childDocumentBean = hippoFolderBean.getBean(documentName, HippoDocumentBean.class);
        
        if (childDocumentBean == null) {
            if (log.isWarnEnabled()) {
                log.warn("Cannot find a folder named '{}'", documentName);
            }
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        
        try {
            return new HippoDocumentRepresentation().represent(childDocumentBean, propertyFilters);
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
    }
    
    @POST
    @Path("/{resourceType}/documents/{nodeTypeName}/{documentName}/")
    public void createDocument(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @PathParam("nodeTypeName") String nodeTypeName, @PathParam("documentName") String documentName) {
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(getRequestContext(servletRequest));
        createContentNode(servletRequest, hippoFolderBean, nodeTypeName, documentName);
    }
    
    @DELETE
    @Path("/{resourceType}/documents/{documentName}/")
    public void deleteDocument(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @PathParam("documentName") String documentName) {
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(getRequestContext(servletRequest));
        deleteContentNode(servletRequest, hippoFolderBean, documentName);
    }
    
    @GET
    @Path("/{resourceType}/childbeans/")
    public NodeRepresentationDataset getChildBeans(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @MatrixParam("type") String childNodePrimaryNodeType, @MatrixParam("name") String childNodeName, @MatrixParam("pf") Set<String> propertyFilters) {
        if (StringUtils.isBlank(childNodePrimaryNodeType) && StringUtils.isBlank(childNodeName)) {
            if (log.isWarnEnabled()) {
                log.warn("primary node type name or node name must be provided.");
            }
            throw new WebApplicationException(new IllegalArgumentException("primary node type name or node name must be provided."));
        }
        
        HippoBean hippoBean = getRequestContentAsHippoBean(getRequestContext(servletRequest));
        List<NodeRepresentation> childNodeRepresentations = new ArrayList<NodeRepresentation>();
        NodeRepresentationDataset dataset = new NodeRepresentationDataset(childNodeRepresentations);
        
        List<HippoBean> childBeans = null;
        
        if (!StringUtils.isBlank(childNodePrimaryNodeType)) {
            childBeans = hippoBean.getChildBeans(childNodePrimaryNodeType);
        } else {
            childBeans = hippoBean.getChildBeansByName(childNodeName);
        }
        
        try {
            for (HippoBean childBean : childBeans) {
                childNodeRepresentations.add(new NodeRepresentation().represent(childBean, propertyFilters));
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
    
    @GET
    @Path("/{resourceType}/childbeans/{childName}/")
    public NodeRepresentation getChildBean(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @PathParam("childName") String childName, @MatrixParam("pf") Set<String> propertyFilters) {
        HippoBean hippoBean = getRequestContentAsHippoBean(getRequestContext(servletRequest));
        HippoBean childBean = hippoBean.getBean(childName);
        
        if (childBean == null) {
            if (log.isWarnEnabled()) {
                log.warn("Cannot find a bean named '{}'", childName);
            }
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        
        try {
            return new NodeRepresentation().represent(childBean, propertyFilters);
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
    }
    
    @POST
    @Path("/{resourceType}/childbeans/{childName}/property/{propertyName}/")
    public NodeProperty setChildResourceProperty(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @PathParam("childName") String childName, @PathParam("propertyName") String propertyName, @FormParam("pv") List<String> propertyValues) {
        HippoBean hippoBean = getRequestContentAsHippoBean(getRequestContext(servletRequest));
        HippoBean childBean = hippoBean.getBean(childName);
        
        if (childBean == null) {
            if (log.isWarnEnabled()) {
                log.warn("Cannot find a bean named '{}'", childName);
            }
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        
        return setResourceNodeProperty(childBean.getNode(), propertyName, propertyValues);
    }
    
    @DELETE
    @Path("/{resourceType}/childbeans/{childName}/")
    public void deleteChildBean(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @PathParam("childName") String childName) {
        HippoBean hippoBean = getRequestContentAsHippoBean(getRequestContext(servletRequest));
        deleteContentNode(servletRequest, hippoBean, childName);
    }
}
