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

package org.onehippo.cms7.essentials.plugins.tagging;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import org.onehippo.cms7.essentials.dashboard.service.ContentTypeService;
import org.onehippo.cms7.essentials.dashboard.service.JcrService;
import org.onehippo.cms7.essentials.plugins.tagging.model.Configuration;
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

    @POST
    @Path("/")
    public UserFeedback addDocuments(final Configuration configuration, @Context HttpServletResponse response) {
        final Collection<String> addedDocuments = new HashSet<>();
        final Session session = jcrService.createSession();
        try {
            final List<String> jcrContentTypes = configuration.getJcrContentTypes();
            if (jcrContentTypes == null || jcrContentTypes.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
                return new UserFeedback().addError("No documents were selected");
            }

            for (final String jcrContentType : jcrContentTypes) {
                final String prefix = contentTypeService.extractPrefix(jcrContentType);
                final String shortName = contentTypeService.extractShortName(jcrContentType);
                final String fieldImportPath = contentTypeService.jcrBasePathForContentType(jcrContentType) + "/editor:templates/_default_";
                final Node editorTemplate = session.getNode(fieldImportPath);
                final String suggestFieldPath = fieldImportPath + "/" + TAGSUGGEST_FIELD;
                if (editorTemplate.hasNode(TAGSUGGEST_FIELD)) {
                    log.info("Suggest field path: [{}] already exists.", suggestFieldPath);
                    continue;
                }

                contentTypeService.addMixinToContentType(jcrContentType, MIXIN_NAME, true);
                // add place holders:
                final Map<String, Object> templateData = new HashMap<>(configuration.getParameters());
                templateData.put("fieldLocation", contentTypeService.determineDefaultFieldPosition(jcrContentType));
                templateData.put("prefix", prefix);
                templateData.put("document", shortName);
                // import field:
                if (!editorTemplate.hasNode(TAGS_FIELD)) {
                    jcrService.importResource(editorTemplate, "/tagging-template-field_tags.xml", templateData);
                }

                // import suggest field:
                if (!editorTemplate.hasNode(TAGSUGGEST_FIELD)) {
                    jcrService.importResource(editorTemplate, "/tagging-template-field_tag_suggest.xml", templateData);
                }

                // import field translations
                jcrService.importTranslationsResource(session, "/taggingtypes-translations.json", templateData);

                addedDocuments.add(jcrContentType);
                session.save();
            }
        } catch (RepositoryException e) {
            log.error("Error adding tagging documents field", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return new UserFeedback().addError("Error adding tagging fields: " + e.getMessage());
        } finally {
            jcrService.destroySession(session);
        }

        final String message = addedDocuments.isEmpty()
                ? "No tagging was added to selected documents."
                : "Successfully added tagging to following document: " + addedDocuments.stream().collect(Collectors.joining(", "));
        return new UserFeedback().addSuccess(message);
    }
}
