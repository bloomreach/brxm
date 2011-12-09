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
import java.util.Locale;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
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
    @Path("/parameters/{locale}")
    @Produces(MediaType.APPLICATION_JSON)
    public ComponentWrapper getParameters(@Context HttpServletRequest servletRequest,
                                          @Context HttpServletResponse servletResponse, @PathParam("locale") String localeString) {

        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);
            Locale locale = null;
            if (localeString != null) {
                String[] localeParts = localeString.split("_");
                if (localeParts.length == 3) {
                    locale = new Locale(localeParts[0], localeParts[1], localeParts[2]);
                } else if (localeParts.length == 2) {
                    locale = new Locale(localeParts[0], localeParts[1]);
                } else if (localeParts.length == 1) {
                    locale = new Locale(localeParts[0]);
                } else {
                    log.warn("Failed to create Locale from string '{}'", localeString);
                }
            }
            return new ComponentWrapper(getRequestConfigNode(requestContext), locale);
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
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);
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
            if(jcrNode.hasProperty(HstNodeTypes.COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES)) {
                log.warn("Setting parameter names / values through the template composer for a hst component that also contains" +
                		" '{}' does not work in the 7.7. The '{}' property will now be removed for " +
                		"component '"+jcrNode.getPath()+"'", HstNodeTypes.COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES, HstNodeTypes.COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES);
                jcrNode.getProperty(HstNodeTypes.COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES).remove();
            }
            for (String param : params.keySet()) {
                // the FORCE_CLIENT_HOST is some 'magic' parameter we do not need to store
                // this check can be removed once in all code, the FORCE_CLIENT_HOST parameter from the queryString
                // has been replaced by a request header.
                if(!param.equals("FORCE_CLIENT_HOST")) {
                   hstParameters.put(param, params.getFirst(param));
                } 
            }

            String[] values = new String[hstParameters.size()];
            jcrNode.setProperty(HST_PARAMETERNAMES, hstParameters.keySet().toArray(values));
            jcrNode.setProperty(HST_PARAMETERVALUES, hstParameters.values().toArray(values));
            jcrNode.getSession().save();
        } catch (RepositoryException e) {
            log.error("Unable to get the parameters of the component " + e, e);
            throw new WebApplicationException(e);
        }

        return ok("Properties saved successfully, please refresh to see the changes.", null);
    }
}
