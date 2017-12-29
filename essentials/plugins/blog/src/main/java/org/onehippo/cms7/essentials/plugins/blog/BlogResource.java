/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugins.blog;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.onehippo.cms7.essentials.dashboard.model.UserFeedback;
import org.onehippo.cms7.essentials.dashboard.service.JcrService;
import org.onehippo.cms7.essentials.plugins.blog.model.BlogDaemonModuleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST endpoint to configure the Blog Importer
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON})
@Path("blog")
public class BlogResource {

    private static final Logger LOG = LoggerFactory.getLogger(BlogResource.class);
    private static final String CONFIG_EVENT_BUS = "/hippo:configuration/hippo:modules/essentials-eventbus-listener/hippo:moduleconfig";

    @Inject private JcrService jcrService;

    @POST
    @Path("/")
    public UserFeedback configure(BlogDaemonModuleConfiguration configuration, @Context HttpServletResponse response) {
        final Session session = jcrService.createSession();
        if (session == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return new UserFeedback().addError("Failed to configure blog importer: Consult back-end logs for more info.");
        }

        try {
            if (!session.nodeExists(CONFIG_EVENT_BUS)) {
                response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
                return new UserFeedback().addError("Failed to configure blog importer: BlogListenerModule is not installed.");
            }

            final Node eventBusNode = session.getNode(CONFIG_EVENT_BUS);
            eventBusNode.setProperty("active", configuration.isActive());
            eventBusNode.setProperty("runInstantly", configuration.isRunInstantly());
            eventBusNode.setProperty("blogsBasePath", configuration.getBlogsBasePath());
            eventBusNode.setProperty("authorsBasePath", configuration.getAuthorsBasePath());
            eventBusNode.setProperty("projectNamespace", configuration.getProjectNamespace());
            eventBusNode.setProperty("cronExpression", configuration.getCronExpression());
            eventBusNode.setProperty("cronExpressionDescription", configuration.getCronExpressionDescription());
            eventBusNode.setProperty("maxDescriptionLength", (long) configuration.getMaxDescriptionLength());

            final String[] urls = configuration.getUrls().stream().map(BlogDaemonModuleConfiguration.URL::getValue).toArray(String[]::new);
            final String[] authors = configuration.getUrls().stream().map(BlogDaemonModuleConfiguration.URL::getAuthor).toArray(String[]::new);
            eventBusNode.setProperty("urls", urls);
            eventBusNode.setProperty("authors", authors);

            session.save();
        } catch (RepositoryException e) {
            LOG.error("Failed to configure blog importer.", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return new UserFeedback().addError("Failed to configure blog importer: Consult back-end logs for more info.");
        } finally {
            jcrService.destroySession(session);
        }

        return new UserFeedback().addSuccess("Successfully configured blog importer.");
    }
}
