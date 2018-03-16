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
import org.onehippo.cms7.essentials.plugin.sdk.utils.TemplateQueryUtils;
import org.onehippo.cms7.essentials.sdk.api.model.rest.ContentType;
import org.onehippo.cms7.essentials.sdk.api.model.rest.UserFeedback;
import org.onehippo.cms7.essentials.sdk.api.service.ContentTypeService;
import org.onehippo.cms7.essentials.sdk.api.service.JcrService;


/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("templatequerygenerator/")
public class TemplateQueryGeneratorResource {

    private static final String DOCUMENT_SCOPE = "document";
    private static final String FOLDER_SCOPE = "folder";

    private final JcrService jcrService;
    private final ContentTypeService contentTypeService;

    @Inject
    public TemplateQueryGeneratorResource(final JcrService jcrService, final ContentTypeService contentTypeService) {
        this.jcrService = jcrService;
        this.contentTypeService = contentTypeService;
    }

    @POST
    public UserFeedback createTemplateQuery(final TemplateQueryData data) throws Exception {
        final List<String> scopes = data.getScopes();
        if (scopes == null || scopes.isEmpty()) {
            return UserFeedback.error("No scope(s) provided");
        }

        final List<ContentType> contentTypes = data.getContentTypes();
        if (contentTypes == null || contentTypes.isEmpty()) {
            return UserFeedback.error("No content-type(s) provided");
        }

        final UserFeedback feedback = new UserFeedback();
        for (final ContentType contentType : contentTypes) {
            final String prefix = contentType.getPrefix();
            final String name = contentType.getName();
            if (scopes.contains(DOCUMENT_SCOPE)) {
                generateDocumentTemplateQuery(feedback, prefix, name);
            }
            if (scopes.contains(FOLDER_SCOPE)) {
                generateFolderTemplateQuery(feedback, prefix, name);
            }
        }
        return feedback;
    }

    private void generateFolderTemplateQuery(final UserFeedback feedback, final String prefix, final String name) {
        if (TemplateQueryUtils.createFolderTemplateQuery(jcrService, name)) {
            feedback.addSuccess("Folder template query created for " + prefix + ":" + name);
        } else {
            feedback.addError("Failed to generate folder template query for " + prefix + ":" + name);
        }
    }

    private void generateDocumentTemplateQuery(final UserFeedback feedback, final String prefix, final String name) {
        if (TemplateQueryUtils.createDocumentTemplateQuery(jcrService, prefix, name)) {
            feedback.addSuccess("Document template query created for " + prefix + ":" + name);
        } else {
            feedback.addError("Failed to generate document template query for " + prefix + ":" + name);
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

        final String documentName = contentType.getName();
        templateQuery.setDocumentQueryExists(TemplateQueryUtils.documentQueryExists(jcrService, documentName));
        templateQuery.setFolderQueryExists(TemplateQueryUtils.folderQueryExists(jcrService, documentName));

        return templateQuery;
    }

    private boolean isRelaxed(final ContentType contentType) {
        return contentType.getSuperTypes() != null
                && contentType.getSuperTypes().contains(HippoStdNodeType.NT_RELAXED);
    }
}
