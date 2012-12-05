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
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.hippoecm.hst.content.beans.ContentNodeBinder;
import org.hippoecm.hst.content.beans.ContentNodeBindingException;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManager;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.demo.beans.BaseBean;
import org.hippoecm.hst.demo.jaxrs.model.BaseDocumentRepresentation;
import org.hippoecm.hst.jaxrs.model.content.HippoHtmlRepresentation;
import org.hippoecm.hst.jaxrs.services.content.AbstractContentResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
@Path("/demosite:basedocument/")
public class BaseDocumentContentResource extends AbstractContentResource {
    
    private static Logger log = LoggerFactory.getLogger(BaseDocumentContentResource.class);
    
    @GET
    @Path("/")
    public BaseDocumentRepresentation getDocumentResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);       
            BaseBean baseBean = getRequestContentBean(requestContext, BaseBean.class);
            BaseDocumentRepresentation docRep = new BaseDocumentRepresentation().represent(baseBean);
            docRep.addLink(getNodeLink(requestContext, baseBean));
            docRep.addLink(getSiteLink(requestContext, baseBean));
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
    
    @RolesAllowed( { "admin", "author", "editor" } )
    @PUT
    @Path("/")
    public BaseDocumentRepresentation updateDocumentResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            BaseDocumentRepresentation documentRepresentation) {
        BaseBean baseBean = null;
        HstRequestContext requestContext = getRequestContext(servletRequest);
        
        try {
            baseBean = getRequestContentBean(requestContext, BaseBean.class);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
        
        try {
            WorkflowPersistenceManager wpm = (WorkflowPersistenceManager) getPersistenceManager(requestContext);
            final BaseDocumentRepresentation documentRepresentationInput = documentRepresentation;
            
            wpm.update(baseBean, new ContentNodeBinder() {
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
            
            baseBean = (BaseBean) wpm.getObject(baseBean.getPath());
            documentRepresentation = new BaseDocumentRepresentation().represent(baseBean);
            documentRepresentation.addLink(getNodeLink(requestContext, baseBean));
            documentRepresentation.addLink(getSiteLink(requestContext, baseBean));
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
    
    @GET
    @Path("/html/")
    public HippoHtmlRepresentation getHippoHtmlRepresentation(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            @MatrixParam("path") String relPath,
            @MatrixParam("target") String targetMountAlias) {
        return super.getHippoHtmlRepresentation(servletRequest, relPath, targetMountAlias);
    }
    
    @RolesAllowed( { "admin", "author", "editor" } )
    @PUT
    @Path("/html/")
    public HippoHtmlRepresentation updateHippoHtmlRepresentation(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            @MatrixParam("path") String relPath, HippoHtmlRepresentation htmlRepresentation) {
        return super.updateHippoHtmlRepresentation(servletRequest, relPath, htmlRepresentation);
    }
    
    @GET
    @Path("/html/content/")
    public String getHippoHtmlContent(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            @MatrixParam("path") String relPath,
            @MatrixParam("target") String targetMountAlias) {
        return super.getHippoHtmlContent(servletRequest, relPath, targetMountAlias);
    }
    
    @RolesAllowed( { "admin", "author", "editor" } )
    @PUT
    @Path("/html/content/")
    public String updateHippoHtmlContent(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
            @MatrixParam("path") String relPath, String htmlContent) {
        return super.updateHippoHtmlContent(servletRequest, relPath, htmlContent);
    }
}
