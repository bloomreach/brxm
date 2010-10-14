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

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

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
import org.hippoecm.hst.jaxrs.model.content.NodeRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
@Path("/hippostd:folder/")
public class HippoFolderContentResource extends AbstractContentResource {
    
    private static Logger log = LoggerFactory.getLogger(HippoFolderContentResource.class);
    
    @Path("/")
    public HippoFolderRepresentation getFolderResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @MatrixParam("pf") Set<String> propertyFilters) {
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);       
            HippoFolderBean folderBean = (HippoFolderBean) getRequestContentBean(requestContext);
            return new HippoFolderRepresentation().represent(folderBean, propertyFilters);
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
    
    @GET
    @Path("/folders/")
    public HippoFolderRepresentationDataset getFolderResources(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @MatrixParam("sorted") boolean sorted, @MatrixParam("pf") Set<String> propertyFilters) {
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
    public HippoFolderRepresentation getFolderResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("folderName") String folderName, @MatrixParam("pf") Set<String> propertyFilters) {
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
    public void createFolderResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("folderName") String folderName) {
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(getRequestContext(servletRequest));
        createContentResource(servletRequest, hippoFolderBean, "hippostd:folder", folderName);
    }
    
    @DELETE
    @Path("/folders/{folderName}/")
    public void deleteFolderResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("folderName") String folderName) {
        try {
            HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(getRequestContext(servletRequest));
            deleteContentResource(servletRequest, hippoFolderBean, folderName);
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to delete content folder.", e);
            } else {
                log.warn("Failed to delete content folder. {}", e.toString());
            }
            throw new WebApplicationException(e);
        } catch (ObjectBeanManagerException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to convert content folder to content bean.", e);
            } else {
                log.warn("Failed to convert content folder to content bean. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
    }
    
    @GET
    @Path("/documents/")
    public HippoDocumentRepresentationDataset getDocumentResources(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @MatrixParam("sorted") boolean sorted, @MatrixParam("pf") Set<String> propertyFilters) {
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
    public HippoDocumentRepresentation getDocumentResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("documentName") String documentName, @MatrixParam("pf") Set<String> propertyFilters) {
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
    public void createDocumentResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("nodeTypeName") String nodeTypeName, @PathParam("documentName") String documentName) {
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(getRequestContext(servletRequest));
        createContentResource(servletRequest, hippoFolderBean, nodeTypeName, documentName);
    }
    
    @DELETE
    @Path("/documents/{documentName}/")
    public void deleteDocumentResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("documentName") String documentName) {
        try {
            HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(getRequestContext(servletRequest));
            deleteContentResource(servletRequest, hippoFolderBean, documentName);
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to delete content folder.", e);
            } else {
                log.warn("Failed to delete content folder. {}", e.toString());
            }
            throw new WebApplicationException(e);
        } catch (ObjectBeanManagerException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to convert content folder to content bean.", e);
            } else {
                log.warn("Failed to convert content folder to content bean. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
    }
    
    private HippoFolderBean getRequestContentAsHippoFolderBean(HstRequestContext requestContext) throws WebApplicationException {
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
    
    private void createContentResource(HttpServletRequest servletRequest, HippoFolderBean baseFolderBean, String nodeTypeName, String name) throws WebApplicationException {
        ObjectBeanPersistenceManager obpm = null;
        
        try {
            obpm = getContentPersistenceManager(getRequestContext(servletRequest));
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

}
