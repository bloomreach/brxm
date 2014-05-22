/*
 * Copyright 2013-2014 Hippo B.V. (http://www.onehippo.com)
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

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.repository.scxml.SCXMLWorkflowData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DocumentHandle provides the {@link SCXMLWorkflowData} backing model object for the DocumentWorkflow SCXML state machine.
 */
public class DocumentHandle implements SCXMLWorkflowData {

    private static final Logger log = LoggerFactory.getLogger(DocumentHandle.class);

    private final Node handle;
    private Map<String, DocumentVariant> documents = new HashMap<>();
    private Map<String, Request> requests = new HashMap<>();
    private Boolean requestPending = false;
    private boolean initialized;

    public DocumentHandle(Node handle) throws WorkflowException {
        this.handle = handle;
    }

    protected DocumentVariant createDocumentVariant(Node node) throws RepositoryException {
        return new DocumentVariant(node);
    }

    protected Request createRequest(Node node) throws RepositoryException {
        return Request.createRequest(node);
    }

    protected boolean isInitialized() {
        return initialized;
    }

    @Override
    public void initialize() throws WorkflowException {
        if (initialized) {
            reset();
        }
        initialized = true;

        try {
            for (Node variant : new NodeIterable(handle.getNodes(handle.getName()))) {
                DocumentVariant doc = createDocumentVariant(variant);
                if (documents.containsKey(doc.getState())) {
                    log.warn("Document at path {} has multiple variants with state {}. Variant with identifier {} ignored.",
                            handle.getPath(), doc.getState(), variant.getIdentifier());
                }
                documents.put(doc.getState(), doc);
            }

            for (Node requestNode : new NodeIterable(handle.getNodes(HippoStdPubWfNodeType.HIPPO_REQUEST))) {
                Request request = createRequest(requestNode);
                if (request != null) {
                    if (request.isWorkflowRequest()) {
                        requests.put(request.getIdentity(), request);
                        if (!HippoStdPubWfNodeType.REJECTED.equals(((WorkflowRequest)request).getType())) {
                            requestPending = true;
                        }
                    }
                    else if (request.isScheduledRequest()) {
                        requests.put(request.getIdentity(), request);
                        requestPending = true;
                    }
                }
            }
        }
        catch (RepositoryException e) {
            throw new WorkflowException("DocumentHandle initialization failed", e);
        }
    }

    @Override
    public void reset() {
        if (initialized) {
            documents.clear();
            requests.clear();
            requestPending = false;
            initialized = false;
        }
    }

    public Node getHandle() {
        return handle;
    }

    public Map<String, Request> getRequests() {
        return requests;
    }

    public boolean isRequestPending() {
        return requestPending;
    }

    public Map<String, DocumentVariant> getDocuments() {
        return documents;
    }
}
