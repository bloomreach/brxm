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

import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.persistence.ContentPersistenceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/contentservice/")
public class ContentService extends BaseHstContentService {
    
    private static final String SERVICE_PATH = StringUtils.removeEnd(ContentService.class.getAnnotation(Path.class).value(), "/");
    
    private static Logger log = LoggerFactory.getLogger(ContentService.class);
    
    @Context
    private HttpServletRequest servletRequest;
    
    @Context
    private UriInfo uriInfo;
    
    public ContentService() {
        super();
    }
    
    @GET
    @Path("/uuid/{uuid}/")
    public HippoBeanContent getContentNodeByUUID(@PathParam("uuid") String uuid) {
        HippoBeanContent beanContent = new HippoBeanContent();
        
        try {
            ContentPersistenceManager cpm = getContentPersistenceManager(servletRequest);
            HippoBean bean = (HippoBean) cpm.getObjectByUuid(uuid);
            
            if (bean != null) {
                beanContent = createHippoBeanContent(bean);
                String encoding = servletRequest.getCharacterEncoding();
                beanContent.buildUri(getRequestURIBase(uriInfo) + SERVICE_PATH, getSiteContentPath(servletRequest), encoding);
                beanContent.buildChildUris(getRequestURIBase(uriInfo) + SERVICE_PATH, getSiteContentPath(servletRequest), encoding);
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
        
        return beanContent;
    }
    
    @GET
    @Path("/{path:.*}")
    public ItemContent getContentItem(@PathParam("path") List<PathSegment> pathSegments) {
        String itemPath = getContentItemPath(servletRequest, pathSegments);
        ItemContent itemContent = new ItemContent();
        
        try {
            Item item = getHstRequestContext(servletRequest).getSession().getItem(itemPath);
            
            if (item.isNode()) {
                ContentPersistenceManager cpm = getContentPersistenceManager(servletRequest);
                HippoBean bean = (HippoBean) cpm.getObject(itemPath);
                
                if (bean != null) {
                    HippoBeanContent beanContent = createHippoBeanContent(bean);
                    String encoding = servletRequest.getCharacterEncoding();
                    beanContent.buildUri(getRequestURIBase(uriInfo) + SERVICE_PATH, getSiteContentPath(servletRequest), encoding);
                    beanContent.buildChildUris(getRequestURIBase(uriInfo) + SERVICE_PATH, getSiteContentPath(servletRequest), encoding);
                    itemContent = beanContent;
                }
            } else {
                PropertyContent propContent = new PropertyContent((Property) item);
                String encoding = servletRequest.getCharacterEncoding();
                propContent.buildUri(getRequestURIBase(uriInfo) + SERVICE_PATH, getSiteContentPath(servletRequest), encoding);
                itemContent = propContent;
            }
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
    public Response deleteContentNode(@PathParam("path") List<PathSegment> pathSegments) {
        String itemPath = getContentItemPath(servletRequest, pathSegments);
        
        try {
            ContentPersistenceManager cpm = getContentPersistenceManager(servletRequest);
            HippoBean bean = (HippoBean) cpm.getObject(itemPath);
            
            if (bean == null) {
                return Response.serverError().status(Response.Status.NOT_FOUND).build();
            } else {
                HippoBeanContent beanContent = createHippoBeanContent(bean);
                cpm.remove(bean);
                cpm.save();
                return Response.ok(beanContent).build();
            }
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
    public Response createContentDocument(@PathParam("path") List<PathSegment> pathSegments, HippoDocumentBeanContent documentBeanContent) {
        String itemPath = getContentItemPath(servletRequest, pathSegments);
        
        try {
            String primaryType = null;
            Collection<PropertyContent> propertyContents = documentBeanContent.getPropertyContents();
            
            if (propertyContents != null) {
                for (PropertyContent propertyContent : propertyContents) {
                    if ("jcr:primaryType".equals(propertyContent.getName())) {
                        primaryType = propertyContent.getValues()[0].toString();
                        break;
                    }
                }
            }
            
            if (primaryType == null) {
                return Response.serverError().status(Response.Status.BAD_REQUEST).build();
            }
            
            ContentPersistenceManager cpm = getContentPersistenceManager(servletRequest);
            int offset = itemPath.lastIndexOf('/');
            String folderPath = itemPath.substring(0, offset);
            
            cpm.create(folderPath, primaryType, documentBeanContent.getName(), true);
            cpm.save();
            
            HippoBean bean = (HippoBean) cpm.getObject(itemPath);
            
            if (bean == null) {
                return Response.serverError().status(Response.Status.NOT_FOUND).build();
            } else {
                documentBeanContent = (HippoDocumentBeanContent) createHippoBeanContent(bean);
                return Response.status(Response.Status.CREATED).entity(documentBeanContent).build();
            }
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
    public Response createContentNode(@PathParam("path") List<PathSegment> pathSegments, NodeContent nodeContent) {
        String itemPath = getContentItemPath(servletRequest, pathSegments);
        
        try {
            ContentPersistenceManager cpm = getContentPersistenceManager(servletRequest);
            int offset = itemPath.lastIndexOf('/');
            String parentPath = itemPath.substring(0, offset);
            String nodeName = itemPath.substring(offset + 1);
            
            HippoBean bean = (HippoBean) cpm.getObject(parentPath);
            HippoBeanContent beanContent = createHippoBeanContent(bean);
            
            if (bean == null) {
                return Response.serverError().status(Response.Status.NOT_FOUND).build();
            } else {
                Node canonicalParentNode = beanContent.getCanonicalNode();
                Node node = canonicalParentNode.addNode(nodeName);
                
                Collection<PropertyContent> propertyContents = nodeContent.getPropertyContents();
                
                if (propertyContents != null) {
                    for (PropertyContent propertyContent : propertyContents) {
                        setPropertyValue(node, propertyContent);
                    }
                }
                
                node.getSession().save();
                
                bean = (HippoBean) cpm.getObject(parentPath);
                beanContent = createHippoBeanContent(bean);
                
                return Response.status(Response.Status.CREATED).entity(beanContent).build();
            }
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
    public Response updateContentItem(@PathParam("path") List<PathSegment> pathSegments, ItemContent itemContent) {
        String itemPath = getContentItemPath(servletRequest, pathSegments);
        
        try {
            ContentPersistenceManager cpm = getContentPersistenceManager(servletRequest);
            
            if (itemContent instanceof PropertyContent) {
                PropertyContent propertyContent = (PropertyContent) itemContent;
                int offset = itemPath.lastIndexOf('/');
                String nodePath = itemPath.substring(0, offset);
                
                HippoBean bean = (HippoBean) cpm.getObject(nodePath);
                
                if (bean == null) {
                    return Response.serverError().status(Response.Status.NOT_FOUND).build();
                } else {
                    HippoBeanContent beanContent = createHippoBeanContent(bean);
                    setPropertyValue(beanContent.getCanonicalNode(), propertyContent);
                    cpm.update(bean);
                    cpm.save();
                    
                    bean = (HippoBean) cpm.getObject(nodePath);
                    beanContent = (HippoBeanContent) createHippoBeanContent(bean);
                    
                    return Response.status(Response.Status.ACCEPTED).entity(beanContent).build();
                }
            } else if (itemContent instanceof HippoBeanContent) {
                HippoBean bean = (HippoBean) cpm.getObject(itemPath);
                
                if (bean == null) {
                    return Response.serverError().status(Response.Status.NOT_FOUND).build();
                } else {
                    HippoBeanContent beanContent = createHippoBeanContent(bean);
                    Collection<PropertyContent> propertyContents = ((HippoBeanContent) itemContent).getPropertyContents();
                    
                    if (propertyContents != null) {
                        for (PropertyContent propertyContent : propertyContents) {
                            setPropertyValue(beanContent.getCanonicalNode(), propertyContent);
                        }
                    }
                    
                    cpm.update(bean);
                    cpm.save();
                    
                    bean = (HippoBean) cpm.getObject(itemPath);
                    beanContent = (HippoBeanContent) createHippoBeanContent(bean);
                    
                    return Response.status(Response.Status.ACCEPTED).entity(beanContent).build();
                }
            }
            
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
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
        boolean isMultiple = Boolean.parseBoolean(propertyContent.getMultiple());
        int propertyType = PropertyType.valueFromName(propertyContent.getType());
        Object [] values = propertyContent.getValues();
        
        switch (propertyType) {
        case PropertyType.BOOLEAN: 
            if (isMultiple) {
                MethodUtils.invokeExactMethod(canonicalNode, "setProperty", values, new Class [] { boolean [].class });
            } else {
                MethodUtils.invokeExactMethod(canonicalNode, "setProperty", values, new Class [] { boolean.class });
            }
            break;
        case PropertyType.NAME:
        case PropertyType.REFERENCE:
        case PropertyType.STRING:
            if (isMultiple) {
                MethodUtils.invokeExactMethod(canonicalNode, "setProperty", values, new Class [] { String [].class });
            } else {
                MethodUtils.invokeExactMethod(canonicalNode, "setProperty", values, new Class [] { String.class });
            }
            break;
        case PropertyType.LONG :
            if (isMultiple) {
                MethodUtils.invokeExactMethod(canonicalNode, "setProperty", values, new Class [] { long [].class });
            } else {
                MethodUtils.invokeExactMethod(canonicalNode, "setProperty", values, new Class [] { long.class });
            }
            break;
        case PropertyType.DOUBLE :
            if (isMultiple) {
                MethodUtils.invokeExactMethod(canonicalNode, "setProperty", values, new Class [] { double [].class });
            } else {
                MethodUtils.invokeExactMethod(canonicalNode, "setProperty", values, new Class [] { double.class });
            }
            break;
        case PropertyType.DATE :
            if (isMultiple) {
                MethodUtils.invokeExactMethod(canonicalNode, "setProperty", values, new Class [] { Calendar [].class });
            } else {
                MethodUtils.invokeExactMethod(canonicalNode, "setProperty", values, new Class [] { Calendar.class });
            }
            break;
        }
    }
    
}
