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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;

@Path("/rep:root/")
public class RootResource extends AbstractConfigResource {
    
    private String rootPath;

    public void setRootPath(final String rootPath) {
        this.rootPath = rootPath;
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
        session.removeAttribute(ContainerConstants.RENDER_VARIANT);
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

    @POST
    @Path("/setvariant/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setVariant(@Context HttpServletRequest servletRequest, @FormParam("variant") String variant) {
        servletRequest.getSession().setAttribute(ContainerConstants.RENDER_VARIANT, variant);
        return ok("Variant set");
    }

    @POST
    @Path("/clearvariant/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearVariant(@Context HttpServletRequest servletRequest, String variant) {
        servletRequest.getSession().removeAttribute(ContainerConstants.RENDER_VARIANT);
        return ok("Variant cleared");
    }

}
