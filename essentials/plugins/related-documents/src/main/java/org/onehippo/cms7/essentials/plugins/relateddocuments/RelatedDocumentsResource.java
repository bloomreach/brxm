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

package org.onehippo.cms7.essentials.plugins.relateddocuments;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContextFactory;
import org.onehippo.cms7.essentials.dashboard.model.UserFeedback;
import org.onehippo.cms7.essentials.dashboard.rest.PostPayloadRestful;
import org.onehippo.cms7.essentials.dashboard.service.ContentTypeService;
import org.onehippo.cms7.essentials.dashboard.service.JcrService;
import org.onehippo.cms7.essentials.dashboard.utils.PayloadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("related-documents")
public class RelatedDocumentsResource {

    private static final Logger log = LoggerFactory.getLogger(RelatedDocumentsResource.class);
    private static final String MIXIN_NAME = "relateddocs:relatabledocs";

    @Inject private PluginContextFactory contextFactory;
    @Inject private JcrService jcrService;
    @Inject private ContentTypeService contentTypeService;

    @POST
    @Path("/")
    public UserFeedback addDocuments(final PostPayloadRestful payloadRestful, @Context HttpServletResponse response) {
        final Collection<String> changedDocuments = new HashSet<>();
        final PluginContext context = contextFactory.getContext();
        final Session session = jcrService.createSession();
        try {
            final Map<String, String> values = payloadRestful.getValues();
            final String documents = values.get("documents");
            final String prefix = context.getProjectNamespacePrefix();

            if (!Strings.isNullOrEmpty(documents)) {

                final String[] docs = PayloadUtils.extractValueArray(values.get("documents"));
                final String[] searchPaths = PayloadUtils.extractValueArray(values.get("searchPaths"));
                final String[] suggestions = PayloadUtils.extractValueArray(values.get("numberOfSuggestions"));

                for (int idx = 0; idx < docs.length; idx++) {
                    final String document = docs[idx];
                    final String jcrContentType = prefix + ":" + document;
                    final String fieldImportPath = MessageFormat.format("/hippo:namespaces/{0}/{1}/editor:templates/_default_", prefix, document);
                    final String suggestFieldPath = MessageFormat.format("{0}/relateddocs", fieldImportPath);
                    if (session.nodeExists(suggestFieldPath)) {
                        log.info("Suggest field path: [{}] already exists.", fieldImportPath);
                        continue;
                    }
                    contentTypeService.addMixinToContentType(jcrContentType, MIXIN_NAME, true);
                    // add place holders:
                    final Map<String, Object> templateData = new HashMap<>(values);
                    templateData.put("fieldLocation", contentTypeService.determineDefaultFieldPosition(jcrContentType));
                    templateData.put("searchPaths", searchPaths[idx]);
                    templateData.put("numberOfSuggestions", suggestions[idx]);

                    final Node targetNode = session.getNode(fieldImportPath);
                    jcrService.importResource(targetNode, "/related_documents_template.xml", templateData);
                    jcrService.importResource(targetNode, "/related_documents_suggestion_template.xml", templateData);
                    session.save();
                    changedDocuments.add(document);
                }
            }
        } catch (RepositoryException e) {
            log.error("Error adding related documents field", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return new UserFeedback().addError("Failed to add related documents field: " + e.getMessage());
        } finally {
            jcrService.destroySession(session);
        }

        if (changedDocuments.size() > 0) {
            final String join = Joiner.on(',').join(changedDocuments);
            return new UserFeedback().addSuccess("Added related document fields to following documents: " + join);
        }
        return new UserFeedback().addSuccess("No related document fields were added");
    }
}
