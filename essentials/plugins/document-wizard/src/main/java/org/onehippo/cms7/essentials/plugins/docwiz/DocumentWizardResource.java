/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugins.docwiz;

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

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms7.essentials.sdk.api.rest.UserFeedback;
import org.onehippo.cms7.essentials.sdk.api.service.JcrService;
import org.onehippo.cms7.essentials.plugins.docwiz.model.DocumentWizardConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("documentwizard")
public class DocumentWizardResource {

    private static final Logger log = LoggerFactory.getLogger(DocumentWizardResource.class);
    private static final String ROOT_CONFIG_PATH = "/hippo:configuration/hippo:frontend/cms/cms-dashshortcuts";

    @Inject private JcrService jcrService;

    @POST
    @Path("/")
    public UserFeedback addWizard(DocumentWizardConfiguration configuration, @Context HttpServletResponse response) {
        final String shortcutName = configuration.getShortcutName();
        if (StringUtils.isBlank(shortcutName)) {
            response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
            return new UserFeedback().addError("You must specify a shortcut name.");
        }

        final Session session = jcrService.createSession();
        if (session == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return new UserFeedback().addError("Failed to access JCR repository.");
        }
        try {
            final Node root = session.getNode(ROOT_CONFIG_PATH);
            if (root.hasNode(shortcutName)) {
                response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
                return new UserFeedback().addError("Shortcut name '" + shortcutName + "' already exists.");
            }
            final String classificationType = configuration.getClassificationType();
            final Node node = root.addNode(shortcutName, "frontend:plugin");
            node.setProperty("browser.id", "service.browse");
            node.setProperty("wicket.id", "shortcut");
            node.setProperty("workaround", "4");
            node.setProperty("plugin.class", "org.onehippo.forge.dashboard.documentwizard.NewDocumentWizardPlugin");
            node.setProperty("baseFolder", configuration.getBaseFolder());
            node.setProperty("query", configuration.getDocumentQuery());
            node.setProperty("documentType", configuration.getDocumentType());
            node.setProperty("classificationType", classificationType);

            final Node translationNode = node.addNode("en", "frontend:pluginconfig");
            translationNode.setProperty("shortcut-link-label", configuration.getShortcutLinkLabel());
            translationNode.setProperty("name-label", configuration.getNameLabel());

            if (classificationType.equals("list")) {
                node.setProperty("valueListPath", configuration.getValueListPath());
                translationNode.setProperty("list-label", configuration.getListLabel());
            } else {
                translationNode.setProperty("date-label", configuration.getDateLabel());
            }
            session.save();
        } catch (RepositoryException e) {
            log.error("Error configuring document wizard shortcuts", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return new UserFeedback().addError("Failed to configure document wizard shortcut: " + e.getMessage());
        } finally {
            jcrService.destroySession(session);
        }
        return new UserFeedback().addSuccess("Successfully created Document Wizard shortcut: " + shortcutName);
    }
}
