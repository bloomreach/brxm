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

import java.net.URLDecoder;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.persistence.ContentPersistenceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/contentservice/")
public class ContentService extends BaseHstContentService {
    
    private static Logger log = LoggerFactory.getLogger(ContentService.class);
    
    @Context
    private HttpServletRequest servletRequest;
    
    public ContentService() {
        super();
    }
    
    @GET
    @Path("/node/uuid/{uuid}/")
    public HippoBeanContent getContentNodeByUUID(@PathParam("uuid") String uuid) {
        HippoBeanContent beanContent = new HippoBeanContent();
        
        try {
            ContentPersistenceManager cpm = getContentPersistenceManager();
            HippoBean bean = (HippoBean) cpm.getObjectByUuid(uuid);
            
            if (bean != null) {
                beanContent = createHippoContentBean(bean);
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
    @Path("/node/{path:.*}")
    public HippoBeanContent getContentNode(@PathParam("path") List<PathSegment> pathSegments) {
        String itemPath = getContentItemPath(pathSegments);
        HippoBeanContent beanContent = new HippoBeanContent();
        
        try {
            ContentPersistenceManager cpm = getContentPersistenceManager();
            HippoBean bean = (HippoBean) cpm.getObject(itemPath);
            
            if (bean != null) {
                beanContent = createHippoContentBean(bean);
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
    @Path("/property/{property}/uuid/{uuid}/")
    public PropertyContent getContentPropertyByUUID(@PathParam("uuid") String uuid, @PathParam("property") String propertyName) {
        PropertyContent propContent = new PropertyContent();
        HippoBeanContent beanContent = getContentNodeByUUID(uuid);
        
        if (beanContent.getBean() != null) {
            try {
                propContent = new PropertyContent(beanContent.getBean().getNode().getProperty(propertyName));
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.warn("Failed to retrieve content bean property.", e);
                } else {
                    log.warn("Failed to retrieve content bean property. {}", e.toString());
                }
            }
        }
        
        return propContent;
    }
    
    @GET
    @Path("/property/{property}/{path:.*}")
    public PropertyContent getContentProperty(@PathParam("path") List<PathSegment> pathSegments, @PathParam("property") String propertyName) {
        PropertyContent propContent = new PropertyContent();
        HippoBeanContent beanContent = getContentNode(pathSegments);
        
        if (beanContent.getBean() != null) {
            try {
                propContent = new PropertyContent(beanContent.getBean().getNode().getProperty(propertyName));
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.warn("Failed to retrieve content bean property.", e);
                } else {
                    log.warn("Failed to retrieve content bean property. {}", e.toString());
                }
            }
        }
        
        return propContent;
    }
    
    private String getContentItemPath(final List<PathSegment> pathSegments) {
        StringBuilder pathBuilder = new StringBuilder(80).append(getSiteContentPath());
        
        for (PathSegment pathSegment : pathSegments) {
            pathBuilder.append('/').append(pathSegment.getPath());
        }
        
        String path = pathBuilder.toString();
        
        String encoding = servletRequest.getCharacterEncoding();
        
        if (encoding == null)
        {
            encoding = "ISO-8859-1";
        }
        
        try {
            path = URLDecoder.decode(path, encoding);
        } catch (Exception e) {
            log.warn("Failed to decode. {}", path);
        }
        
        return path;
    }
    
}
