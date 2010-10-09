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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
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

import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.ObjectBeanPersistenceException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanPersistenceManager;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManager;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManagerImpl;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.jaxrs.JAXRSService;
import org.hippoecm.hst.jaxrs.model.content.HippoDocumentRepresentation;
import org.hippoecm.hst.jaxrs.model.content.HippoDocumentRepresentationDataset;
import org.hippoecm.hst.jaxrs.model.content.HippoFolderRepresentation;
import org.hippoecm.hst.jaxrs.model.content.HippoFolderRepresentationDataset;
import org.hippoecm.hst.jaxrs.model.content.NodeProperty;
import org.hippoecm.hst.jaxrs.model.content.NodeRepresentation;
import org.hippoecm.hst.jaxrs.model.content.PropertyValue;
import org.hippoecm.hst.jaxrs.util.NodePropertyUtils;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.ObjectConverterUtils;
import org.hippoecm.hst.util.PropertyDefinitionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
@Path("/")
public class NodeResource {

    private static Logger log = LoggerFactory.getLogger(NodeResource.class);
    
    private List<Class<? extends HippoBean>> annotatedClasses;
    private ObjectConverter objectConverter;
    private HstQueryManager hstQueryManager;
    private WorkflowPersistenceManager workflowPersistenceManager;
    
    public void setAnnotatedClasses(List<Class<? extends HippoBean>> annotatedClasses) {
        this.annotatedClasses = annotatedClasses;
    }
    
    public void setObjectConverter(ObjectConverter objectConverter) {
    	this.objectConverter = objectConverter;
    }
    
    public void setHstQueryManager(HstQueryManager hstQueryManager) {
    	this.hstQueryManager = hstQueryManager;
    }
    
    protected List<Class<? extends HippoBean>> getAnnotatedClasses() {
        if (annotatedClasses == null) {
            return Collections.emptyList();
        }
        return annotatedClasses;
    }
    
    protected ObjectConverter getObjectConverter() {
        if (objectConverter == null) {
            List<Class<? extends HippoBean>> annotatedClasses = getAnnotatedClasses();
            objectConverter = ObjectConverterUtils.createObjectConverter(annotatedClasses);
        }
        return objectConverter;
    }
    
    protected HstQueryManager getHstQueryManager() {
        if (hstQueryManager == null) {
            ComponentManager compManager = HstServices.getComponentManager();
            if (compManager != null) {
                HstQueryManagerFactory hstQueryManagerFactory = (HstQueryManagerFactory) compManager.getComponent(HstQueryManagerFactory.class.getName());
                hstQueryManager = hstQueryManagerFactory.createQueryManager(getObjectConverter());
            }
        }
        return hstQueryManager;
    }
    
    protected WorkflowPersistenceManager getWorkflowPersistenceManager(HttpServletRequest servletRequest) throws RepositoryException {
        if (workflowPersistenceManager == null) {
            workflowPersistenceManager = new WorkflowPersistenceManagerImpl(getRequestContext(servletRequest).getSession(), getObjectConverter());
        }
        return workflowPersistenceManager;
    }
    
    protected ObjectBeanPersistenceManager getContentPersistenceManager(Session jcrSession) throws LoginException, RepositoryException {
        return new WorkflowPersistenceManagerImpl(jcrSession, getObjectConverter());
    }
    
    protected HstRequestContext getRequestContext(HttpServletRequest servletRequest) {
        return (HstRequestContext) servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
    }
    
    protected String getRequestContentPath(HstRequestContext requestContext) {
    	return (String)requestContext.getAttribute(JAXRSService.REQUEST_CONTENT_PATH_KEY);
    }
    
    protected Node getRequestContentNode(HstRequestContext requestContext) {
    	return (Node)requestContext.getAttribute(JAXRSService.REQUEST_CONTENT_NODE_KEY);
    }
    
