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

package org.onehippo.cms7.essentials.plugin.clonecomponent;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
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

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.rest.BaseResource;
import org.onehippo.cms7.essentials.dashboard.rest.KeyValueRestful;
import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.PostPayloadRestful;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/cloneComponent/")
public class CloneComponentResource extends BaseResource {

    public static final String COMPONENTS_ROOT = "/hst:hst/hst:configurations/hst:default/hst:catalog/essentials-catalog";

    private static final Logger log = LoggerFactory.getLogger(CloneComponentResource.class);
    public static final String HST_TEMPLATE = "hst:template";

    @GET
    public List<KeyValueRestful> runCloneComponent(@Context ServletContext servletContext) {

        final PluginContext context = getContext(servletContext);
        final Session session = context.createSession();
        List<KeyValueRestful> componentList = new ArrayList<>();
        try {

            if (session.nodeExists(COMPONENTS_ROOT)) {
                final Node node = session.getNode(COMPONENTS_ROOT);
                final NodeIterator nodes = node.getNodes();
                while (nodes.hasNext()) {

                    final Node componentNode = nodes.nextNode();
                    if (componentNode.hasProperty(HST_TEMPLATE)) {
                        componentList.add(new KeyValueRestful(componentNode.getName(), componentNode.getProperty(HST_TEMPLATE).getString()));
                    } else {
                        log.warn("Component node does not have a template: {}", componentNode.getPath());
                    }


                }
            }
        } catch (RepositoryException e) {
            log.error("Error fetching components", e);
        } finally {
            GlobalUtils.cleanupSession(session);
        }
        return componentList;
    }

    @POST
    public MessageRestful cloneComponent(final PostPayloadRestful payload, @Context ServletContext servletContext) {

        final PluginContext context = getContext(servletContext);
        final Session session = context.createSession();
        try {
            if (session.nodeExists(COMPONENTS_ROOT)) {
                session.getNode(COMPONENTS_ROOT);
            }
        } catch (RepositoryException e) {
            log.error("", e);
        } finally {
            GlobalUtils.cleanupSession(session);
        }


        return new MessageRestful("Not implemented yet");
    }


}
