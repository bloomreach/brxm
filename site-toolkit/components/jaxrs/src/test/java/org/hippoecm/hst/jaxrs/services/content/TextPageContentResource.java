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

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
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
import org.hippoecm.hst.jaxrs.model.beans.TextPage;
import org.hippoecm.hst.jaxrs.model.content.HippoHtmlRepresentation;
import org.hippoecm.hst.jaxrs.model.content.TextPageRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
@Path("/testproject:textpage/")
public class TextPageContentResource extends AbstractContentResource {
    
    private static Logger log = LoggerFactory.getLogger(TextPageContentResource.class);
    
    @GET
    @Path("/")
    public TextPageRepresentation getTextPageResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);       
            TextPage textPage = (TextPage) getRequestContentBean(requestContext);
            return new TextPageRepresentation().represent(textPage);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Path("/html/")
    public HippoHtmlRepresentation getHippoHtmlRepresentation(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
                                                              @MatrixParam("path") String relPath,
                                                              @MatrixParam("target") String targetMountAlias) {
        return super.getHippoHtmlRepresentation(servletRequest, relPath, targetMountAlias);
    }

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

    @PUT
    @Path("/html/content/")
    public String updateHippoHtmlContent(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo,
                                         @MatrixParam("path") String relPath, String htmlContent) {
        return super.updateHippoHtmlContent(servletRequest, relPath, htmlContent);
    }

    @RolesAllowed(value={"manager"})
    @GET
    @Path("/protected/")
    public TextPageRepresentation getProtectedTextPageResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        return getTextPageResource(servletRequest, servletResponse, uriInfo);
    }
    
    @DenyAll
    @GET
    @Path("/denyall/")
    public TextPageRepresentation getDenyAllTextPageResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        return getTextPageResource(servletRequest, servletResponse, uriInfo);
    }
    
    @PermitAll
    @GET
    @Path("/permitall/")
    public TextPageRepresentation getPermitAllTextPageResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        return getTextPageResource(servletRequest, servletResponse, uriInfo);
    }
    
    @PUT
    @Path("/")
    public TextPageRepresentation updateTextPageResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo, 
            TextPageRepresentation textPageRepresentation) {
        TextPage textPage = null;
        HstRequestContext requestContext = getRequestContext(servletRequest);
        
        try {
            textPage = (TextPage) getRequestContentBean(requestContext);
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
            final TextPageRepresentation textPageRepresentationInput = textPageRepresentation;
            
            wpm.update(textPage, new ContentNodeBinder() {
                public boolean bind(Object content, Node node) throws ContentNodeBindingException {
                    try {
                        node.setProperty("testproject:title", textPageRepresentationInput.getTitle());
                        node.setProperty("testproject:summary", textPageRepresentationInput.getSummary());
                        Node htmlNode = node.getNode("testproject:body");
                        htmlNode.setProperty("hippostd:content", textPageRepresentationInput.getBodyContent());
                        return true;
                    } catch (RepositoryException e) {
                        throw new ContentNodeBindingException(e);
                    }
                }
            });
            wpm.save();
            
            textPage = (TextPage) wpm.getObject(textPage.getPath());
            textPageRepresentation = new TextPageRepresentation().represent(textPage);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
        
        return textPageRepresentation;
    }
}
