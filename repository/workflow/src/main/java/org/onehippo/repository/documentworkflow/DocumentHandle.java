/*
 * Copyright 2013-2017 Hippo B.V. (http://www.onehippo.com)
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.DocumentVariant;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.WorkflowUtils;
import org.onehippo.repository.branch.BranchHandle;
import org.onehippo.repository.scxml.SCXMLWorkflowData;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_STATE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.api.HippoNodeType.NT_HIPPO_VERSION_INFO;
import static org.hippoecm.repository.standardworkflow.DocumentVariant.MASTER_BRANCH_ID;
import static org.hippoecm.repository.standardworkflow.DocumentVariant.MASTER_BRANCH_LABEL_UNPUBLISHED;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.UNPUBLISHED;

/**
 * DocumentHandle provides the {@link SCXMLWorkflowData} backing model object for the DocumentWorkflow SCXML state machine.
 */
public class DocumentHandle implements SCXMLWorkflowData {

    private static final Logger log = LoggerFactory.getLogger(DocumentHandle.class);

    private final Node handle;
    private Map<String, DocumentVariant> documents = new HashMap<>();
    private Set<String> branches = new HashSet<>();
    private String branchId;
    private Map<String, Request> requests = new HashMap<>();
    private boolean requestPending = false;
    private boolean initialized;

    public DocumentHandle(Node handle) {
        this.handle = handle;
    }

    /**
     * Provide hook for extension
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    protected DocumentVariant createDocumentVariant(Node node) throws RepositoryException {
        return new DocumentVariant(node);
    }

    /**
     * Provide hook for extension
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    protected Request createRequest(Node node) throws RepositoryException {
        return Request.createRequest(node);
    }

    protected boolean isInitialized() {
        return initialized;
    }

    @Override
    public void initialize() throws WorkflowException {
        initialize(null);
    }

    @Override
    public void initialize(final String branchId) throws WorkflowException {
        if (initialized) {
            reset();
        }
        try {
            if (branchId == null) {
                this.branchId = MASTER_BRANCH_ID;
            } else {
                this.branchId = branchId;
            }
            initializeDocumentVariants();
            initializeDocumentBranches();
            initializeRequestStatus();
            initialized = true;
        } catch (RepositoryException e) {
            reset();
            throw new WorkflowException("DocumentHandle initialization failed", e);
        }
    }

    /**
     * Provide hook for extension
     * <p>
     * This implementation calls {@link #createDocumentVariant(Node)}
     *
     * @throws RepositoryException
     */
    protected void initializeDocumentVariants() throws RepositoryException {
        for (Node variant : new NodeIterable(handle.getNodes(handle.getName()))) {
            DocumentVariant doc = createDocumentVariant(variant);
            if (documents.containsKey(doc.getState())) {
                log.warn("Document at path {} has multiple variants with state {}. Variant with identifier {} ignored.",
                        handle.getPath(), doc.getState(), variant.getIdentifier());
            }
            documents.put(doc.getState(), doc);
        }
    }

    /**
     * Provide hook for extension
     * <p>
     * This implementation calls {@link #createRequest(Node)}
     *
     * @throws RepositoryException
     */
    protected void initializeRequestStatus() throws RepositoryException {
        for (Node requestNode : new NodeIterable(handle.getNodes(HippoStdPubWfNodeType.HIPPO_REQUEST))) {
            Request request = createRequest(requestNode);
            if (request != null) {
                if (request.isWorkflowRequest()) {
                    requests.put(request.getIdentity(), request);
                    if (!HippoStdPubWfNodeType.REJECTED.equals(((WorkflowRequest) request).getType())) {
                        requestPending = true;
                    }
                } else if (request.isScheduledRequest()) {
                    requests.put(request.getIdentity(), request);
                    requestPending = true;
                }
            }
        }
    }

    /**
     * Provide hook for extension
     */
    public final void setRequestPending(boolean requestPending) {
        this.requestPending = requestPending;
    }

