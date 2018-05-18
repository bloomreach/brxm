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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.ArrayUtils.add;
import static org.apache.commons.lang3.ArrayUtils.contains;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_BRANCHES_PROPERTY;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME;
import static org.hippoecm.repository.api.HippoNodeType.NT_HIPPO_VERSION_INFO;
import static org.hippoecm.repository.util.JcrUtils.getMultipleStringProperty;
import static org.onehippo.repository.documentworkflow.DocumentVariant.CORE_BRANCH_ID;
import static org.onehippo.repository.documentworkflow.DocumentVariant.CORE_BRANCH_LABEL_PREVIEW;

public class BranchTask extends AbstractDocumentTask {

    static final Logger log = LoggerFactory.getLogger(BranchTask.class);
    private static final long serialVersionUID = 1L;

    private DocumentVariant variant;
    private String branchId;
    private String branchName;

    public DocumentVariant getVariant() {
        return variant;
    }

    public void setVariant(DocumentVariant variant) {
        this.variant = variant;
    }

    public void setBranchId(final String branchId) {
        this.branchId = branchId;
    }

    public void setBranchName(final String branchName) {
        this.branchName = branchName;
    }

    @Override
    protected Object doExecute() throws WorkflowException, RepositoryException, RemoteException {
        if (getVariant() == null || !getVariant().hasNode()) {
            throw new WorkflowException("No variant provided");
        }
        if (branchId == null || (branchName == null && !CORE_BRANCH_ID.equals(branchId))) {
            throw new WorkflowException("Both branchId and branchName need to be provided");
        }

        final Session workflowSession = getWorkflowContext().getInternalWorkflowSession();
        final Node targetNode = getVariant().getNode(workflowSession);

        final String targetLabel = branchId + "-preview";

        final VersionManager versionManager = workflowSession.getWorkspace().getVersionManager();
        final VersionHistory versionHistory = versionManager.getVersionHistory(targetNode.getPath());

        if (versionHistory.hasVersionLabel(targetLabel)) {
            throw new WorkflowException(String.format("TargetLabel '%s' does already exist in version history hence cannot " +
                    "create branch for '%s'", targetLabel, branchId));
        }

        if (targetNode.isNodeType(HIPPO_MIXIN_BRANCH_INFO)) {
            if (branchId.equals(targetNode.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString())) {
                // preview is already for branchId. Nothing to do
                return new Document(targetNode);
            }
        }
        if (branchId.equals(CORE_BRANCH_ID) && !targetNode.isNodeType(HIPPO_MIXIN_BRANCH_INFO)) {
            // preview already core. Nothing to do
            return new Document(targetNode);
        }

        JcrUtils.ensureIsCheckedOut(targetNode);

        addBranchesPropertyToHandle(targetNode.getParent(), branchId, versionHistory);

        if (CORE_BRANCH_ID.equals(branchId) && targetNode.isNodeType(HIPPO_MIXIN_BRANCH_INFO)) {
            // to be sure also remove the properties since hippo document is relaxed
            targetNode.getProperty(HIPPO_PROPERTY_BRANCH_ID).remove();
            targetNode.getProperty(HIPPO_PROPERTY_BRANCH_NAME).remove();
            // the 'core' branch just means removing the branch info
            targetNode.removeMixin(HIPPO_MIXIN_BRANCH_INFO);
        } else {
            targetNode.addMixin(HIPPO_MIXIN_BRANCH_INFO);
            targetNode.setProperty(HIPPO_PROPERTY_BRANCH_ID, branchId);
            targetNode.setProperty(HIPPO_PROPERTY_BRANCH_NAME, branchName);
        }

        return new Document(targetNode);
    }

    private void addBranchesPropertyToHandle(final Node handle, final String branchId, final VersionHistory versionHistory) throws RepositoryException {

        if (!handle.isNodeType(NT_HIPPO_VERSION_INFO)) {
            handle.addMixin(NT_HIPPO_VERSION_INFO);
        }

        String[] branches = getMultipleStringProperty(handle, HIPPO_BRANCHES_PROPERTY, new String[0]);
        if (!branchId.equals(CORE_BRANCH_ID) && !contains(branches, CORE_BRANCH_ID) &&
                versionHistory.hasVersionLabel(CORE_BRANCH_LABEL_PREVIEW)) {
            // add core to available branches
            branches = add(branches, CORE_BRANCH_ID);
        }
        if (contains(branches, branchId)) {
             log.warn("Handle property '{}' already contains branchId '{}' which is unexpected since the branch should " +
                     "have already been created.", HIPPO_BRANCHES_PROPERTY, branchId);
        } else {
            handle.setProperty(HIPPO_BRANCHES_PROPERTY, add(branches, branchId));
        }
    }

}
