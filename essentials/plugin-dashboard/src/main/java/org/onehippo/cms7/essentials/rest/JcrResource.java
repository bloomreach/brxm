/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.rest;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.onehippo.cms7.essentials.dashboard.rest.NodeRestful;
import org.onehippo.cms7.essentials.dashboard.rest.PropertyRestful;
import org.onehippo.cms7.essentials.dashboard.rest.QueryRestful;
import org.onehippo.cms7.essentials.dashboard.rest.BaseResource;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

/**
 * @version "$Id$"
 */

@Api(value = "/jcr/", description = "Generic API for accessing Hippo repository")
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/jcr/")
public class JcrResource extends BaseResource {

    private static final Logger log = LoggerFactory.getLogger(JcrResource.class);

    @ApiOperation(
            value = "Populated NodeRestful",
            notes = "Retrieves and returns root node",
            response = NodeRestful.class)

    @GET
    @Path("/")
    public NodeRestful getRootNode(@Context ServletContext servletContext) throws RepositoryException {
        return getNode(new NodeRestful("/", "/"), servletContext);
    }

    @ApiOperation(
            value = "Populated NodeRestful",
            notes = "Path is taken from payload object which is also of type NodeRestful",
            response = NodeRestful.class
    )
    @POST
    @Path("/")
    public NodeRestful getNode(final NodeRestful payload, @Context ServletContext servletContext) throws RepositoryException {
        final Session session = GlobalUtils.createSession();
        try {

            String path = payload.getPath();
            if (Strings.isNullOrEmpty(path)) {
                log.error("Path was null or empty, using root path [/]");
                path = "/";
            }
            // TODO abs path
            final Node n = session.getNode(path);
            final NodeRestful nodeRestful = new NodeRestful(n.getName(), n.getPath());
            populateProperties(n, nodeRestful);
            //############################################
            // LOAD KIDS (lazy)
            //############################################
            populateNodes(n, nodeRestful, true);

            //############################################
            //
            //############################################
            return nodeRestful;

        } finally {

            GlobalUtils.cleanupSession(session);
        }

    }

    private void populateNodes(final Node node, final NodeRestful nodeRestful, boolean anotherLevel) throws RepositoryException {
        final NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            final Node kid = nodes.nextNode();
            final NodeRestful jcrKid = new NodeRestful(kid.getName(), kid.getPath());
            nodeRestful.addNode(jcrKid);
            populateProperties(kid, jcrKid);
            if (anotherLevel) {
                populateNodes(kid, jcrKid, false);
            }


        }
    }


    private void populateProperties(final Node node, final NodeRestful nodeRestful) throws RepositoryException {
        final PropertyIterator properties = node.getProperties();
        while (properties.hasNext()) {
            final Property p = properties.nextProperty();
            final PropertyRestful propertyRestful = new PropertyRestful();
            propertyRestful.setName(p.getName());
            propertyRestful.setValue(p.getPath());
            nodeRestful.addProperty(propertyRestful);
        }
    }


    @POST
    @Path("/property/")
    public PropertyRestful getProperty(final PropertyRestful payload, @Context ServletContext servletContext) {

        return new PropertyRestful();
    }


    @POST
    @Path("/query/")
    public NodeRestful executeQuery(final QueryRestful payload, @Context ServletContext servletContext) {

        final NodeRestful restful = new NodeRestful(true);

        Session session = null;
        try {

            session = GlobalUtils.createSession();
            final QueryManager queryManager = session.getWorkspace().getQueryManager();
            final String s = "//element(*, hst:template)[@hst:script]";
            final Query query = queryManager.createQuery(payload.getQuery(), payload.getType());
            // TODO add paging
            final QueryResult result = query.execute();
            final NodeIterator nodes = result.getNodes();

            while (nodes.hasNext()) {
                final Node node = nodes.nextNode();
                final NodeRestful nodeRestful = new NodeRestful(node.getName(), node.getPath());
                populateProperties(node, nodeRestful);
                //############################################
                // LOAD KIDS (lazy)
                //############################################
                populateNodes(node, nodeRestful, true);
                restful.addNode(nodeRestful);
            }
        } catch (RepositoryException e) {
            log.error("Error executing query", e);


        } finally {
            GlobalUtils.cleanupSession(session);
        }


        return restful;
    }


}
