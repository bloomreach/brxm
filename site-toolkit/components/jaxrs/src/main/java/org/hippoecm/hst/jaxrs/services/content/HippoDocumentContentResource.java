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
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.jaxrs.model.content.HippoDocumentRepresentation;
import org.hippoecm.hst.jaxrs.model.content.NodeProperty;
import org.hippoecm.hst.jaxrs.model.content.NodeRepresentation;
import org.hippoecm.hst.jaxrs.model.content.NodeRepresentationDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
@Path("/hippo:document/")
public class HippoDocumentContentResource extends AbstractContentResource {
    
    private static Logger log = LoggerFactory.getLogger(HippoDocumentContentResource.class);
    
    @GET
    @Path("/")
    public HippoDocumentRepresentation getDocumentResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @MatrixParam("pf") Set<String> propertyFilters) {
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);       
            HippoDocumentBean documentBean = (HippoDocumentBean) getRequestContentBean(requestContext);
            return new HippoDocumentRepresentation().represent(documentBean, propertyFilters);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } 
            else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
    }
    
    @POST
    @Path("/property/{propertyName}/")
    public NodeProperty setDocumentResourceProperty(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
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
    
    @GET
    @Path("/childbeans/")
    public NodeRepresentationDataset getChildBeans(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @MatrixParam("type") String childNodePrimaryNodeType, @MatrixParam("name") String childNodeName, @MatrixParam("pf") Set<String> propertyFilters) {
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
            @PathParam("childName") String childName, @MatrixParam("pf") Set<String> propertyFilters) {
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
            @PathParam("childName") String childName, @PathParam("propertyName") String propertyName, @FormParam("pv") List<String> propertyValues) {
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
        NodeProperty nodeProperty = null;
        
        try {
            nodeProperty = setResourceNodeProperty(childBeanNode, propertyName, propertyValues);
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
            @PathParam("childName") String childName) {
        try {
            HippoBean hippoBean = getRequestContentBean(getRequestContext(servletRequest));
            deleteContentResource(servletRequest, hippoBean, childName);
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