    @Override
    public void reset() {
        if (initialized) {
            branchId = null;
            documents.clear();
            requests.clear();
            branches.clear();
            requestPending = false;
            initialized = false;
            //Do NOT clear initialPayload
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

    public Set<String> getBranches() {
        return branches;
    }

    public boolean isOnlyMaster() {
        return branches.size() == 1 && branches.contains(MASTER_BRANCH_ID);
    }

    public boolean isLiveAvailable() {
        return getBranchHandle().isLiveAvailable();
    }

    public boolean isAnyBranchLiveAvailable() throws WorkflowException {
        final DocumentVariant published = documents.get(WorkflowUtils.Variant.PUBLISHED.getState());
        if (published == null) {
            return false;
        }
        try {
            return WorkflowUtils.hasAvailability(published.getNode(), "live");
        } catch (RepositoryException e) {
            throw new WorkflowException("Exception while trying to find live variant", e);
        }
    }

    public boolean isPreviewAvailable() {
        return getBranchHandle().isPreviewAvailable();
    }

    public boolean isModified() {
        return getBranchHandle().isModified();
    }

    public String getBranchId() {
        return branchId;
    }

    public boolean hasMultipleDocumentVariants(final String state) throws RepositoryException {
        int count = 0;
        for (Node variant : new NodeIterable(handle.getNodes(handle.getName()))) {
            if (state.equals(JcrUtils.getStringProperty(variant, HIPPOSTD_STATE, null))) {
                count++;
            }
        }
        return count > 1;
    }

    /**
     * Returns a branch handle with the same branch id as this document handle was initialized with.
     *
     * @return branch handle with same branch id as this handle
     * @throws IllegalStateException if the document handle is not initialized.
     */
    public BranchHandle getBranchHandle() {
        if (isInitialized()) {
            return new BranchHandleImpl(branchId, this);
        }
        throw new IllegalStateException("document handle not initialized, please initialize it before calling this method");
    }

    private void initializeDocumentBranches() throws RepositoryException {

        // do not use the possibly frozen nodes which you get from getDocuments().get("unpublished").getNode
        // but use the handle instead
        final Node published = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED).orElse(null);
        final Node unpublished = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).orElse(null);
        final Node draft = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT).orElse(null);

        if (published != null) {
            if (published.isNodeType(HIPPO_MIXIN_BRANCH_INFO)) {
                branches.add(published.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
            } else {
                branches.add(MASTER_BRANCH_ID);
            }
        }

        if (unpublished != null) {
            if (unpublished.isNodeType(HIPPO_MIXIN_BRANCH_INFO)) {
                branches.add(unpublished.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
            } else {
                branches.add(MASTER_BRANCH_ID);
            }
            if (!unpublished.isNodeType(JcrConstants.MIX_VERSIONABLE)) {
                return;
            }

            // first check the handle for being of nodetype NT_HIPPO_VERSION_INFO for performance: Otherwise, skip
            // version history
            if (unpublished.getParent().isNodeType(NT_HIPPO_VERSION_INFO)) {
                final VersionManager versionManager = unpublished.getSession().getWorkspace().getVersionManager();
                try {
                    final VersionHistory versionHistory = versionManager.getVersionHistory(unpublished.getPath());

                    if (versionHistory.hasVersionLabel(MASTER_BRANCH_LABEL_UNPUBLISHED)) {
                        // master branch present
                        branches.add(MASTER_BRANCH_ID);
                    }

                    for (String label : versionHistory.getVersionLabels()) {
                        if (label.endsWith("-" + UNPUBLISHED.getState())) {
                            final Version version = versionHistory.getVersionByLabel(label);
                            final Node frozenNode = version.getFrozenNode();
                            if (frozenNode.hasProperty(HIPPO_PROPERTY_BRANCH_ID)) {
                                // found a real branch instead of a label for a non-branch
                                branches.add(frozenNode.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
                            }
                        }
                    }
                } catch (RepositoryException e) {
                    log.info("Could not get version history, most likely the unpublished has mix:versionable but has not yet " +
                            "been saved.", e);
                }
            }

        }

        if (draft != null) {
            if (draft.isNodeType(HIPPO_MIXIN_BRANCH_INFO)) {
                branches.add(draft.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
            } else {
                branches.add(MASTER_BRANCH_ID);
            }
        }
    }

}
