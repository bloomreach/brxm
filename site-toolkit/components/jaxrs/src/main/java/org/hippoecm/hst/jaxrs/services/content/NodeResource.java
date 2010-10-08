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
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.PropertyDefinition;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.hippoecm.hst.content.beans.manager.ObjectBeanPersistenceManager;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManagerImpl;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.jaxrs.JAXRSService;
import org.hippoecm.hst.jaxrs.model.content.HippoDocumentRepresentation;
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
 *
 */
@Path("/")
public class NodeResource {

    private static Logger log = LoggerFactory.getLogger(NodeResource.class);
    
    private List<Class<? extends HippoBean>> annotatedClasses;
    private ObjectConverter objectConverter;
    private HstQueryManager hstQueryManager;
    
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
    
    /* TODO: temporary simple NodeRepresentation Factory */
    protected NodeRepresentation getRepresentation(HippoBean hippoBean, Set<String> pf) throws RepositoryException {
    	if (hippoBean instanceof HippoDocumentBean) {
        	return new HippoDocumentRepresentation().represent(hippoBean, pf);
    	}
    	return new NodeRepresentation().represent(hippoBean, pf);
    }
    
    @GET
    @Path("/{resourceType}/")
    public  NodeRepresentation getResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, @PathParam("resourceType") String resourceType, @QueryParam("pf") Set<String> pf) {
    	HstRequestContext requestContext = getRequestContext(servletRequest);    	
    	Node requestContentNode = getRequestContentNode(requestContext);
    	NodeRepresentation representation = null;
    	try {
        	HippoBean hippoBean = (HippoBean)getObjectConverter().getObject(requestContentNode);
        	if (hippoBean != null) {
        		representation = getRepresentation(hippoBean, pf);
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
    @Path("/{resourceType}/children")
    public  List<NodeRepresentation> getChildren(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, @PathParam("resourceType") String resourceType, @QueryParam("pf") Set<String> pf) {
    	HstRequestContext requestContext = getRequestContext(servletRequest);    	
    	Node requestContentNode = getRequestContentNode(requestContext);
    	List<NodeRepresentation> children = null;
    	try {
        	HippoBean hippoBean = (HippoBean)getObjectConverter().getObject(requestContentNode);
        	if (hippoBean != null) {
        		children = new ArrayList<NodeRepresentation>();
        		// TODO: should have a HippoBean method for this instead of having to access its Node property directly
                for (NodeIterator it = hippoBean.getNode().getNodes(); it.hasNext(); ) {
                    Node childNode = it.nextNode();                    
                    if (childNode != null) {
                    	hippoBean = (HippoBean)getObjectConverter().getObject(childNode);
                        children.add(getRepresentation(hippoBean, pf));
                    }
                }        		
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
    	return children;
    }
    
    @GET
    @Path("/{resourceType}/property/{propertyName}")
    public NodeProperty getNodeProperty(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, @PathParam("resourceType") String resourceType, @PathParam("propertyName") String propertyName) {
        NodeRepresentation representation = getResource(servletRequest, servletResponse, uriInfo, resourceType, null);
        return representation.getProperty(propertyName);
    }
    
    @POST
    @Path("/{resourceType}/property/{propertyName}")
    public NodeProperty setNodeProperty(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, @PathParam("resourceType") String resourceType, @PathParam("propertyName") String propertyName, @FormParam("pv") List<String> propertyValues) {
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