    protected HippoBean getRequestContentAsHippoBean(HstRequestContext requestContext) throws WebApplicationException {
        Node requestContentNode = getRequestContentNode(requestContext);
        
        if (requestContentNode == null) {
            if (log.isWarnEnabled()) {
                log.warn("Request content node is not found.");
            }
            throw new WebApplicationException(new IllegalStateException("Request content node is not found."));
        }
        
        try {
            return (HippoBean) getObjectConverter().getObject(requestContentNode);
        } catch (ObjectBeanManagerException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to convert content node.", e);
            } else {
                log.warn("Failed to convert content node. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
    }
    
    protected HippoFolderBean getRequestContentAsHippoFolderBean(HstRequestContext requestContext) throws WebApplicationException {
        HippoBean hippoBean = getRequestContentAsHippoBean(requestContext);
        
        if (!hippoBean.isHippoFolderBean()) {
            if (log.isWarnEnabled()) {
                log.warn("The content bean is not a folder bean.");
            }
            throw new WebApplicationException(new IllegalArgumentException("The content bean is not a folder bean."));
        }
        
        return (HippoFolderBean) hippoBean;
    }
    
    protected HippoDocumentBean getRequestContentAsHippoDocumentBean(HstRequestContext requestContext) throws WebApplicationException {
        HippoBean hippoBean = getRequestContentAsHippoBean(requestContext);
        
        if (!hippoBean.isHippoDocumentBean()) {
            if (log.isWarnEnabled()) {
                log.warn("The content bean is not a document bean.");
            }
            throw new WebApplicationException(new IllegalArgumentException("The content bean is not a document bean."));
        }
        
        return (HippoDocumentBean) hippoBean;
    }
    
    protected void createContentNode(HttpServletRequest servletRequest, HippoFolderBean baseFolderBean, String nodeTypeName, String name) throws WebApplicationException {
        WorkflowPersistenceManager wpm = null;
        
        try {
            wpm = getWorkflowPersistenceManager(servletRequest);
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to create workflow persistence manager.", e);
            } else {
                log.warn("Failed to create workflow persistence manager. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
        
        try {
            wpm.create(baseFolderBean.getPath(), nodeTypeName, name);
        } catch (ObjectBeanPersistenceException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to create node.", e);
            } else {
                log.warn("Failed to create node. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
    }
    
    public void deleteContentNode(HttpServletRequest servletRequest, HippoFolderBean baseFolderBean, String relPath) {
        WorkflowPersistenceManager wpm = null;
        
        try {
            wpm = getWorkflowPersistenceManager(servletRequest);
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to create workflow persistence manager.", e);
            } else {
                log.warn("Failed to create workflow persistence manager. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
        
        HippoBean child = baseFolderBean.getBean(relPath);
        
        if (child == null) {
            if (log.isWarnEnabled()) {
                log.warn("Child node not found: " + relPath);
            }
            throw new WebApplicationException(new IllegalArgumentException("Child node not found: " + relPath));
        }
        
        try {
            wpm.remove(child);
        } catch (ObjectBeanPersistenceException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to create folder.", e);
            } else {
                log.warn("Failed to create folder. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
    }
    
    @GET
    @Path("/{resourceType}/")
    public NodeRepresentation getResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @MatrixParam("pf") Set<String> pf) {
        
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
    @Path("/{resourceType}/folders")
    public HippoFolderRepresentationDataset getFolders(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @MatrixParam("sorted") boolean sorted, @MatrixParam("pf") Set<String> pf) {
        
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
    @Path("/{resourceType}/folders")
    public void createFolder(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @QueryParam("name") String folderName) {
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(getRequestContext(servletRequest));
        createContentNode(servletRequest, hippoFolderBean, "hippostd:folder", folderName);
    }
    
    @DELETE
    @Path("/{resourceType}/folders")
    public void deleteFolder(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @QueryParam("name") String folderName) {
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(getRequestContext(servletRequest));
        deleteContentNode(servletRequest, hippoFolderBean, folderName);
    }
    
    @GET
    @Path("/{resourceType}/documents")
    public HippoDocumentRepresentationDataset getDocuments(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @MatrixParam("sorted") boolean sorted, @MatrixParam("pf") Set<String> pf) {
        
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
    @Path("/{resourceType}/documents")
    public void createDocument(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @QueryParam("type") String nodeTypeName, @QueryParam("name") String documentName) {
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(getRequestContext(servletRequest));
        createContentNode(servletRequest, hippoFolderBean, nodeTypeName, documentName);
    }
    
    @DELETE
    @Path("/{resourceType}/documents")
    public void deleteDocument(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @QueryParam("name") String documentName) {
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(getRequestContext(servletRequest));
        deleteContentNode(servletRequest, hippoFolderBean, documentName);
    }
    
    @GET
    @Path("/{resourceType}/property/{propertyName}")
    public NodeProperty getNodeProperty(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @PathParam("propertyName") String propertyName) {
        
        NodeRepresentation representation = getResource(servletRequest, servletResponse, uriInfo, resourceType, null);
        return representation.getProperty(propertyName);
    }
    
    @POST
    @Path("/{resourceType}/property/{propertyName}")
    public NodeProperty setNodeProperty(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("resourceType") String resourceType, @PathParam("propertyName") String propertyName, @FormParam("pv") List<String> propertyValues) {
        
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
