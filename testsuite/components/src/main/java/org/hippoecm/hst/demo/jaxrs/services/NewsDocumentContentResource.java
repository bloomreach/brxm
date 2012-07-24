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
package org.hippoecm.hst.demo.jaxrs.services;

import javax.annotation.security.RolesAllowed;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.hippoecm.hst.content.beans.ContentNodeBinder;
import org.hippoecm.hst.content.beans.ContentNodeBindingException;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManager;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.demo.beans.NewsBean;
import org.hippoecm.hst.demo.jaxrs.model.BaseDocumentRepresentation;
import org.hippoecm.hst.demo.jaxrs.model.NewsDocumentRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
@Path("/demosite:newsdocument/")
public class NewsDocumentContentResource extends BaseDocumentContentResource {
    
    private static Logger log = LoggerFactory.getLogger(NewsDocumentContentResource.class);
    
    @GET
    @Path("/")
    public NewsDocumentRepresentation getDocumentResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);       
            NewsBean newsBean = getRequestContentBean(requestContext, NewsBean.class);
            NewsDocumentRepresentation newsRep = new NewsDocumentRepresentation().represent(newsBean);
            newsRep.addLink(getNodeLink(requestContext, newsBean));
            newsRep.addLink(getSiteLink(requestContext, newsBean));
            return newsRep;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
    }
    
    @RolesAllowed( { "admin", "author", "editor" } )
    @PUT
    @Path("/")
    public NewsDocumentRepresentation updateDocumentResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            NewsDocumentRepresentation documentRepresentation) {
        NewsBean newsBean = null;
        HstRequestContext requestContext = getRequestContext(servletRequest);
        
        try {
            newsBean = (NewsBean) getRequestContentBean(requestContext);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
        
        try {
            WorkflowPersistenceManager wpm = (WorkflowPersistenceManager) getContentPersistenceManager(requestContext);
            final BaseDocumentRepresentation documentRepresentationInput = documentRepresentation;
            
            wpm.update(newsBean, new ContentNodeBinder() {
                public boolean bind(Object content, Node node) throws ContentNodeBindingException {
                    try {
                        node.setProperty("demosite:title", documentRepresentationInput.getTitle());
                        node.setProperty("demosite:summary", documentRepresentationInput.getSummary());
                        return true;
                    } catch (RepositoryException e) {
                        throw new ContentNodeBindingException(e);
                    }
                }
            });
            wpm.save();
            
            newsBean = (NewsBean) wpm.getObject(newsBean.getPath());
            documentRepresentation = new NewsDocumentRepresentation().represent(newsBean);
            documentRepresentation.addLink(getNodeLink(requestContext, newsBean));
            documentRepresentation.addLink(getSiteLink(requestContext, newsBean));
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
        
        return documentRepresentation;
    }
}
