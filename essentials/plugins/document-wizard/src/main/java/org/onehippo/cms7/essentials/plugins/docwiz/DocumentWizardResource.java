/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Map;

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

import com.google.common.base.Strings;

import org.onehippo.cms7.essentials.dashboard.model.UserFeedback;
import org.onehippo.cms7.essentials.dashboard.rest.PostPayloadRestful;
import org.onehippo.cms7.essentials.dashboard.service.JcrService;
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
    public UserFeedback addWizard(final PostPayloadRestful payloadRestful, @Context HttpServletResponse response) {
        final Map<String, String> values = payloadRestful.getValues();
        final String shortcutName = values.get("shortcutName");
        final String classificationType = values.get("classificationType");
        final String documentType = values.get("documentType");
        final String baseFolder = values.get("baseFolder");
        final String valueListPath = values.get("valueListPath");
        final String query = values.get("documentQuery");
        if (Strings.isNullOrEmpty(shortcutName)) {
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
            final Node node = root.addNode(shortcutName, "frontend:plugin");
            node.setProperty("browser.id", "service.browse");
            node.setProperty("wicket.id", "shortcut");
            node.setProperty("workaround", "4");
            node.setProperty("plugin.class", "org.onehippo.forge.dashboard.documentwizard.NewDocumentWizardPlugin");
            node.setProperty("baseFolder", baseFolder);
            node.setProperty("query", query);
            node.setProperty("documentType", documentType);
            node.setProperty("classificationType", classificationType);
            if (classificationType.equals("list")) {
                node.setProperty("valueListPath", valueListPath);
            }
            // add translation node:
            final Node translationNode = node.addNode("en", "frontend:pluginconfig");
            translationNode.setProperty("shortcut-link-label", values.get("shortcutLinkLabel"));
            translationNode.setProperty("name-label", values.get("nameLabel"));
            if (classificationType.equals("list")) {
                translationNode.setProperty("list-label", values.get("listLabel"));
            } else {
                translationNode.setProperty("date-label", values.get("dateLabel"));
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
