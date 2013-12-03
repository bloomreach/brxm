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
import org.hippoecm.repository.api.RepositoryMap;
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
    private String ds;
    private DocumentWorkflow.Features features = DocumentWorkflow.Features.all;

    private Node handle;

    public DocumentHandle(WorkflowContext context, Node subject) throws RepositoryException {

        RepositoryMap workflowConfiguration = context.getWorkflowConfiguration();
        if (workflowConfiguration.exists()) {
            String featuresConfiguration = (String)workflowConfiguration.get("workflow.features");
            if (featuresConfiguration != null) {
                try {
                    features = DocumentWorkflow.Features.valueOf(featuresConfiguration);
                }
                catch (IllegalArgumentException e) {
                    String configurationPath = (String)workflowConfiguration.get("_path");
                    if (configurationPath == null) {
                        configurationPath = "<unknown>";
                    }
                    log.warn("Unknown DocumentWorkflow.Features [{}] configured for property workflow.features at: {}", featuresConfiguration, configurationPath);
                }
            }
        }

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

    /**
     * @return configured Features or {@link DocumentWorkflow.Features#all} by default
     */
    public DocumentWorkflow.Features getFeatures() {
        return features;
    }

    /**
     * Script supporting shortened version of {@link #getFeatures() getFeatures().name()}
     * @return configured {@link DocumentWorkflow.Features} name value
     */
    public String getF() {
        return getFeatures().name();
    }

    /**
     * Get short notation representing the states of all existing Variants
     * @return concatenation of: "d" if Draft variant exists, "u" if Unpublished variant exists, "p" if Published variant exists
     */
    public String getStates() {
        if (ds == null) {
            ds = "";
            if (getDraft() != null) {
                ds += "d";
            }
            if (getUnpublished() != null) {
                ds += "u";
            }
            if (getPublished() != null) {
                ds += "p";
            }
        }
        return ds;
    }

    /**
     * Script supporting shortened version of {@link #getStates}
     * @return concatenation of: "d" if Draft variant exists, "u" if Unpublished variant exists, "p" if Published variant exists
     */
    public String getS() {
        return getStates();
    }

    public String getUser() {
        return user;
    }

    /**
     * @return the state of the Document variant which is subject of this workflow or null if none
     */
    public String getSubjectState() {
        return workflowState;
    }

    /**
     * Script supporting shortened version of {@link #getSubjectState}
     * @return the state of the Document variant which is subject of this workflow or null if none
     */
    public String getSs() {
        return getSubjectState();
    }

    /**
     * @return the active Request or null if none
     */
    public PublicationRequest getRequest() {
        return request;
    }

    /**
     * Script supporting shortened version of {@link #getRequest}
     * @return the active Request or null if none
     */
    public PublicationRequest getR() {
        return getRequest();
    }

    /**
     * @return the rejected Request which is subject of this workflow or null if none
     */
    public PublicationRequest getRejectedRequest() {
        return rejectedRequest;
    }

    /**
     * Script supporting shortened version of {@link #getRejectedRequest}
     * @return the rejected Request which is subject of this workflow or null if none
     */
    public PublicationRequest getRr() {
        return getRejectedRequest();
    }

    public void setRequest(final PublicationRequest request) throws RepositoryException {
        if (this.request != null) {
            // TODO: probably an error situation?
        }
        this.request = request;
    }

    public void putDocumentVariant(PublishableDocument variant) throws RepositoryException {
        ds = null;
        getDocuments(true).put(variant.getState(), variant);
    }

    public PublishableDocument getDocumentVariantByState(String state) {
        return getDocuments(false).get(state);
    }

    /**
     * @return Draft variant if it exists or null otherwise
     */
    public PublishableDocument getDraft() {
        return getDocuments(false).get(PublishableDocument.DRAFT);
    }

    /**
     * @return Draft variant if it exists or null otherwise
     */
    public PublishableDocument getD() {
        return getDraft();
    }

    /**
     * @return Unpublished variant if it exists or null otherwise
     */
    public PublishableDocument getUnpublished() {
        return getDocuments(false).get(PublishableDocument.UNPUBLISHED);
    }

    /**
     * @return Unpublished variant if it exists or null otherwise
     */
    public PublishableDocument getU() {
        return getUnpublished();
    }

    /**
     * @return Published variant if it exists or null otherwise
     */
    public PublishableDocument getPublished() {
        return getDocuments(false).get(PublishableDocument.PUBLISHED);
    }

    /**
     * @return Published variant if it exists or null otherwise
     */
    public PublishableDocument getP() {
        return getPublished();
    }

    public Map<String, Serializable> getHints() {
            return hints;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
