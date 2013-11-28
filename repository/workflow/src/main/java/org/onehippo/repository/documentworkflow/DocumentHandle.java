/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.repository.documentworkflow;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentHandle {

    private static final Logger log = LoggerFactory.getLogger(DocumentHandle.class);

    private Map<String, Serializable> hints = new HashMap<>();
    private Map<String, PublishableDocument> documents = null;
    private Map<String, PublicationRequest> requests = null;
    private PublishableDocument subjectDocument = null;
    private PublicationRequest subjectRequest = null;
    private String user;
    private String workflowState;

    private Node handle;

    public DocumentHandle(WorkflowContext context, Node subject) throws RepositoryException {
        this.user = context.getUserIdentity();

        handle = subject.getParent();

        boolean subjectFound = false;

        for (Node variant : new NodeIterable(handle.getNodes(handle.getName()))) {
            PublishableDocument doc = new PublishableDocument(variant);
            if (documents != null && documents.containsKey(doc.getState())) {
                log.warn("Document at path {} has multiple variants with state {}. Variant with identifier {} ignored.",
                        new String[]{handle.getPath(), doc.getState(), variant.getIdentifier()});
//                throw new IllegalStateException("Document at path "+handle.getPath()+" has multiple variants with state "+doc.getState());
            }
            if (documents == null) {
                documents = new HashMap<>();
            }
            documents.put(doc.getState(), doc);
            if (!subjectFound && variant.isSame(subject)) {
                subjectDocument = doc;
                workflowState = doc.getState();
                subjectFound = true;
            }
        }
        for (Node request : new NodeIterable(handle.getNodes(HippoNodeType.NT_REQUEST))) {
            PublicationRequest req = new PublicationRequest(request);
            String requestType = JcrUtils.getStringProperty(request, "hippostdpubwf:type", "");
            if ("rejected".equals(requestType)) {
                if (!subjectFound && request.isSame(subject)) {
                    subjectRequest = req;
                    subjectFound = true;
                }
                else {
                    // ignore all other rejected requests
                    continue;
                }
            }
            if (requests != null && requests.containsKey(requestType)) {
                log.warn("Document at path {} has multiple requests of type {}. Request with identifier {} ignored.",
                        new String[]{handle.getPath(), requestType, request.getIdentifier()});
//                throw new IllegalStateException("Document at path "+handle.getPath()+" has multiple requests of type "+requestType);
                continue;
            }
            if (requests == null) {
                requests = new HashMap<>();
            }
            requests.put(requestType, req);
            if (!subjectFound && request.isSame(subject)) {
                subjectRequest = req;
                subjectFound = true;
            }
        }
    }

    public String getUser() {
        return user;
    }

    public String getWorkflowState() {
        return workflowState;
    }

    public PublicationRequest getRequest() {
        // TODO: temporary solution, probably this shouldn't be needed or the intended functionality be changed
        if (subjectRequest == null && requests != null && !requests.isEmpty()) {
            return requests.values().iterator().next();
        }
        return subjectRequest;
    }

    // TODO: temporary solution, probably this shouldn't be needed or the intended functionality be changed
    public PublishableDocument getDocument() {
        return subjectDocument;
    }

    public void setRequest(final PublicationRequest request) throws RepositoryException {
        String requestType = request.getType();
        if (requests == null) {
            requests = new HashMap<>();
        }
        if (requests.put(requestType, request) != null) {
            // TODO: probably an error situation?
        }
    }

    public void putDocumentVariant(PublishableDocument variant) throws RepositoryException {
        documents.put(variant.getState(), variant);
    }

    public PublishableDocument getDocumentVariantByState(String state) {
        return documents.get(state);
    }

    public PublishableDocument getDraft() {
        return documents.get(PublishableDocument.DRAFT);
    }

    public void setDraft(final PublishableDocument draft) {
    }

    public PublishableDocument getUnpublished() {
        return documents.get(PublishableDocument.UNPUBLISHED);
    }

    public void setUnpublished(final PublishableDocument unpublished) {
    }

    public PublishableDocument getPublished() {
        return documents.get(PublishableDocument.PUBLISHED);
    }

    public void setPublished(final PublishableDocument published) {
    }

    public Map<String, Serializable> getHints() {
            return hints;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
