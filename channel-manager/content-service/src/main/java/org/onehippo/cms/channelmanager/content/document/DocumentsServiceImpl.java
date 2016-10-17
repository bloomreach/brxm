/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content.document;

import java.util.Locale;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.util.DocumentUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.WorkflowUtils;
import org.onehippo.cms.channelmanager.content.document.model.Document;
import org.onehippo.cms.channelmanager.content.document.model.DocumentInfo;
import org.onehippo.cms.channelmanager.content.document.model.EditingInfo;
import org.onehippo.cms.channelmanager.content.documenttype.DocumentTypesService;
import org.onehippo.cms.channelmanager.content.documenttype.DocumentTypeNotFoundException;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;
import org.onehippo.cms.channelmanager.content.document.util.EditingUtils;
import org.onehippo.cms.channelmanager.content.MockResponse;

public class DocumentsServiceImpl implements DocumentsService {
    private static final DocumentsService INSTANCE = new DocumentsServiceImpl();

    static DocumentsService getInstance() {
        return INSTANCE;
    }

    private DocumentsServiceImpl() { }

    @Override
    public Document createDraft(final String uuid, final Session session, final Locale locale)
            throws DocumentNotFoundException {
        final Node handle = DocumentUtils.getHandle(uuid, session).orElseThrow(DocumentNotFoundException::new);
        final EditableWorkflow workflow = WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)
                                                       .orElseThrow(DocumentNotFoundException::new);
        final DocumentType docType = getDocumentType(handle, locale);
        final Document document = assembleDocument(uuid, handle, workflow, docType);
        final EditingInfo editingInfo = document.getInfo().getEditingInfo();

        if (editingInfo.getState() == EditingInfo.State.AVAILABLE) {
            final Optional<Node> optionalDraft = EditingUtils.createDraft(workflow, handle);
            if (optionalDraft.isPresent()) {
                loadFields(document, optionalDraft.get(), docType);
            } else {
                editingInfo.setState(EditingInfo.State.UNAVAILABLE);
            }
        }
        return document;
    }

    @Override
    public Document getPublished(final String uuid, final Session session, final Locale locale)
            throws DocumentNotFoundException {
        if ("test".equals(uuid)) {
            return MockResponse.createTestDocument(uuid);
        }

        final Node handle = DocumentUtils.getHandle(uuid, session).orElseThrow(DocumentNotFoundException::new);
        final EditableWorkflow workflow = WorkflowUtils.getWorkflow(handle, "editing", EditableWorkflow.class)
                                                       .orElseThrow(DocumentNotFoundException::new);
        final DocumentType docType = getDocumentType(handle, locale);
        final Document document = assembleDocument(uuid, handle, workflow, docType);

        WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED)
                .ifPresent(unpublished -> loadFields(document, unpublished, docType));
        return document;
    }

    private DocumentType getDocumentType(final Node handle, final Locale locale)
            throws DocumentNotFoundException {
        try {
            return DocumentTypesService.get().getDocumentType(handle, locale);
        } catch (DocumentTypeNotFoundException e) {
            final String handlePath = JcrUtils.getNodePathQuietly(handle);
            throw new DocumentNotFoundException("Failed to retrieve type of document '" + handlePath + "'", e);
        }
    }

    private Document assembleDocument(final String uuid, final Node handle,
                                      final EditableWorkflow workflow, final DocumentType docType) {
        final Document document = new Document();
        document.setId(uuid);

        final DocumentInfo documentInfo = new DocumentInfo();
        documentInfo.setTypeId(docType.getId());
        document.setInfo(documentInfo);

        DocumentUtils.getDisplayName(handle).ifPresent(document::setDisplayName);

        final EditingInfo editingInfo = EditingUtils.determineEditingInfo(workflow, handle);
        documentInfo.setEditingInfo(editingInfo);

        return document;
    }

    private void loadFields(final Document document, final Node variant, final DocumentType docType) {
        for (FieldType field : docType.getFields()) {
            field.readFrom(variant).ifPresent(value -> document.addField(field.getId(), value));
        }
    }
}
