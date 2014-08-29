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

package org.onehippo.cms7.essentials.rest.picker;


import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.onehippo.cms7.essentials.dashboard.rest.BaseResource;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/jcrbrowser")
public class JcrBrowserResource extends BaseResource {

    private static final Logger log = LoggerFactory.getLogger(JcrBrowserResource.class);
    public static final int DEFAULT_DEPTH = 3;

    @GET
    @Path("/")
    public JcrNode getFromRoot(@Context ServletContext servletContext) throws RepositoryException {
        final JcrQuery query = new JcrQuery("/");
        return getNode(query, servletContext);
    }
    @GET
    @Path("/folders")
    public JcrNode getFolders(@Context ServletContext servletContext) throws RepositoryException {
        final JcrQuery query = new JcrQuery("/content");
        query.setFolderPicker(true);
        query.setFetchProperties(false);
        query.setDepth(DEFAULT_DEPTH);
        return getNode(query, servletContext);
    }


    @POST
    @Path("/")
    public JcrNode getNode(final JcrQuery query, @Context ServletContext servletContext) throws RepositoryException {
        final Session session = GlobalUtils.createSession();
        try {
            String path = query.getPath();
            if (Strings.isNullOrEmpty(path)) {
                log.error("Path was null or empty, using root path [/]");
                path = "/";
            }
            // TODO abs path
            final Node n = session.getNode(path);
            final JcrNode jcrNode = new JcrNode(n.getName(), n.getPath());

            populateProperties(n, jcrNode, query);
            populateNodes(n, jcrNode, query);
            return jcrNode;

        } finally {

            GlobalUtils.cleanupSession(session);
        }

    }

    private void populateNodes(final Node node, final JcrNode jcrNode, final JcrQuery query) throws RepositoryException {
        final NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            final Node kid = nodes.nextNode();
            if(query.isFolderPicker()) {
                final String name = kid.getPrimaryNodeType().getName();
                if(!name.equals("hippostd:folder")){
                    continue;
                }
                if(query.isExcluded(kid)){
                    continue;
                }
            }
            final JcrNode jcrKid = new JcrNode(kid.getName(), kid.getPath());
            jcrNode.addNode(jcrKid);
            populateProperties(kid, jcrKid,query);
            if (query.getCurrentDepth() < query.getDepth()) {
                query.incrementDepth();
                populateNodes(kid, jcrKid, query);
            }
        }
    }


    private void populateProperties(final Node node, final JcrNode jcrNode, final JcrQuery query) throws RepositoryException {
        if (query.isFetchProperties()) {
            final PropertyIterator properties = node.getProperties();
            while (properties.hasNext()) {
                final Property p = properties.nextProperty();
                final JcrProperty<String> jcrProperty = new JcrProperty<>();
                jcrProperty.setName(p.getName());
                jcrProperty.setValue(p.getPath());
                jcrNode.addProperty(jcrProperty);
            }
        }
    }

    @POST
    @Path("/property/")
    public JcrProperty<?> getProperty(final JcrProperty<?> payload, @Context ServletContext servletContext) {

        return new JcrProperty<>();
    }
}