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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.ObjectBeanPersistenceException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanPersistenceManager;
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
import org.hippoecm.hst.jaxrs.model.content.PropertyValue;
import org.hippoecm.hst.jaxrs.util.NodePropertyUtils;
import org.hippoecm.hst.util.PropertyDefinitionUtils;
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
            @PathParam("resourceType") String resourceType, @MatrixParam("pf") Set<String> propertyFilters) {
    	HstRequestContext requestContext = getRequestContext(servletRequest);    	
    	Node requestContentNode = getRequestContentNode(requestContext);
    	return getNodeRepresentation(requestContentNode, propertyFilters);
    }
    
    @POST
    @Path("/property/{propertyName}/")
    public NodeProperty setContentResourceProperty(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @PathParam("propertyName") String propertyName, @FormParam("pv") List<String> propertyValues) {
        HstRequestContext requestContext = getRequestContext(servletRequest);
        Node requestContentNode = getRequestContentNode(requestContext);
        NodeProperty nodeProperty = setResourceNodeProperty(requestContentNode, propertyName, propertyValues);
        
        try {
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
    public void deleteContentResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            @PathParam("resourceType") String resourceType) {
        try {
            HippoBean hippoBean = getRequestContentBean(getRequestContext(servletRequest));
            deleteContentBean(servletRequest, hippoBean);
        } catch (ObjectBeanManagerException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to convert content node to content bean.", e);
            } else {
                log.warn("Failed to convert content node to content bean. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
    }
    
    @GET
    @Path("/folders/")
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
    @Path("/folders/{folderName}/")
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
    @Path("/folders/{folderName}/")
    public void createFolder(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @PathParam("folderName") String folderName) {
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(getRequestContext(servletRequest));
        createContentResource(servletRequest, hippoFolderBean, "hippostd:folder", folderName);
    }
    
    @DELETE
    @Path("/folders/{folderName}/")
    public void deleteFolder(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @PathParam("folderName") String folderName) {
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(getRequestContext(servletRequest));
        deleteContentResource(servletRequest, hippoFolderBean, folderName);
    }
    
    @GET
    @Path("/documents/")
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
    @Path("/documents/{documentName}/")
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
    @Path("/documents/{nodeTypeName}/{documentName}/")
    public void createDocument(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @PathParam("nodeTypeName") String nodeTypeName, @PathParam("documentName") String documentName) {
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(getRequestContext(servletRequest));
        createContentResource(servletRequest, hippoFolderBean, nodeTypeName, documentName);
    }
    
    @DELETE
    @Path("/documents/{documentName}/")
    public void deleteDocument(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @PathParam("documentName") String documentName) {
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(getRequestContext(servletRequest));
        deleteContentResource(servletRequest, hippoFolderBean, documentName);
    }
    
    @GET
    @Path("/childbeans/")
    public NodeRepresentationDataset getChildBeans(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @MatrixParam("type") String childNodePrimaryNodeType, @MatrixParam("name") String childNodeName, @MatrixParam("pf") Set<String> propertyFilters) {
        if (StringUtils.isBlank(childNodePrimaryNodeType) && StringUtils.isBlank(childNodeName)) {
            if (log.isWarnEnabled()) {
                log.warn("primary node type name or node name must be provided.");
            }
            throw new WebApplicationException(new IllegalArgumentException("primary node type name or node name must be provided."));
        }
        
        HippoBean hippoBean = null;
        
        try {
            hippoBean = getRequestContentBean(getRequestContext(servletRequest));
        } catch (ObjectBeanManagerException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to convert content node to content bean.", e);
            } else {
                log.warn("Failed to convert content node to content bean. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
        
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
    @Path("/childbeans/{childName}/")
    public NodeRepresentation getChildBean(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @PathParam("childName") String childName, @MatrixParam("pf") Set<String> propertyFilters) {
        HippoBean childBean = null;
        
        try {
            HippoBean hippoBean = getRequestContentBean(getRequestContext(servletRequest));
            childBean = hippoBean.getBean(childName);
        } catch (ObjectBeanManagerException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to convert content node to content bean.", e);
            } else {
                log.warn("Failed to convert content node to content bean. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
        
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
    @Path("/childbeans/{childName}/property/{propertyName}/")
    public NodeProperty setChildResourceProperty(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @PathParam("childName") String childName, @PathParam("propertyName") String propertyName, @FormParam("pv") List<String> propertyValues) {
        HippoBean childBean = null;
        
        try {
            HippoBean hippoBean = getRequestContentBean(getRequestContext(servletRequest));
            childBean = hippoBean.getBean(childName);
        } catch (ObjectBeanManagerException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to convert content node to content bean.", e);
            } else {
                log.warn("Failed to convert content node to content bean. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
        
        if (childBean == null) {
            if (log.isWarnEnabled()) {
                log.warn("Cannot find a bean named '{}'", childName);
            }
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        
        Node childBeanNode = childBean.getNode();
        NodeProperty nodeProperty = setResourceNodeProperty(childBeanNode, propertyName, propertyValues);
        
        try {
            childBeanNode.save();
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
    @Path("/childbeans/{childName}/")
    public void deleteChildBean(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @PathParam("childName") String childName) {
        try {
            HippoBean hippoBean = getRequestContentBean(getRequestContext(servletRequest));
            deleteContentResource(servletRequest, hippoBean, childName);
        } catch (ObjectBeanManagerException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to convert content node to content bean.", e);
            } else {
                log.warn("Failed to convert content node to content bean. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
    }
    
    protected NodeRepresentation getNodeRepresentation(Node contentNode, Set<String> propertyFilters) throws WebApplicationException {
        NodeRepresentation nodeRepresentation = null;
        
        try {
            HippoBean hippoBean = (HippoBean) getObjectConverter().getObject(contentNode);
            
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
    
    protected HippoFolderBean getRequestContentAsHippoFolderBean(HstRequestContext requestContext) throws WebApplicationException {
        HippoBean hippoBean = null;
        
        try {
            hippoBean = getRequestContentBean(requestContext);
        } catch (ObjectBeanManagerException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to convert content node to content bean.", e);
            } else {
                log.warn("Failed to convert content node to content bean. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }

        if (!hippoBean.isHippoFolderBean()) {
            if (log.isWarnEnabled()) {
                log.warn("The content bean is not a folder bean.");
            }
            throw new WebApplicationException(new IllegalArgumentException("The content bean is not a folder bean."));
        }
        
        return (HippoFolderBean) hippoBean;
    }
    
    protected HippoDocumentBean getRequestContentAsHippoDocumentBean(HstRequestContext requestContext) throws WebApplicationException {
        HippoBean hippoBean = null;
        
        try {
            hippoBean = getRequestContentBean(requestContext);
        } catch (ObjectBeanManagerException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to convert content node to content bean.", e);
            } else {
                log.warn("Failed to convert content node to content bean. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
        
        if (!hippoBean.isHippoDocumentBean()) {
            if (log.isWarnEnabled()) {
                log.warn("The content bean is not a document bean.");
            }
            throw new WebApplicationException(new IllegalArgumentException("The content bean is not a document bean."));
        }
        
        return (HippoDocumentBean) hippoBean;
    }
    
    protected void createContentResource(HttpServletRequest servletRequest, HippoFolderBean baseFolderBean, String nodeTypeName, String name) throws WebApplicationException {
        ObjectBeanPersistenceManager obpm = null;
        
        try {
            obpm = getContentPersistenceManager(getRequestContext(servletRequest).getSession());
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to create workflow persistence manager.", e);
            } else {
                log.warn("Failed to create workflow persistence manager. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
        
        try {
            obpm.create(baseFolderBean.getPath(), nodeTypeName, name);
        } catch (ObjectBeanPersistenceException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to create node.", e);
            } else {
                log.warn("Failed to create node. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
    }
    
    protected void deleteContentResource(HttpServletRequest servletRequest, HippoBean baseBean, String relPath) throws WebApplicationException {
        HippoBean child = baseBean.getBean(relPath);
        
        if (child == null) {
            if (log.isWarnEnabled()) {
                log.warn("Child node not found: " + relPath);
            }
            throw new WebApplicationException(new IllegalArgumentException("Child node not found: " + relPath));
        }
        
        deleteContentBean(servletRequest, child);
    }
    
    protected void deleteContentBean(HttpServletRequest servletRequest, HippoBean hippoBean) throws WebApplicationException {
        ObjectBeanPersistenceManager obpm = null;
        
        try {
            obpm = getContentPersistenceManager(getRequestContext(servletRequest).getSession());
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to create workflow persistence manager.", e);
            } else {
                log.warn("Failed to create workflow persistence manager. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
        
        try {
            obpm.remove(hippoBean);
            obpm.save();
        } catch (ObjectBeanPersistenceException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to remove hippo bean.", e);
            } else {
                log.warn("Failed to remove hippo bean. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
    }
    
    protected NodeProperty setResourceNodeProperty(Node resourceNode, String propertyName, List<String> propertyValues) {
        
        PropertyDefinition propDef = null;
        
        try {
            propDef = PropertyDefinitionUtils.getPropertyDefinition(resourceNode, propertyName);
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
            NodePropertyUtils.setProperty(resourceNode, nodeProperty);
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
