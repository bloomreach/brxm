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
package org.hippoecm.hst.pagecomposer.rest;

import java.util.HashMap;
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
import javax.xml.ws.Response;

import org.hippoecm.hst.services.support.jaxrs.content.BaseHstContentService;

/**
 * PropertiesService provides the JAX-RS service to GET and POST HST Component Properties
 */
@Path("/PropertiesService/")
public class PropertiesService extends BaseHstContentService {
    private static final String HST_PARAMETERVALUES = "hst:parametervalues";
    private static final String HST_PARAMETERNAMES = "hst:parameternames";

    public PropertiesService() {
        super();
    }

    /**
     * Responds to /component_path URI and sends JSON representation of the specified component at path.
     * The path is relative to hst:configuration/hst:configuration
     *
     * @param servletRequest  Injected by the context
     * @param servletResponse Injected by the context
     * @param path            The relative path of the node  (from hst:configuration/hst:configuration) for which the JSON is needed.
     * @return JSON Representation of the node specified by path.
     */
    @GET
    @Path("/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public ComponentWrapper getNode(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @PathParam("path") String path) {
        try {
            Node jcrNode = getJcrSession(servletRequest).getRootNode().getNode(path);
            return new ComponentWrapper(jcrNode);
        } catch (RepositoryException e) {
            e.printStackTrace(); //TODO fix me
            throw new WebApplicationException(e);
        } catch (Exception e) {
            e.printStackTrace(); //TODO fix me
            throw new WebApplicationException(e);
        }
    }

    @POST
    @Path("/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setNode(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse, @PathParam("path") String path, MultivaluedMap<String, String> params) {
        Response r = null;

        try {
            Node jcrNode = getJcrSession(servletRequest).getRootNode().getNode(path);

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
