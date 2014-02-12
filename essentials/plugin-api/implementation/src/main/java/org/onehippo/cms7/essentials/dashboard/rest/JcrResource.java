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

package org.onehippo.cms7.essentials.dashboard.rest;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.onehippo.cms7.essentials.dashboard.model.RestfulNode;
import org.onehippo.cms7.essentials.dashboard.model.RestfulProperty;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/jcrresource/")
public class JcrResource extends BaseResource {

    private static final Logger log = LoggerFactory.getLogger(JcrResource.class);

    @POST
    @Path("/")
    public RestfulNode getNode(final RestfulNode payload, @Context ServletContext servletContext) throws RepositoryException {
        final Session session = GlobalUtils.createSession();
        try {
            String path = payload.getPath();
            if (Strings.isNullOrEmpty(path)) {
                log.error("Path was null or empty, using root path [/]");
                path = "/";
            }
            // TODO abs path
            final Node n = session.getNode(path);
            final RestfulNode restfulNode = new RestfulNode(n.getName(), n.getPath());
            populateProperties(n, restfulNode);
            //############################################
            // LOAD KIDS (lazy)
            //############################################
            populateNodes(n, restfulNode, true);

            //############################################
            //
            //############################################
            return restfulNode;

        } finally {

            GlobalUtils.cleanupSession(session);
        }

    }

    private void populateNodes(final Node node, final RestfulNode restfulNode, boolean anotherLevel) throws RepositoryException {
        final NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            final Node kid = nodes.nextNode();
            final RestfulNode jcrKid = new RestfulNode(kid.getName(), kid.getPath());
            restfulNode.addNode(jcrKid);
            populateProperties(kid, jcrKid);
            if (anotherLevel) {
                populateNodes(kid, jcrKid, false);
            }


        }
    }


    private void populateProperties(final Node node, final RestfulNode restfulNode) throws RepositoryException {
        final PropertyIterator properties = node.getProperties();
        while (properties.hasNext()) {
            final Property p = properties.nextProperty();
            final RestfulProperty<String> restfulProperty = new RestfulProperty<>();
            restfulProperty.setName(p.getName());
            restfulProperty.setValue(p.getPath());
            restfulNode.addProperty(restfulProperty);
        }
    }

    @POST
    @Path("/property/")
    public RestfulProperty<?> getProperty(final RestfulProperty<?> payload, @Context ServletContext servletContext) {

        return new RestfulProperty<>();
    }
}
