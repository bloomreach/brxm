/*
*  Copyright 2010-2011 Hippo.
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
package org.hippoecm.hst.pagecomposer.jaxrs.services;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.PersonasRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/rep:root/")
public class RootResource extends AbstractConfigResource {
    
    private static Logger log = LoggerFactory.getLogger(RootResource.class);

    private String rootPath;

    public void setRootPath(final String rootPath) {
        this.rootPath = rootPath;
    }

    @GET
    @Path("/keepalive/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response keepAlive(@Context HttpServletRequest servletRequest,
                              @Context HttpServletResponse servletResponse) {

        @SuppressWarnings("unused")
        HttpSession session = servletRequest.getSession(true);

        return ok("Keepalive successful", null);
    }

    @GET
    @Path("/composermode/{renderingHost}/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response composerModeGet(@Context HttpServletRequest servletRequest,
                                    @Context HttpServletResponse servletResponse,
                                    @PathParam("renderingHost") String renderingHost) {
        HttpSession session = servletRequest.getSession(true);
        session.setAttribute(ContainerConstants.RENDERING_HOST, renderingHost);
        session.setAttribute(ContainerConstants.COMPOSER_MODE_ATTR_NAME, Boolean.TRUE);

        boolean canWrite;
        try {
            HstRequestContext requestContext = (HstRequestContext) servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
            canWrite = requestContext.getSession().hasPermission(rootPath + "/accesstest", Session.ACTION_SET_PROPERTY);
        } catch (RepositoryException e) {
            return error("Could not determine authorization", e);
        }
        return ok("Composer-Mode successful", canWrite);
    }

    @GET
    @Path("/previewmode/{renderingHost}/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response previewMode(@Context HttpServletRequest servletRequest,
                                @Context HttpServletResponse servletResponse,
                                @PathParam("renderingHost") String renderingHost) {
        HttpSession session = servletRequest.getSession(true);
        session.setAttribute(ContainerConstants.RENDERING_HOST, renderingHost);
        session.setAttribute(ContainerConstants.COMPOSER_MODE_ATTR_NAME, Boolean.FALSE);
        return ok("Preview-Mode successful", null);
    }

    @GET
    @Path("/personas/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response personas(@Context HttpServletRequest servletRequest,
                             @Context HttpServletResponse servletResponse) {
        try {
            Node personasNode = getRequestContext(servletRequest).getSession().getNode("/behavioral:configuration/behavioral:personas");
            PersonasRepresentation personasRepresentation = new PersonasRepresentation(personasNode);
            Object personaArray = personasRepresentation.getPersonas().toArray();
            return ok("Personas loaded successfully", personaArray);
        } catch (RepositoryException e) {
            log.error("Failed to load personas", e);
            return error("Could not load personas", e);
        }
    }

}
