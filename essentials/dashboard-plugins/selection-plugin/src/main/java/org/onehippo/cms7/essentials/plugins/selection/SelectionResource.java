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

package org.onehippo.cms7.essentials.plugins.selection;

import java.text.MessageFormat;
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

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.rest.BaseResource;
import org.onehippo.cms7.essentials.dashboard.rest.ErrorMessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.PostPayloadRestful;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("selectionplugin")
public class SelectionResource extends BaseResource {

    private static Logger log = LoggerFactory.getLogger(SelectionResource.class);

    @POST
    @Path("/")
    public MessageRestful tickle(final PostPayloadRestful payloadRestful, @Context ServletContext servletContext) {
        // just to make sure the front-end - back-end connection is working...
        log.error("tickling...");
        return null;
    }

    @POST
    @Path("/addfield")
    public MessageRestful addField(final PostPayloadRestful payloadRestful, @Context ServletContext servletContext) {
        final Map<String, String> values = payloadRestful.getValues();
        final String docTypeBase = MessageFormat.format("/hippo:namespaces/{0}/{1}/",
                values.get("namespace"), values.get("documentType"));
        final String documentType = values.get("namespace") + ":" + values.get("documentType");
        final PluginContext context = getContext(servletContext);
        final Session session = context.createSession();

        Node editorTemplate;
        try {
            editorTemplate = session.getNode(docTypeBase + "editor:templates/_default_");
        } catch (RepositoryException e) {
            log.warn("Error trying to retrieve editor template node.", e);
            return new ErrorMessageRestful("Failed to update document type '" + documentType + "'. Check logs.");
        }

        Node nodeType;
        try {
            final Node nodeTypeHandle = session.getNode(docTypeBase + "hipposysedit:nodetype");
            if (nodeTypeHandle.getNodes().getSize() > 1) {
                return new ErrorMessageRestful("Document type '" + documentType + "' is currently being edited in the CMS, "
                        + "please commit any pending changes before adding a selection field.");
            }
            nodeType = nodeTypeHandle.getNode("hipposysedit:nodetype");
        } catch (RepositoryException e) {
            log.warn("Error trying to retrieve nodetype node.", e);
            return new ErrorMessageRestful("Failed to update document type '" + documentType + "'. Check logs.");
        }

        // provide XML templates, load them ("streamAsText"), replace them using mustache and xmlImport them on the session.

// from XmlInstruction:
//        InputStream stream = getClass().getClassLoader().getResourceAsStream(source);
//        final String myData = TemplateUtils.replaceTemplateData(GlobalUtils.readStreamAsText(stream), context.getPlaceholderData());
//        session.importXML(destination.getPath(), IOUtils.toInputStream(myData), ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
//        session.save();

        final String successMessage = MessageFormat.format("Successfully added new selection field {0} to document type {1}.",
                values.get("fieldName"), documentType);
        return new MessageRestful(successMessage);
    }
}