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
import org.hippoecm.hst.demo.beans.TextBean;
import org.hippoecm.hst.demo.jaxrs.model.BaseDocumentRepresentation;
import org.hippoecm.hst.demo.jaxrs.model.TextDocumentRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
@Path("/demosite:textdocument/")
public class TextDocumentContentResource extends BaseDocumentContentResource {
    
    private static Logger log = LoggerFactory.getLogger(TextDocumentContentResource.class);
    
    @GET
    @Path("/")
    public TextDocumentRepresentation getDocumentResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);       
            TextBean textBean = (TextBean) getRequestContentBean(requestContext);
            return new TextDocumentRepresentation().represent(textBean);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
    }
    
    @PUT
    @Path("/")
    public TextDocumentRepresentation updateDocumentResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            TextDocumentRepresentation documentRepresentation) {
        TextBean textBean = null;
        HstRequestContext requestContext = getRequestContext(servletRequest);
        
        try {
            textBean = (TextBean) getRequestContentBean(requestContext);
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
            
            wpm.update(textBean, new ContentNodeBinder() {
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
            
            textBean = (TextBean) wpm.getObject(textBean.getPath());
            documentRepresentation = new TextDocumentRepresentation().represent(textBean);
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
