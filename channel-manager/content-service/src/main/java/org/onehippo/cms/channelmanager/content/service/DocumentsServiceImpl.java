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

package org.onehippo.cms.channelmanager.content.service;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.util.WorkflowUtils;
import org.onehippo.cms.channelmanager.content.exception.DocumentNotFoundException;
import org.onehippo.cms.channelmanager.content.exception.DocumentTypeNotFoundException;
import org.onehippo.cms.channelmanager.content.model.Document;
import org.onehippo.cms.channelmanager.content.model.DocumentInfo;
import org.onehippo.cms.channelmanager.content.model.DocumentTypeSpec;
import org.onehippo.cms.channelmanager.content.model.EditingInfo;
import org.onehippo.cms.channelmanager.content.model.FieldTypeSpec;
import org.onehippo.cms.channelmanager.content.model.UserInfo;
import org.onehippo.cms.channelmanager.content.util.MockResponse;
import org.onehippo.repository.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentsServiceImpl implements DocumentsService {
    private static final Logger log = LoggerFactory.getLogger(DocumentsServiceImpl.class);
    private static final DocumentsService INSTANCE = new DocumentsServiceImpl();

    static DocumentsService getInstance() {
        return INSTANCE;
    }

    private DocumentsServiceImpl() { }

    @Override
    public Document getDocument(final String id, final Session session, final Locale locale) throws DocumentNotFoundException {
        if ("test".equals(id)) {
            return MockResponse.createTestDocument(id);
        }

        final Document document = new Document();
        document.setId(id);

        try {
            final Node documentHandle = session.getNodeByIdentifier(id);
            document.setDisplayName(((HippoNode)documentHandle).getDisplayName());

            if (!HippoNodeType.NT_HANDLE.equals(documentHandle.getPrimaryNodeType().getName())
                    || !documentHandle.hasNode(documentHandle.getName())) {
                log.debug("Id {} doesn't refer to valid document", id);
                throw new DocumentNotFoundException();
            }

            final DocumentInfo info = new DocumentInfo();
            final Node variant = documentHandle.getNode(documentHandle.getName());
            info.setTypeId(variant.getPrimaryNodeType().getName());

            final EditableWorkflow workflow = retrieveWorkflow(documentHandle);
            info.setEditingInfo(determineEditingInfo(session, workflow));
            document.setInfo(info);

            determineDocumentFields(document, documentHandle, workflow, locale);
        } catch (RepositoryException e) {
            log.debug("Problem reading vital document parameters", e);
            throw new DocumentNotFoundException();
        }

        return document;
    }

    // protected for better testability
    protected void determineDocumentFields(final Document document,
                                           final Node handle,
                                           final EditableWorkflow workflow,
                                           final Locale locale) throws DocumentNotFoundException {
        final Node draft = getOrMakeDraftNode(workflow, handle);
        try {
            final DocumentTypeSpec docType = getDocumentTypeSpec(document, handle.getSession(), locale);

            // look-up document type and fill in document content
            for (FieldTypeSpec field : docType.getFields()) {
                final String fieldId = field.getId();
                if (draft.hasProperty(fieldId)) {
                    if (field.isStoredAsMultiValueProperty()) {
                        final List<String> values = new ArrayList<>();
                        for (Value v : draft.getProperty(fieldId).getValues()) {
                            values.add(v.getString());
                        }
                        if (!values.isEmpty()) {
                            document.addField(fieldId, values);
                        }
                    } else {
                        document.addField(fieldId, draft.getProperty(fieldId).getString());
                    }
                }
            }
        } catch (DocumentTypeNotFoundException|RepositoryException e) {
            throw new DocumentNotFoundException();
        }
    }

    // protected for better testability
    protected Node getOrMakeDraftNode(final EditableWorkflow workflow, final Node handle) throws DocumentNotFoundException {
        try {
            final Map<String, Serializable> hints = workflow.hints();
            final Session session = handle.getSession();

            if (isDocumentEditable(hints, session)) {
                return workflow.obtainEditableInstance().getNode(session);
            } else {
                final Optional<Node> variant = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT);
                if (!variant.isPresent()) {
                    throw new DocumentNotFoundException();
                }
                return variant.get();
            }
        } catch (WorkflowException|RepositoryException|RemoteException e) {
            throw new DocumentNotFoundException();
        }
    }

    // protected for better testability
    protected DocumentTypeSpec getDocumentTypeSpec(final Document document, final Session session, final Locale locale)
            throws DocumentTypeNotFoundException {
        final String docTypeId = document.getInfo().getType().getId();
        final DocumentTypesService docTypesService = DocumentTypesService.get();
        return docTypesService.getDocumentTypeSpec(docTypeId, session, locale);
    }

    // protected for better testability
    protected EditableWorkflow retrieveWorkflow(final Node documentHandle) throws DocumentNotFoundException {
        try {
            final HippoWorkspace workspace = (HippoWorkspace) documentHandle.getSession().getWorkspace();
            final WorkflowManager workflowManager = workspace.getWorkflowManager();
            final Workflow workflow = workflowManager.getWorkflow("editing", documentHandle);

            if (!(workflow instanceof EditableWorkflow)) {
                throw new DocumentNotFoundException();
            }

            return (EditableWorkflow)workflow;
        } catch (RepositoryException e) {
            throw new DocumentNotFoundException();
        }
    }

    // protected for better testability
    protected EditingInfo determineEditingInfo(final Session session, final Workflow workflow) {
        final EditingInfo info = new EditingInfo();

        try {
            final Map<String, Serializable> hints = workflow.hints();

            if (isDocumentEditable(hints, session)) {
                info.setState(EditingInfo.State.AVAILABLE);
            } else if (hints.containsKey("inUseBy")) {
                info.setState(EditingInfo.State.UNAVAILABLE_HELD_BY_OTHER_USER);
                info.setHolder(determineHolder((String)hints.get("inUseBy"), (HippoWorkspace) session.getWorkspace()));
            } else if (hints.containsKey("requests")) {
                info.setState(EditingInfo.State.UNAVAILABLE_REQUEST_PENDING);
            }
        } catch (RepositoryException|WorkflowException|RemoteException e) {
            log.debug("Failed to determine editing info", e);
        }
        return info;
    }

    private boolean isDocumentEditable(final Map<String, Serializable> hints, final Session session) {
        if ((Boolean) hints.get("obtainEditableInstance")) {
            return true;
        }

        // TODO: initial tests suggested that once the user has obtained the editable instance of a document,
        //       the hints would have set the obtainEditableInstance flag to false and the inUseBy flag to the
        //       current holder (self). Subsequet tests no longer observed this behaviour. Should we keep below
        //       extra check or not?
        if (hints.containsKey("inUseBy")) {
            final String inUseBy = (String) hints.get("inUseBy");
            if (inUseBy.equals(session.getUserID())) {
                return true;
            }
        }

        return false;
    }

    // protected for better testability
    protected UserInfo determineHolder(final String holderId, final HippoWorkspace workspace) {
        final UserInfo holder = new UserInfo();
        holder.setId(holderId);
        try {
            final User user =  workspace.getSecurityService().getUser(holderId);
            final String firstName = user.getFirstName();
            final String lastName = user.getLastName();

            // TODO: Below logic was copied from the org.hippoecm.frontend.plugins.cms.admin.users.User.java
            // (hippo-cms-perspectives). Move this logic into the repository's User class to be able to share it?
            StringBuilder sb = new StringBuilder();
            if (firstName != null) {
                sb.append(firstName.trim());
                sb.append(" ");
            }
            if (lastName != null) {
                sb.append(lastName.trim());
            }
            holder.setDisplayName(sb.toString().trim());
        } catch (RepositoryException e) {
            log.debug("Unable to determine displayName of holder", e);
        }
        return holder;
    }
}
