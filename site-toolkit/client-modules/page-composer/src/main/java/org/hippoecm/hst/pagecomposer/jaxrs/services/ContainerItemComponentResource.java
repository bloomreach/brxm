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

import java.util.HashMap;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.hosting.SiteMount;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ComponentWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The REST resource handler for the nodes that are of the type "hst:containeritemcomoponent". This is specified using the @Path annotation.
 *
 * @version $Id$
 */

@Path("/hst:containeritemcomponent/")
public class ContainerItemComponentResource extends AbstractConfigResource {
    private static Logger log = LoggerFactory.getLogger(ContainerItemComponentResource.class);
    private static final String HST_PARAMETERVALUES = "hst:parametervalues";
    private static final String HST_PARAMETERNAMES = "hst:parameternames";


    @GET
    @Path("/parameters/")
    @Produces(MediaType.APPLICATION_JSON)
    public ComponentWrapper getParameters(@Context HttpServletRequest servletRequest,
                                          @Context HttpServletResponse servletResponse) {

        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);
            SiteMount parentMount = requestContext.getResolvedSiteMount().getSiteMount().getParent();
            if (parentMount == null) {
                log.warn("Page Composer only work when there is a parent site mount");
            }
            return new ComponentWrapper(getRequestConfigNode(requestContext));

        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Failed to retrieve parameters.", e);
            } else {
                log.warn("Failed to retrieve parameters . {}", e.toString());
            }
        }
        return null;
    }

    @POST
    @Path("/parameters/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setNode(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse,
                            MultivaluedMap<String, String> params) {
        Response r = null;

        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);
            Session session = requestContext.getSession();
            Node jcrNode = getRequestConfigNode(requestContext);

            Map<String, String> hstParameters = new HashMap<String, String>();
            if (jcrNode.hasProperty(HST_PARAMETERNAMES) && jcrNode.hasProperty(HST_PARAMETERVALUES)) {
                hstParameters = new HashMap<String, String>();
                Value[] paramNames = jcrNode.getProperty(HST_PARAMETERNAMES).getValues();
                Value[] paramValues = jcrNode.getProperty(HST_PARAMETERVALUES).getValues();
                for (int i = 0; i < paramNames.length; i++) {
                    hstParameters.put(paramNames[i].getString(), paramValues[i].getString());
                }
            }
            for (String param : params.keySet()) {
                hstParameters.put(param, params.getFirst(param));
            }

            String[] values = new String[hstParameters.size()];
            jcrNode.setProperty(HST_PARAMETERNAMES, hstParameters.keySet().toArray(values));
            jcrNode.setProperty(HST_PARAMETERVALUES, hstParameters.values().toArray(values));
            jcrNode.save();
        } catch (RepositoryException e) {
            e.printStackTrace();  //TODO fix me
            throw new WebApplicationException(e);
        }
        return r;
    }
}
