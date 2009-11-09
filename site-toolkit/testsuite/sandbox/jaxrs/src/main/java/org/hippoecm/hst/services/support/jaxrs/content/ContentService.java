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

import java.util.List;

import javax.jcr.Item;
import javax.jcr.Property;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.persistence.ContentPersistenceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/contentservice/")
public class ContentService extends BaseHstContentService {
    
    private static final String SERVICE_PATH = StringUtils.removeEnd(ContentService.class.getAnnotation(Path.class).value(), "/");
    
    private static Logger log = LoggerFactory.getLogger(ContentService.class);
    
    public ContentService() {
        super();
    }
    
    @GET
    @Path("/uuid/{uuid}/")
    public HippoBeanContent getContentNodeByUUID(@Context HttpServletRequest servletRequest, @Context UriInfo uriInfo, @PathParam("uuid") String uuid) {
        HippoBeanContent beanContent = new HippoBeanContent();
        
        try {
            ContentPersistenceManager cpm = getContentPersistenceManager();
            HippoBean bean = (HippoBean) cpm.getObjectByUuid(uuid);
            
            if (bean != null) {
                beanContent = createHippoBeanContent(bean);
                String encoding = servletRequest.getCharacterEncoding();
                beanContent.buildUrl(getRequestURIBase(uriInfo) + SERVICE_PATH, getSiteContentPath(), encoding);
                beanContent.buildChildUrls(getRequestURIBase(uriInfo) + SERVICE_PATH, getSiteContentPath(), encoding);
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
        }
        
        return beanContent;
    }
    
    @GET
    @Path("/{path:.*}")
    public ItemContent getContentItem(@Context HttpServletRequest servletRequest, @Context UriInfo uriInfo, @PathParam("path") List<PathSegment> pathSegments) {
        String itemPath = getContentItemPath(servletRequest, pathSegments);
        ItemContent itemContent = new ItemContent();
        
        try {
            Item item = getHstRequestContext().getSession().getItem(itemPath);
            
            if (item.isNode()) {
                ContentPersistenceManager cpm = getContentPersistenceManager();
                HippoBean bean = (HippoBean) cpm.getObject(itemPath);
                
                if (bean != null) {
                    HippoBeanContent beanContent = createHippoBeanContent(bean);
                    String encoding = servletRequest.getCharacterEncoding();
                    beanContent.buildUrl(getRequestURIBase(uriInfo) + SERVICE_PATH, getSiteContentPath(), encoding);
                    beanContent.buildChildUrls(getRequestURIBase(uriInfo) + SERVICE_PATH, getSiteContentPath(), encoding);
                    itemContent = beanContent;
                }
            } else {
                PropertyContent propContent = new PropertyContent((Property) item);
                String encoding = servletRequest.getCharacterEncoding();
                propContent.buildUrl(getRequestURIBase(uriInfo) + SERVICE_PATH, getSiteContentPath(), encoding);
                itemContent = propContent;
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
        }
        
        return itemContent;
    }
    
    @POST
    @Path("/{path:.*}")
    public Response addDocument(@Context HttpServletRequest servletRequest, @Context UriInfo uriInfo, @PathParam("path") List<PathSegment> pathSegments, HippoDocumentBeanContent documentBeanContent) {
        String itemPath = getContentItemPath(servletRequest, pathSegments);
        
        try {
            ContentPersistenceManager cpm = getContentPersistenceManager();
            int offset = itemPath.lastIndexOf('/');
            String folderPath = itemPath.substring(0, offset);
            cpm.create(folderPath, "collab:wikipage", documentBeanContent.getName(), true);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve save bean.", e);
            } else {
                log.warn("Failed to retrieve save bean. {}", e.toString());
            }
        }
        
        return Response.ok(documentBeanContent).build();
    }
    
}
