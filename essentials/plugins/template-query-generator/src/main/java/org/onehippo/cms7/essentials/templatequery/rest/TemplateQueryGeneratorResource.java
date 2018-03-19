/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.templatequery.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.hippoecm.repository.HippoStdNodeType;
import org.onehippo.cms7.essentials.sdk.api.model.rest.ContentType;
import org.onehippo.cms7.essentials.sdk.api.model.rest.UserFeedback;
import org.onehippo.cms7.essentials.sdk.api.service.ContentTypeService;
import org.onehippo.cms7.essentials.sdk.api.service.TemplateQueryService;

import static org.onehippo.cms7.essentials.templatequery.rest.TemplateQueryData.Scope.DOCUMENT;
import static org.onehippo.cms7.essentials.templatequery.rest.TemplateQueryData.Scope.FOLDER;


/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("templatequerygenerator/")
public class TemplateQueryGeneratorResource {

    private final TemplateQueryService templateQueryService;
    private final ContentTypeService contentTypeService;

    @Inject
    public TemplateQueryGeneratorResource(final TemplateQueryService templateQueryService, final ContentTypeService contentTypeService) {
        this.templateQueryService = templateQueryService;
        this.contentTypeService = contentTypeService;
    }

    @POST
    public UserFeedback createTemplateQuery(final TemplateQueryData data) throws Exception {
        final UserFeedback feedback = new UserFeedback();
        final List<TemplateQueryData.Scope> scopes = data.getScopes();
        if (scopes == null || scopes.isEmpty()) {
            return feedback.addError("No scope(s) provided");
        }

        final List<ContentType> contentTypes = data.getContentTypes();
        if (contentTypes == null || contentTypes.isEmpty()) {
            return feedback.addError("No content-type(s) provided");
        }

        for (final ContentType contentType : contentTypes) {
            final String jcrDocumentType = contentType.getFullName();
            if (scopes.contains(DOCUMENT)) {
                generateDocumentTemplateQuery(feedback, jcrDocumentType);
            }
            if (scopes.contains(FOLDER)) {
                generateFolderTemplateQuery(feedback, jcrDocumentType);
            }
        }
        return feedback;
    }

    private void generateFolderTemplateQuery(final UserFeedback feedback, final String jcrDocumentType) {
        if (templateQueryService.createFolderTemplateQuery(jcrDocumentType)) {
            feedback.addSuccess("Folder template query created for " + jcrDocumentType + ".");
        } else {
            feedback.addError("Failed to generate folder template query for " + jcrDocumentType + ".");
        }
    }

    private void generateDocumentTemplateQuery(final UserFeedback feedback, final String jcrDocumentType) {
        if (templateQueryService.createDocumentTypeTemplateQuery(jcrDocumentType)) {
            feedback.addSuccess("Document template query created for " + jcrDocumentType + ".");
        } else {
            feedback.addError("Failed to generate document template query for " + jcrDocumentType + ".");
        }
    }

    @GET
    @Path("/templatequeries")
    public List<TemplateQuery> getTemplateQueries() throws Exception {
        return contentTypeService.fetchContentTypesFromOwnNamespace().stream()
                .filter(this::isRelaxed)
                .filter(contentType -> !contentType.isCompoundType())
                .map(this::createTemplateQuery)
                .collect(Collectors.toList());
    }

    private TemplateQuery createTemplateQuery(final ContentType contentType) {
        final TemplateQuery templateQuery = new TemplateQuery();
        templateQuery.setContentType(contentType);

        final String jcrDocumentType = contentType.getFullName();
        templateQuery.setDocumentQueryExists(templateQueryService.documentTypeTemplateQueryExists(jcrDocumentType));
        templateQuery.setFolderQueryExists(templateQueryService.folderTemplateQueryExists(jcrDocumentType));

        return templateQuery;
    }

    private boolean isRelaxed(final ContentType contentType) {
        return contentType.getSuperTypes() != null
                && contentType.getSuperTypes().contains(HippoStdNodeType.NT_RELAXED);
    }
}
