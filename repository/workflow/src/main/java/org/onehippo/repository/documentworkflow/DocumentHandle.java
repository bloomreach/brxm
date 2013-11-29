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
import java.util.Collections;
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
    private PublicationRequest rejectedRequest = null;
    private PublicationRequest request = null;
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
            }
            getDocuments(true).put(doc.getState(), doc);
            if (!subjectFound && variant.isSame(subject)) {
                workflowState = doc.getState();
                subjectFound = true;
            }
        }
        for (Node requestNode : new NodeIterable(handle.getNodes(PublicationRequest.HIPPO_REQUEST))) {
            PublicationRequest req = new PublicationRequest(requestNode);
            String requestType = JcrUtils.getStringProperty(requestNode, PublicationRequest.HIPPOSTDPUBWF_TYPE, "");
            if ("rejected".equals(requestType)) {
                if (!subjectFound && requestNode.isSame(subject)) {
                    rejectedRequest = req;
                    subjectFound = true;
                }
                else {
                    // ignore all other rejected requests
                }
                continue;
            }
            if (request != null) {
                log.warn("Document at path {} has multiple outstanding requests. Request with identifier {} ignored.",
                        new String[]{handle.getPath(), requestNode.getIdentifier()});
                continue;
            }
            request = req;
            if (!subjectFound && requestNode.isSame(subject)) {
                subjectFound = true;
            }
        }
    }

    protected Map<String, PublishableDocument> getDocuments(boolean create) {
        if (create && documents == null) {
            documents = new HashMap<>();
        }
        if (documents != null) {
            return documents;
        }
        return Collections.emptyMap();
    }

    public String getUser() {
        return user;
    }

    public String getWorkflowState() {
        return workflowState;
    }

    public PublicationRequest getRequest() {
        return request;
    }

    public PublicationRequest getRejectedRequest() {
        return rejectedRequest;
    }

    public void setRequest(final PublicationRequest request) throws RepositoryException {
        if (this.request != null) {
            // TODO: probably an error situation?
        }
        this.request = request;
    }

    public void putDocumentVariant(PublishableDocument variant) throws RepositoryException {
        getDocuments(true).put(variant.getState(), variant);
    }

    public PublishableDocument getDocumentVariantByState(String state) {
        return getDocuments(false).get(state);
    }

    public PublishableDocument getDraft() {
        return getDocuments(false).get(PublishableDocument.DRAFT);
    }

    public PublishableDocument getUnpublished() {
        return getDocuments(false).get(PublishableDocument.UNPUBLISHED);
    }

    public PublishableDocument getPublished() {
        return getDocuments(false).get(PublishableDocument.PUBLISHED);
    }

    public Map<String, Serializable> getHints() {
            return hints;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
