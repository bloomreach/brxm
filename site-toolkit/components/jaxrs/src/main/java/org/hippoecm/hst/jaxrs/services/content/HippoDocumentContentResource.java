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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.jaxrs.model.content.HippoDocumentRepresentation;
import org.hippoecm.hst.jaxrs.model.content.HippoHtmlRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
@Path("/hippo:document/")
public class HippoDocumentContentResource extends AbstractContentResource {
    
    private static Logger log = LoggerFactory.getLogger(HippoDocumentContentResource.class);
    
    @GET
    @Path("/")
    public HippoDocumentRepresentation getDocumentResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @Context UriInfo uriInfo) {
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);       
            HippoDocumentBean documentBean =  getRequestContentBean(requestContext, HippoDocumentBean.class);
            HippoDocumentRepresentation docRep = new HippoDocumentRepresentation().represent(documentBean);
            docRep.addLink(getNodeLink(requestContext, documentBean));
            docRep.addLink(getSiteLink(requestContext, documentBean));
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
}
