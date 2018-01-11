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

package org.onehippo.cms7.essentials.plugins.tagging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.inject.Inject;
import javax.jcr.ImportUUIDBehavior;
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

import org.apache.commons.io.IOUtils;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContextFactory;
import org.onehippo.cms7.essentials.dashboard.model.UserFeedback;
import org.onehippo.cms7.essentials.dashboard.rest.PostPayloadRestful;
import org.onehippo.cms7.essentials.dashboard.service.ContentTypeService;
import org.onehippo.cms7.essentials.dashboard.service.JcrService;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.PayloadUtils;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;
import org.onehippo.cms7.essentials.dashboard.utils.TranslationsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("taggingplugin")
public class TaggingResource {

    private static final Logger log = LoggerFactory.getLogger(TaggingResource.class);
    private static final String MIXIN_NAME = "hippostd:taggable";
    private static final String TAGS_FIELD = "tags";
    private static final String TAGSUGGEST_FIELD = "tagsuggest";

    @Inject private JcrService jcrService;
    @Inject private ContentTypeService contentTypeService;
    @Inject private PluginContextFactory contextFactory;

    @POST
    @Path("/")
    public UserFeedback addDocuments(final PostPayloadRestful payloadRestful, @Context HttpServletResponse response) {
        final Collection<String> addedDocuments = new HashSet<>();
        final PluginContext context = contextFactory.getContext();
        final Session session = jcrService.createSession();
        try {

            final Map<String, String> values = payloadRestful.getValues();
            final String documents = values.get("documents");
            // NOTE below fields have same name within data payload
            //final String widgetRows = values.get("widgetRows");
            //final String widgetCols = values.get("widgetCols");
            //final String numberOfSuggestions = values.get("numberOfSuggestions");

            final String prefix = context.getProjectNamespacePrefix();

            final String templateTags = GlobalUtils.readStreamAsText(getClass().getResourceAsStream("/tagging-template-field_tags.xml"));
            final String templateSuggest = GlobalUtils.readStreamAsText(getClass().getResourceAsStream("/tagging-template-field_tag_suggest.xml"));
            final String templateTranslations = GlobalUtils.readStreamAsText(getClass().getResourceAsStream("/taggingtypes-translations.json"));

            if (Strings.isNullOrEmpty(documents)) {
                response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
                return new UserFeedback().addError("No documents were selected");
            }

            final String[] docs = PayloadUtils.extractValueArray(values.get("documents"));

            for (final String document : docs) {
                final String fieldImportPath = MessageFormat.format("/hippo:namespaces/{0}/{1}/editor:templates/_default_", prefix, document);
                final String suggestFieldPath = MessageFormat.format("{0}/" + TAGSUGGEST_FIELD, fieldImportPath);
                final String jcrContentType = prefix + ":" + document;
                if (session.nodeExists(suggestFieldPath)) {
                    log.info("Suggest field path: [{}] already exists.", suggestFieldPath);
                    continue;
                }

                contentTypeService.addMixinToContentType(jcrContentType, MIXIN_NAME, true);
                // add place holders:
                final Map<String, Object> templateData = new HashMap<>(values);
                final Node editorTemplate = session.getNode(fieldImportPath);
                templateData.put("fieldLocation", contentTypeService.determineDefaultFieldPosition(jcrContentType));
                templateData.put("prefix", prefix);
                templateData.put("document", document);
                // import field:
                final String tagsPath = fieldImportPath + '/' + TAGS_FIELD;
                if (!session.nodeExists(tagsPath)) {
                    final String fieldData = TemplateUtils.replaceTemplateData(templateTags, templateData);
                    session.importXML(fieldImportPath, IOUtils.toInputStream(fieldData, StandardCharsets.UTF_8), ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
                }

                // import suggest field:
                final String suggestPath = fieldImportPath + '/' + TAGSUGGEST_FIELD;
                if (!session.nodeExists(suggestPath)) {
                    final String suggestData = TemplateUtils.replaceTemplateData(templateSuggest, templateData);
                    session.importXML(fieldImportPath, IOUtils.toInputStream(suggestData, StandardCharsets.UTF_8), ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
                }

                // import field translations
                if (session.nodeExists("/hippo:configuration/hippo:translations")) {
                    final String json = TemplateUtils.replaceTemplateData(templateTranslations, templateData);
                    TranslationsUtils.importTranslations(json, session);
                }

                addedDocuments.add(document);
                session.save();
            }
        } catch (RepositoryException | IOException e) {
            log.error("Error adding tagging documents field", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return new UserFeedback().addError("Error adding tagging fields: " + e.getMessage());
        } finally {
            jcrService.destroySession(session);
        }

        final String message = addedDocuments.isEmpty()
                ? "No tagging was added to selected documents."
                : "Successfully added tagging to following document: " + Joiner.on(",").join(addedDocuments);
        return new UserFeedback().addSuccess(message);
    }
}
