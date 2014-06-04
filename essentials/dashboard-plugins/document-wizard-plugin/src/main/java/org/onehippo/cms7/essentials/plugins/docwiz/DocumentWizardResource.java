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

package org.onehippo.cms7.essentials.plugins.docwiz;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.wicket.util.string.Strings;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.rest.BaseResource;
import org.onehippo.cms7.essentials.dashboard.rest.ErrorMessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.PostPayloadRestful;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("documentwizard")
public class DocumentWizardResource extends BaseResource {

    private static Logger log = LoggerFactory.getLogger(DocumentWizardResource.class);

    public static final String ROOT_CONFIG_PATH = "/hippo:configuration/hippo:frontend/cms/cms-dashshortcuts";


    @POST
    @Path("/")
    public MessageRestful addWizard(final PostPayloadRestful payloadRestful, @Context ServletContext servletContext) {
        final PluginContext context = getContext(servletContext);
        final Session session = context.createSession();
        try {
            final Node root = session.getNode(ROOT_CONFIG_PATH);
            final Map<String, String> values = payloadRestful.getValues();
            final String shortcutName = values.get("shortcutName");
            final String classificationType = values.get("classificationType");
            final String documentType = values.get("documentType");
            final String baseFolder = values.get("baseFolder");

            final String query = values.get("query");
            if (Strings.isEmpty(shortcutName)) {
                return new ErrorMessageRestful("Shortcut name was empty/invalid");
            }
            if (root.hasNode(shortcutName)) {
                return new ErrorMessageRestful("Shortcut name was already configured: " + shortcutName);
            }
            final Node node = root.addNode(shortcutName, "frontend:plugin");
            node.setProperty("browser.id", "service.browse");
            node.setProperty("wicket.id", "shortcut");
            node.setProperty("workaround", "4");
            node.setProperty("plugin.class", "org.onehippo.forge.dashboard.documentwizard.NewDocumentWizardPlugin");
            node.setProperty("baseFolder", baseFolder);
            node.setProperty("documentType", documentType);
            node.setProperty("classificationType", classificationType);
            session.save();
            return new MessageRestful("Successfully created Document Wizard shortcut: " + shortcutName);
        } catch (RepositoryException e) {
            log.error("Error configuring document wizard shortcuts", e);
        } finally {
            GlobalUtils.cleanupSession(session);
        }
        return new MessageRestful("setup");

    }

}
