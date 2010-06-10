/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.services.support.jaxrs.content;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanPersistenceManager;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.util.NodeUtils;
import org.hippoecm.repository.api.HippoQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ContentService
 * 
 * @version $Id$
 */
@Path("/contentservice/")
public class ContentService extends BaseHstContentService {
    
    private static final String SERVICE_PATH = StringUtils.removeEnd(ContentService.class.getAnnotation(Path.class).value(), "/");
    
    private static Logger log = LoggerFactory.getLogger(ContentService.class);
    
    public ContentService() {
        super();
    }
    
    @GET
    @Path("/query/{path:.*}")
    public HippoBeanContentCollection queryContentItems(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @QueryParam("fulljcrquery") String fullJcrQuery,
            @PathParam("path") List<PathSegment> pathSegments, 
            @QueryParam("type") Set<String> nodeTypes,
            @QueryParam("sortby") String sortBy,
            @QueryParam("sortdir") @DefaultValue("descending") String sortDirection,
            @QueryParam("scope") @DefaultValue(".") String queryScope, 
            @QueryParam("op") @DefaultValue("contains") String queryOperator, 
            @QueryParam("query") String queryText, 
            @QueryParam("jcrexpr") String jcrExpression, 
            @QueryParam("lang") @DefaultValue("xpath") String queryLanguage, 
            @QueryParam("begin") @DefaultValue("0") String beginIndex,
            @QueryParam("end") @DefaultValue("100") String endIndex,
            @QueryParam("pv") Set<String> propertyNamesFilledWithValues) {
        
        long begin = Math.max(0L, Long.parseLong(beginIndex));
        long end = Long.parseLong(endIndex);
        
        if (end < 0) {
            end = Long.MAX_VALUE;
        }
        
        if (!StringUtils.isBlank(fullJcrQuery)) {
            return queryContentItemsByFullJcrQuery(servletRequest, uriInfo, fullJcrQuery, queryLanguage, begin, end, propertyNamesFilledWithValues);
        }
        
        HippoBeanContentCollection beanContents = new HippoBeanContentCollection();
        
        try {
            String scopeItemPath = getContentItemPath(servletRequest, pathSegments);
            
            if (!getHstRequestContext(servletRequest).getSession().itemExists(scopeItemPath)) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            
            Item item = getHstRequestContext(servletRequest).getSession().getItem(scopeItemPath);
            
            if (!item.isNode()) {
                throw new IllegalArgumentException("Invalid scope node path: " + scopeItemPath);
            }
            
            String urlBase = getRequestURIBase(uriInfo, servletRequest) + SERVICE_PATH;
            String encoding = servletRequest.getCharacterEncoding();
            
            Node scopeNode = (Node) item;
            
            ObjectBeanPersistenceManager cpm = getContentPersistenceManager(servletRequest);
            HstQueryManager queryManager = getHstQueryManager();
            HstQuery hstQuery = null;
            
            if (CollectionUtils.isEmpty(nodeTypes)) {
                hstQuery = queryManager.createQuery(scopeNode);
            } else if (nodeTypes.size() == 1) {
                hstQuery = queryManager.createQuery(scopeNode, nodeTypes.iterator().next(), true);
            } else {
                hstQuery = queryManager.createQuery(scopeNode, nodeTypes.toArray(new String[nodeTypes.size()]));
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
            
            if (!StringUtils.isBlank(jcrExpression)) {
                filter.addJCRExpression(jcrExpression);
            }
            
            hstQuery.setFilter(filter);
            HstQueryResult result = hstQuery.execute();
            long totalSize = result.getSize();
            HippoBeanIterator iterator = result.getHippoBeans();
            
            // don't skip past unreachable item:
            if (begin < totalSize) {
                iterator.skip((int) begin);
            }
            
            List<HippoBeanContent> list = new LinkedList<HippoBeanContent>();
            
            long maxCount = end - begin;
            long count = 0;
            
            while (iterator.hasNext() && count < maxCount) {
                HippoBean bean = iterator.nextHippoBean();
                
                if (bean != null) {
                    HippoBeanContent beanContent = createHippoBeanContent(bean, propertyNamesFilledWithValues);
                    beanContent.buildUri(urlBase, getSiteContentPath(servletRequest), encoding);
                    beanContent.setPageUri(getPageUriByCanonicalUuid(servletRequest, servletResponse, beanContent.getCanonicalUuid()));
                    list.add(beanContent);
                    count++;
                }
            }
            
            beanContents = new HippoBeanContentCollection(list);
            beanContents.setTotalSize(totalSize);
            beanContents.setBeginIndex(begin);
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
        
        return beanContents;
    }
    
    @GET
    @Path("/uuid/{uuid}/")
    public HippoBeanContent getContentNodeByUUID(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, @PathParam("uuid") String uuid, 
            @QueryParam("pv") Set<String> propertyNamesFilledWithValues) {
        
        HippoBeanContent beanContent = new HippoBeanContent();
        
        try {
            ObjectBeanPersistenceManager cpm = getContentPersistenceManager(servletRequest);
            HippoBean bean = (HippoBean) cpm.getObjectByUuid(uuid);
            
            if (bean != null) {
                beanContent = createHippoBeanContent(bean, propertyNamesFilledWithValues);
                String encoding = servletRequest.getCharacterEncoding();
                String urlBase = getRequestURIBase(uriInfo, servletRequest) + SERVICE_PATH;
                beanContent.buildUri(urlBase, getSiteContentPath(servletRequest), encoding);
                beanContent.buildChildUris(urlBase, getSiteContentPath(servletRequest), encoding);
                beanContent.setPageUri(getPageUriByCanonicalUuid(servletRequest, servletResponse, beanContent.getCanonicalUuid()));
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            e.printStackTrace();
            throw new WebApplicationException(e);
        }
        
        return beanContent;
    }
    
    @GET
    @Path("/{path:.*}")
    public ItemContent getContentItem(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("path") List<PathSegment> pathSegments, 
            @QueryParam("pv") Set<String> propertyNamesFilledWithValues) {
        
        String itemPath = getContentItemPath(servletRequest, pathSegments);
        ItemContent itemContent = new ItemContent();
        
        try {
            if (!getHstRequestContext(servletRequest).getSession().itemExists(itemPath)) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            
            Item item = getHstRequestContext(servletRequest).getSession().getItem(itemPath);
            
            if (item.isNode()) {
                ObjectBeanPersistenceManager cpm = getContentPersistenceManager(servletRequest);
                HippoBean bean = (HippoBean) cpm.getObject(itemPath);
                
                if (bean != null) {
                    HippoBeanContent beanContent = createHippoBeanContent(bean, propertyNamesFilledWithValues);
                    String encoding = servletRequest.getCharacterEncoding();
                    String urlBase = getRequestURIBase(uriInfo, servletRequest) + SERVICE_PATH;
                    beanContent.buildUri(urlBase, getSiteContentPath(servletRequest), encoding);
                    beanContent.buildChildUris(urlBase, getSiteContentPath(servletRequest), encoding);
                    beanContent.setPageUri(getPageUriByCanonicalUuid(servletRequest, servletResponse, beanContent.getCanonicalUuid()));
                    itemContent = beanContent;
                }
            } else {
                PropertyContent propContent = new PropertyContent((Property) item);
                String encoding = servletRequest.getCharacterEncoding();
                String urlBase = getRequestURIBase(uriInfo, servletRequest) + SERVICE_PATH;
                propContent.buildUri(urlBase, getSiteContentPath(servletRequest), encoding);
                itemContent = propContent;
            }
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
        
        return itemContent;
    }
    
    @DELETE
    @Path("/{path:.*}")
    public Response deleteContentNode(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("path") List<PathSegment> pathSegments) {
        
        String itemPath = getContentItemPath(servletRequest, pathSegments);
        
        try {
            ObjectBeanPersistenceManager cpm = getContentPersistenceManager(servletRequest);
            HippoBean bean = (HippoBean) cpm.getObject(itemPath);
            
            if (bean == null) {
                throw new WebApplicationException(new IllegalArgumentException("Invalid item path: " + itemPath), Response.Status.NOT_FOUND);
            }
            
            HippoBeanContent beanContent = createHippoBeanContent(bean, null);
            cpm.remove(bean);
            cpm.save();
            
            return Response.ok().build();
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
    }
    
    @POST
    @Path("/{path:.*}")
    public Response createContentDocument(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("path") List<PathSegment> pathSegments, HippoDocumentBeanContent documentBeanContent) {
        
        String parentPath = getContentItemPath(servletRequest, pathSegments);
        String itemPath = parentPath + "/" + documentBeanContent.getName();
        
        try {
            ObjectBeanPersistenceManager cpm = getContentPersistenceManager(servletRequest);
            PropertyContent primaryTypePropertyContent = documentBeanContent.getPropertyContent("jcr:primaryType");
            
            if (primaryTypePropertyContent == null) {
                throw new WebApplicationException(new IllegalArgumentException("primary node type name not found."), Response.Status.BAD_REQUEST);
            }
            
            cpm.create(parentPath, (String) primaryTypePropertyContent.getValueContents()[0].getValue(), documentBeanContent.getName(), true);
            cpm.save();
            
            HippoBean bean = (HippoBean) cpm.getObject(itemPath);
            
            if (bean == null) {
                throw new WebApplicationException(new IllegalArgumentException("Invalid item path: " + itemPath), Response.Status.NOT_FOUND);
            }
            
            documentBeanContent = (HippoDocumentBeanContent) createHippoBeanContent(bean, null);
            documentBeanContent.setPageUri(getPageUriByCanonicalUuid(servletRequest, servletResponse, documentBeanContent.getCanonicalUuid()));
            return Response.status(Response.Status.CREATED).entity(documentBeanContent).build();
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to save document.", e);
            } else {
                log.warn("Failed to save document. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
    }
    
    @POST
    @Path("/node/{path:.*}")
    public Response createContentNode(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("path") List<PathSegment> pathSegments, NodeContent nodeContent) {
        
        String parentPath = getContentItemPath(servletRequest, pathSegments);
        
        try {
            ObjectBeanPersistenceManager cpm = getContentPersistenceManager(servletRequest);
            HippoBean bean = (HippoBean) cpm.getObject(parentPath);
            
            if (bean == null) {
                throw new WebApplicationException(new IllegalArgumentException("Invalid item path: " + parentPath), Response.Status.NOT_FOUND);
            }
            
            HippoBeanContent beanContent = createHippoBeanContent(bean, null);
            Node canonicalParentNode = NodeUtils.getCanonicalNode(bean.getNode());
            
            Node node = null;
            
            if (nodeContent != null) {
                String primaryNodeTypeName = nodeContent.getPrimaryNodeTypeName();
                
                if (primaryNodeTypeName != null) {
                    node = canonicalParentNode.addNode(nodeContent.getName());
                } else {
                    node = canonicalParentNode.addNode(nodeContent.getName(), primaryNodeTypeName);
                }
                
                Collection<PropertyContent> propertyContents = nodeContent.getPropertyContents();
                
                if (propertyContents != null) {
                    for (PropertyContent propertyContent : propertyContents) {
                        setPropertyValue(node, propertyContent);
                    }
                }
            }
            
            node.getSession().save();
            bean = (HippoBean) cpm.getObject(parentPath);
            beanContent = createHippoBeanContent(bean, null);
            beanContent.setPageUri(getPageUriByCanonicalUuid(servletRequest, servletResponse, beanContent.getCanonicalUuid()));
            
            return Response.status(Response.Status.CREATED).entity(beanContent).build();
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to save document.", e);
            } else {
                log.warn("Failed to save document. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
    }
    
    @PUT
    @Path("/{path:.*}")
    public Response updateContentProperty(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            @PathParam("path") List<PathSegment> pathSegments, PropertyContent propertyContent) {
        
        String itemPath = getContentItemPath(servletRequest, pathSegments);
        
        try {
            ObjectBeanPersistenceManager cpm = getContentPersistenceManager(servletRequest);
            
            HippoBean bean = (HippoBean) cpm.getObject(itemPath);
            
            if (bean == null) {
                throw new WebApplicationException(new IllegalArgumentException("Invalid item path: " + itemPath), Response.Status.NOT_FOUND);
            }
            
            HippoBeanContent beanContent = createHippoBeanContent(bean, null);
            Node canonicalNode = NodeUtils.getCanonicalNode(bean.getNode());
            
            setPropertyValue(canonicalNode, propertyContent);
            
            cpm.update(bean);
            cpm.save();
            
            return Response.ok().build();
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve save bean.", e);
            } else {
                log.warn("Failed to retrieve save bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
    }
    
    private void setPropertyValue(Node canonicalNode, PropertyContent propertyContent) throws Exception {
        int type = propertyContent.getType();
        boolean isMultiple = Boolean.parseBoolean(propertyContent.getMultiple());
        ValueContent [] valueContents = propertyContent.getValueContents();
        Object [] args = null;
        
        if (!isMultiple) {
            args = new Object [] { propertyContent.getName(), valueContents[0].getValue(), new Integer(type) };
            MethodUtils.invokeExactMethod(canonicalNode, "setProperty", args, new Class [] { String.class, String.class, int.class });
        } else {
            String [] values = new String[valueContents.length];
            
            for (int i = 0; i < valueContents.length; i++) {
                values[i] = valueContents[i].getValue();
            }
            
            args = new Object [] { propertyContent.getName(), values, new Integer(type) };
            MethodUtils.invokeExactMethod(canonicalNode, "setProperty", args, new Class [] { String.class, String [].class, int.class });
        }
    }
    
    private HippoBeanContentCollection queryContentItemsByFullJcrQuery(
            final HttpServletRequest servletRequest, 
            final UriInfo uriInfo,
            final String fullJcrQuery, 
            final String queryLanguage, 
            long begin, 
            long end,
            final Set<String> propertyNamesFilledWithValues) {
        
        HippoBeanContentCollection beanContents = new HippoBeanContentCollection();
        
        try {
            String urlBase = getRequestURIBase(uriInfo, servletRequest) + SERVICE_PATH;
            String encoding = servletRequest.getCharacterEncoding();
            
            Query query = getHstRequestContext(servletRequest).getSession().getWorkspace().getQueryManager().createQuery(fullJcrQuery, queryLanguage);
            
            if (query instanceof HippoQuery) {
                ((HippoQuery) query).setLimit(end);
            }
            
            QueryResult result = query.execute();
            List<HippoBeanContent> list = new LinkedList<HippoBeanContent>();
            NodeIterator iterator = result.getNodes();
            long totalSize = iterator.getSize();
            
            // don't skip past unreachable item:
            if (begin < totalSize) {
                iterator.skip(begin);
            }
            
            long maxCount = end - begin;
            long count = 0;
            
            while (iterator.hasNext() && count < maxCount) {
                Node node = iterator.nextNode();
                
                if (node != null) {
                    try {
                        HippoBean bean = (HippoBean) getObjectConverter().getObject(node);
                        
                        if (bean != null) {
                            HippoBeanContent beanContent = createHippoBeanContent(bean, propertyNamesFilledWithValues);
                            beanContent.buildUri(urlBase, getSiteContentPath(servletRequest), encoding);
                            list.add(beanContent);
                            count++;
                        }
                    }  catch (ObjectBeanManagerException e) {
                        if (log.isDebugEnabled()) {
                            log.warn("Exception occurred during object converting.", e);
                        } else if (log.isWarnEnabled()) {
                            log.warn("Exception occurred during object converting. {}", e.toString());
                        }
                    }
                }
            }
            
            beanContents = new HippoBeanContentCollection(list);
            beanContents.setTotalSize(totalSize);
            beanContents.setBeginIndex(begin);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
        
        return beanContents;        
    }
}
