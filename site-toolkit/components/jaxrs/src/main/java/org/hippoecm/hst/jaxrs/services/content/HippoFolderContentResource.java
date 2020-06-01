/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.hst.jaxrs.model.content.Link;
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
    public HippoFolderRepresentation getFolderResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);       
            HippoFolderBean folderBean = getRequestContentBean(requestContext, HippoFolderBean.class);
            HippoFolderRepresentation folderRep = new HippoFolderRepresentation().represent(folderBean);
            folderRep.addLink(getNodeLink(requestContext, folderBean));
            folderRep.addLink(getSiteLink(requestContext, folderBean));
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
            @MatrixParam("begin") @DefaultValue("0") String beginIndex,
            @MatrixParam("end") @DefaultValue("100") String endIndex) {
        
        long begin = Math.max(0L, Long.parseLong(beginIndex));
        long end = Long.parseLong(endIndex);
        
        if (end < 0) {
            end = Long.MAX_VALUE;
        }

        HstRequestContext requestContext = getRequestContext(servletRequest);  
        HippoFolderRepresentationDataset dataset = new HippoFolderRepresentationDataset();
            
        try {
            HippoFolderBean hippoFolderBean = getRequestContentBean(requestContext, HippoFolderBean.class);
            List<HippoFolderRepresentation> folderNodes = new ArrayList<HippoFolderRepresentation>();
            dataset.setNodeRepresentations(folderNodes);
            Link parentLink = getNodeLink(requestContext, hippoFolderBean);
            parentLink.setRel(getHstQualifiedLinkRel("parent"));
            dataset.addLink(parentLink);
        
        
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
                HippoFolderRepresentation childFolderRep = new HippoFolderRepresentation();
                childFolderRep.addLink(getNodeLink(requestContext, childFolderBean));
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
            @PathParam("folderName") String folderName) {
        HstRequestContext requestContext = getRequestContext(servletRequest);       
        try {
        
            HippoFolderBean hippoFolderBean = getRequestContentBean(requestContext, HippoFolderBean.class);
            HippoFolderBean childFolderBean = hippoFolderBean.getBean(folderName, HippoFolderBean.class);
            
            if (childFolderBean == null) {
                log.warn("Cannot find a folder named '{}'", folderName);
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
        
        
            HippoFolderRepresentation childFolderRep = new HippoFolderRepresentation().represent(childFolderBean);
            childFolderRep.addLink(getNodeLink(requestContext, childFolderBean));
            childFolderRep.addLink(getSiteLink(requestContext, childFolderBean));
            Link parentLink = getNodeLink(requestContext, hippoFolderBean);
            parentLink.setRel(getHstQualifiedLinkRel("parent"));
            childFolderRep.addLink(parentLink);
            return childFolderRep;
        } catch (Exception e) {
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
    public String createFolderResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("folderName") String folderName) {
        HippoFolderBean hippoFolderBean;
        try {
            hippoFolderBean = getRequestContentBean(getRequestContext(servletRequest), HippoFolderBean.class);
        } catch (ObjectBeanManagerException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
        return createContentResource(servletRequest, hippoFolderBean, "hippostd:folder", folderName);
    }
    
    @DELETE
    @Path("/folders/{folderName}/")
    public String deleteFolderResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("folderName") String folderName) {
        try {
            HippoFolderBean hippoFolderBean = getRequestContentBean(getRequestContext(servletRequest), HippoFolderBean.class);
            return deleteContentResource(servletRequest, hippoFolderBean, folderName);
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
            @MatrixParam("begin") @DefaultValue("0") String beginIndex,
            @MatrixParam("end") @DefaultValue("100") String endIndex) {
        
        long begin = Math.max(0L, Long.parseLong(beginIndex));
        long end = Long.parseLong(endIndex);
        
        if (end < 0) {
            end = Long.MAX_VALUE;
        }
        
        HippoDocumentRepresentationDataset dataset = new HippoDocumentRepresentationDataset();
        
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);       
            HippoFolderBean hippoFolderBean = getRequestContentBean(getRequestContext(servletRequest), HippoFolderBean.class);
            List<HippoDocumentRepresentation> documentNodes = new ArrayList<HippoDocumentRepresentation>();
            dataset.setNodeRepresentations(documentNodes);
            
            Link parentLink = getNodeLink(requestContext, hippoFolderBean);
            parentLink.setRel(getHstQualifiedLinkRel("parent"));
            dataset.addLink(parentLink);
            
        
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
                HippoDocumentRepresentation childDocRep = new HippoDocumentRepresentation();
                childDocRep.addLink(getNodeLink(requestContext, childDocBean));
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
            @PathParam("documentName") String documentName) {
        HstRequestContext requestContext = getRequestContext(servletRequest);
       
        try {
            HippoFolderBean hippoFolderBean = getRequestContentBean(getRequestContext(servletRequest), HippoFolderBean.class);
            HippoDocumentBean childDocumentBean = hippoFolderBean.getBean(documentName, HippoDocumentBean.class);
            
            if (childDocumentBean == null) {
                log.warn("Cannot find a folder named '{}'", documentName);
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
        
        
            HippoDocumentRepresentation docRep = new HippoDocumentRepresentation().represent(childDocumentBean);
            docRep.addLink(getNodeLink(requestContext, childDocumentBean));
            docRep.addLink(getSiteLink(requestContext, childDocumentBean));
            Link parentLink = getNodeLink(requestContext, hippoFolderBean);
            parentLink.setRel(getHstQualifiedLinkRel("parent"));
            docRep.addLink(parentLink);
            return docRep;
        } catch (Exception e) {
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
    public String createDocumentResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("documentName") String documentName, @FormParam("type") String nodeTypeName) {
        HippoFolderBean hippoFolderBean;
        try {
            hippoFolderBean = getRequestContentBean(getRequestContext(servletRequest), HippoFolderBean.class);
        } catch (ObjectBeanManagerException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
        return createContentResource(servletRequest, hippoFolderBean, nodeTypeName, documentName);
    }
    
    @DELETE
    @Path("/documents/{documentName}/")
    public String deleteDocumentResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("documentName") String documentName) {
        try {
            HippoFolderBean hippoFolderBean = getRequestContentBean(getRequestContext(servletRequest), HippoFolderBean.class);
            return deleteContentResource(servletRequest, hippoFolderBean, documentName);
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
            @MatrixParam("op") @DefaultValue("contains") String queryOperator,
            @MatrixParam("scope") @DefaultValue(".") String queryScope,
            @MatrixParam("begin") @DefaultValue("0") String beginIndex,
            @MatrixParam("end") @DefaultValue("100") String endIndex,
            @MatrixParam("query") String queryMatrixParam,
            @QueryParam("query") String queryParam) {
        
        long begin = Math.max(0L, Long.parseLong(beginIndex));
        long end = Long.parseLong(endIndex);
        
        if (end < 0) {
            end = Long.MAX_VALUE;
        }
        
        String queryText = queryParam;
        if (StringUtils.isBlank(queryText)) {
            queryText = queryMatrixParam;
        }
        
        HstRequestContext requestContext = getRequestContext(servletRequest);       
        NodeRepresentationDataset dataset = new NodeRepresentationDataset();
       
        try {
            HippoFolderBean hippoFolderBean = getRequestContentBean(getRequestContext(servletRequest), HippoFolderBean.class);
            List<NodeRepresentation> nodeReps = new ArrayList<NodeRepresentation>();
            dataset.setNodeRepresentations(nodeReps);
        
            ObjectBeanPersistenceManager cpm = getPersistenceManager(requestContext);
            HstQueryManager queryManager = getHstQueryManager(requestContext.getSession(), requestContext);
            
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
                    NodeRepresentation nodeRep = null;
                    
                    if (hippoBean.isHippoFolderBean()) {
                        nodeRep = new HippoFolderRepresentation();
                    } else if (hippoBean.isHippoDocumentBean()) {
                        nodeRep = new HippoDocumentRepresentation();
                    }
                    
                    if (nodeRep != null) {
                        nodeRep.addLink(getNodeLink(requestContext, hippoBean));
                        nodeRep.addLink(getSiteLink(requestContext, hippoBean));
                        nodeReps.add(nodeRep);
                        count++;
                    }
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
    
    
    private String createContentResource(HttpServletRequest servletRequest, HippoFolderBean baseFolderBean, String nodeTypeName, String name) throws WebApplicationException {
        ObjectBeanPersistenceManager obpm = null;
        
        try {
            obpm = getPersistenceManager(getRequestContext(servletRequest));
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to create workflow persistence manager.", e);
            } else {
                log.warn("Failed to create workflow persistence manager. {}", e.toString());
            }
            throw new WebApplicationException(e);
        }
        
        try {
            return obpm.createAndReturn(baseFolderBean.getPath(), nodeTypeName, name, true);
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
