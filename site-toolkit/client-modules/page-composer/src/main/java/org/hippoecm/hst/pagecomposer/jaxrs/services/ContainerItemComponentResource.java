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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

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

import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ComponentWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The REST resource handler for the nodes that are of the type "hst:containeritemcomponent". This is specified using the @Path annotation.
 *
 * @version $Id$
 */

@Path("/hst:containeritemcomponent/")
public class ContainerItemComponentResource extends AbstractConfigResource {
    private static Logger log = LoggerFactory.getLogger(ContainerItemComponentResource.class);
    private static final String HST_PARAMETERVALUES = "hst:parametervalues";
    private static final String HST_PARAMETERNAMES = "hst:parameternames";
    private static final String HST_PARAMETERNAMEPREFIXES = "hst:parameternameprefixes";


    @GET
    @Path("/parameters/{locale}/{prefix}")
    @Produces(MediaType.APPLICATION_JSON)
    public ComponentWrapper getParameters(@Context HttpServletRequest servletRequest,
                                          @Context HttpServletResponse servletResponse, 
                                          @PathParam("locale") String localeString,
                                          @PathParam("prefix") String prefix) {

        try {
            
            if (prefix == null || prefix.equals("default")) {
                prefix = "";
            }
            
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
            return new ComponentWrapper(getRequestConfigNode(requestContext), locale, prefix);
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
    @Path("/parameters/{prefix}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setNode(@Context HttpServletRequest servletRequest, 
                            @Context HttpServletResponse servletResponse,
                            @PathParam("prefix") String prefix,
                            MultivaluedMap<String, String> params) {
        try {
            
            if (prefix == null || prefix.isEmpty()) {
                prefix = "default";
            }
            
            HstRequestContext requestContext = getRequestContext(servletRequest);
            Node jcrNode = getRequestConfigNode(requestContext);

            Map<String, Map<String, String>> hstParameters = new HashMap<String, Map<String, String>>();
            if (jcrNode.hasProperty(HST_PARAMETERNAMES) && jcrNode.hasProperty(HST_PARAMETERVALUES)) {
                if (jcrNode.hasProperty(HST_PARAMETERNAMEPREFIXES)) {
                    Value[] paramPrefixes = jcrNode.getProperty(HST_PARAMETERNAMEPREFIXES).getValues();
                    Value[] paramNames = jcrNode.getProperty(HST_PARAMETERNAMES).getValues();
                    Value[] paramValues = jcrNode.getProperty(HST_PARAMETERVALUES).getValues();
                    if (!(paramPrefixes.length == paramNames.length && paramPrefixes.length == paramValues.length)) {
                        // TODO: log warning
                        return error(HST_PARAMETERNAMEPREFIXES + ", " + HST_PARAMETERNAMES + " and " 
                                + HST_PARAMETERVALUES + " properties do not have the same number of values");
                    }
                    for (int i = 0; i < paramNames.length; i++) {
                        String paramPrefix = paramPrefixes[i].getString().isEmpty() ? "default" : paramPrefixes[i].getString();
                        Map<String, String> prefixedParameters = hstParameters.get(paramPrefix);
                        if (prefixedParameters == null) {
                            prefixedParameters = new HashMap<String, String>();
                            hstParameters.put(paramPrefix, prefixedParameters);
                        }
                        prefixedParameters.put(paramNames[i].getString(), paramValues[i].getString());
                    }
                } else {
                    Map<String, String> parameters = new HashMap<String, String>();
                    Value[] paramNames = jcrNode.getProperty(HST_PARAMETERNAMES).getValues();
                    Value[] paramValues = jcrNode.getProperty(HST_PARAMETERVALUES).getValues();
                    if (paramNames.length != paramValues.length) {
                        // TODO: log warning
                        return error(HST_PARAMETERNAMES + " and " + HST_PARAMETERVALUES 
                                + " properties do not have the same number of values");
                    }
                    for (int i = 0; i < paramNames.length; i++) {
                        parameters.put(paramNames[i].getString(), paramValues[i].getString());
                    }
                    hstParameters.put("default", parameters);
                }
            }
            
            Map<String, String> prefixedParameters = hstParameters.get(prefix);
            if (prefixedParameters == null) {
                prefixedParameters = new HashMap<String, String>();
                hstParameters.put(prefix, prefixedParameters);
            }
            for (String param : params.keySet()) {
                // the FORCE_CLIENT_HOST is some 'magic' parameter we do not need to store
                // this check can be removed once in all code, the FORCE_CLIENT_HOST parameter from the queryString
                // has been replaced by a request header.
                if(!"FORCE_CLIENT_HOST".equals(param)) {
                   prefixedParameters.put(param, params.getFirst(param));
                } 
            }

            List<String> prefixes = new ArrayList<String>();
            List<String> names = new ArrayList<String>();
            List<String> values = new ArrayList<String>();
            for (Entry<String, Map<String, String>> e : hstParameters.entrySet()) {
                String paramPrefix = e.getKey();
                for (Entry<String, String> f : e.getValue().entrySet()) {
                    prefixes.add(paramPrefix.equals("default") ? "" : paramPrefix);
                    names.add(f.getKey());
                    values.add(f.getValue());
                } 
            }
            jcrNode.setProperty(HST_PARAMETERNAMEPREFIXES, prefixes.toArray(new String[prefixes.size()]));
            jcrNode.setProperty(HST_PARAMETERNAMES, names.toArray(new String[names.size()]));
            jcrNode.setProperty(HST_PARAMETERVALUES, values.toArray(new String[values.size()]));
            jcrNode.getSession().save();
        } catch (RepositoryException e) {
            log.error("Unable to get the parameters of the component " + e, e);
            throw new WebApplicationException(e);
        }

        return ok("Properties saved successfully, please refresh to see the changes.", null);
    }
}
