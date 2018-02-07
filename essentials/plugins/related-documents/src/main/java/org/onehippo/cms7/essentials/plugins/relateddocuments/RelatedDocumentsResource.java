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

package org.onehippo.cms7.essentials.plugins.relateddocuments;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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

import org.onehippo.cms7.essentials.sdk.api.model.rest.UserFeedback;
import org.onehippo.cms7.essentials.sdk.api.service.ContentTypeService;
import org.onehippo.cms7.essentials.sdk.api.service.JcrService;
import org.onehippo.cms7.essentials.plugins.relateddocuments.model.Configuration;
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

    private final JcrService jcrService;
    private final ContentTypeService contentTypeService;

    @Inject
    public RelatedDocumentsResource(final JcrService jcrService, final ContentTypeService contentTypeService) {
        this.jcrService = jcrService;
        this.contentTypeService = contentTypeService;
    }

    @POST
    @Path("/")
    public UserFeedback addDocuments(final Configuration configuration, @Context HttpServletResponse response) {
        final Collection<String> changedDocuments = new HashSet<>();
        final UserFeedback feedback = new UserFeedback();
        final Session session = jcrService.createSession();
        try {
            for (Configuration.Field field : configuration.getFields()) {
                final String jcrContentType = field.getJcrContentType();
                final String basePath = contentTypeService.jcrBasePathForContentType(jcrContentType);
                final String fieldImportPath = basePath + "/editor:templates/_default_";
                final String suggestFieldPath = fieldImportPath + "/relateddocs";
                if (session.nodeExists(suggestFieldPath)) {
                    log.info("Suggest field path: [{}] already exists.", fieldImportPath);
                    continue;
                }
                if (!contentTypeService.addMixinToContentType(jcrContentType, MIXIN_NAME, session, true)) {
                    feedback.addError("Failed to add related documents field for type '" + jcrContentType + "'.");
                    continue;
                }
                // add place holders:
                final Map<String, Object> templateData = new HashMap<>();
                templateData.put("fieldLocation", contentTypeService.determineDefaultFieldPosition(jcrContentType));
                templateData.put("searchPaths", field.getSearchPath());
                templateData.put("numberOfSuggestions", field.getNrOfSuggestions());

                final Node targetNode = session.getNode(fieldImportPath);
                jcrService.importResource(targetNode, "/related_documents_template.xml", templateData);
                jcrService.importResource(targetNode, "/related_documents_suggestion_template.xml", templateData);
                session.save();
                changedDocuments.add(jcrContentType);
            }
        } catch (RepositoryException e) {
            log.error("Error adding related documents field", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return feedback.addError("Failed to add related documents field: " + e.getMessage());
        } finally {
            jcrService.destroySession(session);
        }

        if (changedDocuments.size() > 0) {
            final String docTypeList = changedDocuments.stream().collect(Collectors.joining(", "));
            return feedback.addSuccess("Added related document fields to following documents: " + docTypeList);
        }
        return feedback.addSuccess("No related document fields were added");
    }
}
