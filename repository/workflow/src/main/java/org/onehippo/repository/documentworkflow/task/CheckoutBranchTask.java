/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.documentworkflow.task;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.standardworkflow.DocumentVariant;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.UNPUBLISHED;
import static org.hippoecm.repository.standardworkflow.DocumentVariant.MASTER_BRANCH_ID;
import static org.hippoecm.repository.standardworkflow.DocumentVariant.MASTER_BRANCH_LABEL_PUBLISHED;

public class CheckoutBranchTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;
    public static final String ANY = "*";

    private DocumentVariant variant;
    private String branchId;
    private String stateLabel;
    private boolean forceReplace;

    public DocumentVariant getVariant() {
        return variant;
    }

    public void setVariant(DocumentVariant variant) {
        this.variant = variant;
    }

    public void setBranchId(final String branchId) {
        this.branchId = branchId;
    }

    public void setStateLabel(final String stateLabel) {
        this.stateLabel = stateLabel;
    }

    public void setForceReplace(final Boolean forceReplace) {
        if (forceReplace == null) {
            this.forceReplace = false;
            return;
        }
        this.forceReplace = forceReplace.booleanValue();
    }

    @Override
    protected Object doExecute() throws WorkflowException, RepositoryException, RemoteException {
        if (getVariant() == null || !getVariant().hasNode()) {
            throw new WorkflowException("No variant provided");
        }
        if (branchId == null) {
            throw new WorkflowException("branchId needs to be provided");
        }
        final Session workflowSession = getWorkflowContext().getInternalWorkflowSession();
        // ensure no pending changes which would fail the checkout
        workflowSession.save();

        Node targetNode = getVariant().getNode(workflowSession);

        if (ANY.equals(branchId)) {
            return checkoutMasterOrEldestBranch(workflowSession, targetNode);
        }

        if (!forceReplace && targetNode.isNodeType(HIPPO_MIXIN_BRANCH_INFO)) {
            if (branchId.equals(targetNode.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString())) {
                // preview is already for branchId. Nothing to do
                return new DocumentVariant(targetNode);
            }
        }
        if (!forceReplace && branchId.equals(MASTER_BRANCH_ID) && !targetNode.isNodeType(HIPPO_MIXIN_BRANCH_INFO)) {
            // preview already master. Nothing to do
            return new DocumentVariant(targetNode);
        }

        final VersionManager versionManager = workflowSession.getWorkspace().getVersionManager();
        final VersionHistory versionHistory = versionManager.getVersionHistory(targetNode.getPath());

        final String versionLabelToRestore = branchId + "-" + UNPUBLISHED.getState();

        if (!versionHistory.hasVersionLabel(versionLabelToRestore)) {
            throw new WorkflowException(String.format("version label '%s' does not exist in version history so cannot " +
                    "checkout branch '%s'", versionLabelToRestore, branchId));
        }

        // restore the version to preview
        versionManager.restore(versionHistory.getVersionByLabel(versionLabelToRestore), false);
        // after restore, make sure the preview gets checked out
        return returnDocument(targetNode);
    }

    /**
     * @return a checked out Document for any branch that has the required label or NULL if no such version exists. If
     * there is no Master to restore for stateLabel but multiple others, we pick the oldest version to restore
     */
    private DocumentVariant checkoutMasterOrEldestBranch(final Session workflowSession, final Node targetNode) throws RepositoryException, WorkflowException {
        if (stateLabel == null) {
            throw new WorkflowException("When branchId is '*', a non-null label is required");
        }
        final VersionManager versionManager = workflowSession.getWorkspace().getVersionManager();
        final VersionHistory versionHistory = versionManager.getVersionHistory(targetNode.getPath());
        if (versionHistory.hasVersionLabel(MASTER_BRANCH_LABEL_PUBLISHED)) {
            // master has precedence
            versionManager.restore(versionHistory.getVersionByLabel(MASTER_BRANCH_LABEL_PUBLISHED), false);
            // after restore, make sure the preview gets checked out
            return returnDocument(targetNode);
        }

        final List<String> labels = Arrays.stream(versionHistory.getVersionLabels()).filter(label -> label.endsWith("-" + stateLabel)).collect(Collectors.toList());
        if (labels.isEmpty()) {
            // there is no version for 'stateLabel', return null
            return null;
        }

        // restore any branch that is published
        final Version version = findOldestVersion(versionHistory, labels);

        versionManager.restore(version, false);
        // after restore, make sure the preview gets checked out
        return returnDocument(targetNode);

    }

    private Version findOldestVersion(final VersionHistory versionHistory, final List<String> labels) throws RepositoryException {
        Version versionToRestore = null;
        for (String label : labels) {
            final Version version = versionHistory.getVersionByLabel(label);
            if (versionToRestore == null) {
                versionToRestore = version;
                continue;
            }
            if (version.getCreated().before(versionToRestore.getCreated())) {
                versionToRestore = version;
            }
        }
        return versionToRestore;
    }

    private DocumentVariant returnDocument(final Node targetNode) throws RepositoryException {
        JcrUtils.ensureIsCheckedOut(targetNode);
        return new DocumentVariant(targetNode);
    }
}
