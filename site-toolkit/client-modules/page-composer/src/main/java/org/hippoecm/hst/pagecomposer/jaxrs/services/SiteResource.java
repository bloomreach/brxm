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
package org.hippoecm.hst.pagecomposer.jaxrs.services;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ToolkitRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @version $Id$
 */

@Path("/hst:site/")
public class SiteResource extends AbstractConfigResource {
    private static Logger log = LoggerFactory.getLogger(SiteResource.class);


    @GET
    @Path("/toolkit/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getToolkitRepresentation(@Context HttpServletRequest servletRequest,
                                             @Context HttpServletResponse servletResponse) {

        HstRequestContext requestContext = getRequestContext(servletRequest);
        Mount parentMount = requestContext.getResolvedMount().getMount().getParent();
        if (parentMount == null) {
            log.warn("Page Composer only work when there is a parent Mount");
            return error("Page Composer only work when there is a parent Mount");
        }
        ToolkitRepresentation toolkitRepresentation = new ToolkitRepresentation().represent(parentMount);
        return ok("Toolkit items loaded successfully", toolkitRepresentation.getComponents().toArray());

    }

    @GET
    @Path("/keepalive/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response keepAlive(@Context HttpServletRequest servletRequest,
                              @Context HttpServletResponse servletResponse){
        HttpSession session = servletRequest.getSession(false);
        return ok("Keepalive successful", null);
    }

    @GET
    @Path("/logout/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout(@Context HttpServletRequest servletRequest,
                           @Context HttpServletResponse servletResponse) {

        HttpSession session = servletRequest.getSession(false);
        if (session != null) {
            // logout
            session.invalidate();
        }
        return ok("You are logged out", null);
    }
}
