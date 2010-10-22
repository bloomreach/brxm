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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.ObjectBeanPersistenceException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanPersistenceManager;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.jaxrs.model.content.HippoDocumentRepresentation;
import org.hippoecm.hst.jaxrs.model.content.HippoDocumentRepresentationDataset;
import org.hippoecm.hst.jaxrs.model.content.HippoFolderRepresentation;
import org.hippoecm.hst.jaxrs.model.content.HippoFolderRepresentationDataset;
import org.hippoecm.hst.jaxrs.model.content.NodeRepresentation;
import org.hippoecm.hst.jaxrs.model.content.NodeRepresentationDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
@Path("/hippostd:folder/")
public class HippoFolderContentResource extends AbstractContentResource {
    
    private static Logger log = LoggerFactory.getLogger(HippoFolderContentResource.class);
    
    @GET
    @Path("/")
    public HippoFolderRepresentation getFolderResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @MatrixParam("pf") Set<String> propertyFilters) {
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);       
            HippoFolderBean folderBean = (HippoFolderBean) getRequestContentBean(requestContext);
            HippoFolderRepresentation folderRep = new HippoFolderRepresentation().represent(folderBean, propertyFilters);
            folderRep.setPageLink(getPageLinkURL(requestContext, folderBean));
            return folderRep;
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
            @MatrixParam("sorted") boolean sorted, 
            @MatrixParam("pf") Set<String> propertyFilters,
            @MatrixParam("begin") @DefaultValue("0") String beginIndex,
            @MatrixParam("end") @DefaultValue("100") String endIndex) {
        
        long begin = Math.max(0L, Long.parseLong(beginIndex));
        long end = Long.parseLong(endIndex);
        
        if (end < 0) {
            end = Long.MAX_VALUE;
        }

        HstRequestContext requestContext = getRequestContext(servletRequest);       
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(requestContext);
        List<HippoFolderRepresentation> folderNodes = new ArrayList<HippoFolderRepresentation>();
        HippoFolderRepresentationDataset dataset = new HippoFolderRepresentationDataset(folderNodes);
        
        try {
            List<HippoFolderBean> hippoFolderBeans = hippoFolderBean.getFolders(sorted);
            long totalSize = hippoFolderBeans.size();
            long maxCount = end - begin;
            
            Iterator<HippoFolderBean> iterator = hippoFolderBeans.iterator();
            for (int i = 0; i < begin && iterator.hasNext(); i++) {
                iterator.next();
            }
            
            long count = 0;
            
            while (iterator.hasNext() && count < maxCount) {
                HippoFolderBean childFolderBean = iterator.next();
                HippoFolderRepresentation childFolderRep = new HippoFolderRepresentation().represent(childFolderBean, propertyFilters);
                childFolderRep.setPageLink(getPageLinkURL(requestContext, childFolderBean));
                folderNodes.add(childFolderRep);
                count++;
            }
            
            dataset.setTotalSize(totalSize);
            dataset.setBeginIndex(begin);
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
        HstRequestContext requestContext = getRequestContext(servletRequest);       
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(requestContext);
        HippoFolderBean childFolderBean = hippoFolderBean.getBean(folderName, HippoFolderBean.class);
        
        if (childFolderBean == null) {
            if (log.isWarnEnabled()) {
                log.warn("Cannot find a folder named '{}'", folderName);
            }
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        
        try {
            HippoFolderRepresentation childFolderRep = new HippoFolderRepresentation().represent(childFolderBean, propertyFilters);
            childFolderRep.setPageLink(getPageLinkURL(requestContext, childFolderBean));
            return childFolderRep;
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
            @MatrixParam("sorted") boolean sorted, 
            @MatrixParam("pf") Set<String> propertyFilters,
            @MatrixParam("begin") @DefaultValue("0") String beginIndex,
            @MatrixParam("end") @DefaultValue("100") String endIndex) {
        
        long begin = Math.max(0L, Long.parseLong(beginIndex));
        long end = Long.parseLong(endIndex);
        
        if (end < 0) {
            end = Long.MAX_VALUE;
        }

        HstRequestContext requestContext = getRequestContext(servletRequest);       
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(requestContext);
        List<HippoDocumentRepresentation> documentNodes = new ArrayList<HippoDocumentRepresentation>();
        HippoDocumentRepresentationDataset dataset = new HippoDocumentRepresentationDataset(documentNodes);
        
        try {
            List<HippoDocumentBean> hippoDocumentBeans = hippoFolderBean.getDocuments(sorted);
            long totalSize = hippoDocumentBeans.size();
            long maxCount = end - begin;
            
            Iterator<HippoDocumentBean> iterator = hippoDocumentBeans.iterator();
            for (int i = 0; i < begin && iterator.hasNext(); i++) {
                iterator.next();
            }
            
            long count = 0;
            
            while (iterator.hasNext() && count < maxCount) {
                HippoDocumentBean childDocBean = iterator.next();
                HippoDocumentRepresentation childDocRep = new HippoDocumentRepresentation().represent(childDocBean, propertyFilters);
                childDocRep.setPageLink(getPageLinkURL(requestContext, childDocBean));
                documentNodes.add(childDocRep);
                count++;
            }
            
            dataset.setTotalSize(totalSize);
            dataset.setBeginIndex(begin);
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
        HstRequestContext requestContext = getRequestContext(servletRequest);
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(requestContext);
        HippoDocumentBean childDocumentBean = hippoFolderBean.getBean(documentName, HippoDocumentBean.class);
        
        if (childDocumentBean == null) {
            if (log.isWarnEnabled()) {
                log.warn("Cannot find a folder named '{}'", documentName);
            }
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        
        try {
            HippoDocumentRepresentation docRep = new HippoDocumentRepresentation().represent(childDocumentBean, propertyFilters);
            docRep.setPageLink(getPageLinkURL(requestContext, childDocumentBean));
            return docRep;
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
    @Path("/documents/{documentName}/")
    public void createDocumentResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("documentName") String documentName, @FormParam("type") String nodeTypeName) {
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
    
    @GET
    @Path("/search/")
    public NodeRepresentationDataset searchDocumentResources(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @MatrixParam("sortby") String sortBy, 
            @MatrixParam("sortdir") String sortDirection,
            @MatrixParam("type") Set<String> nodeTypes,
            @MatrixParam("pf") Set<String> propertyFilters,
            @MatrixParam("op") @DefaultValue("contains") String queryOperator,
            @MatrixParam("scope") @DefaultValue(".") String queryScope,
            @MatrixParam("begin") @DefaultValue("0") String beginIndex,
            @MatrixParam("end") @DefaultValue("100") String endIndex,
            @QueryParam("query") String queryText) {
        
        long begin = Math.max(0L, Long.parseLong(beginIndex));
        long end = Long.parseLong(endIndex);
        
        if (end < 0) {
            end = Long.MAX_VALUE;
        }
        
        HstRequestContext requestContext = getRequestContext(servletRequest);       
        HippoFolderBean hippoFolderBean = getRequestContentAsHippoFolderBean(requestContext);
        List<NodeRepresentation> nodeReps = new ArrayList<NodeRepresentation>();
        NodeRepresentationDataset dataset = new NodeRepresentationDataset(nodeReps);
        
        try {
            ObjectBeanPersistenceManager cpm = getContentPersistenceManager(requestContext);
            HstQueryManager queryManager = getHstQueryManager(requestContext);
            
            HstQuery hstQuery = null;
            
            if (CollectionUtils.isEmpty(nodeTypes)) {
                hstQuery = queryManager.createQuery(hippoFolderBean);
            } else if (nodeTypes.size() == 1) {
                hstQuery = queryManager.createQuery(hippoFolderBean.getNode(), nodeTypes.iterator().next(), true);
            } else {
                hstQuery = queryManager.createQuery(hippoFolderBean, nodeTypes.toArray(new String[nodeTypes.size()]));
            }
            
            if (!StringUtils.isBlank(sortBy)) {
                if ("descending".equals(sortDirection)) {
                    hstQuery.addOrderByDescending(sortBy);
                } else {
                    hstQuery.addOrderByAscending(sortBy);
                }
            }
            
            Filter filter = hstQuery.createFilter();
            
            if (queryText != null) {
                if ("contains".equals(queryOperator)) {
                    filter.addContains(queryScope, queryText);
                } else if ("equalto".equals(queryOperator)) {
                    filter.addEqualTo(queryScope, queryText);
                }
            }
            
            hstQuery.setFilter(filter);
            HstQueryResult result = hstQuery.execute();
            long totalSize = result.getSize();
            HippoBeanIterator iterator = result.getHippoBeans();
            
            // don't skip past unreachable item:
            if (begin < totalSize) {
                iterator.skip((int) begin);
            }
            
            long maxCount = end - begin;
            long count = 0;
            
            while (iterator.hasNext() && count < maxCount) {
                HippoBean hippoBean = iterator.nextHippoBean();
                
                if (hippoBean != null) {
                    NodeRepresentation nodeRep = new NodeRepresentation().represent(hippoBean);
                    nodeRep.setPageLink(getPageLinkURL(requestContext, hippoBean));
                    nodeReps.add(nodeRep);
                    count++;
                }
            }
            
            dataset.setTotalSize(totalSize);
            dataset.setBeginIndex(begin);
        } catch (WebApplicationException e) {
            throw e;
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
