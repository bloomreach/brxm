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
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.JcrConstants;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.Utilities;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.jackrabbit.JcrConstants.JCR_ROOTVERSION;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME;
import static org.hippoecm.repository.util.JcrUtils.getStringProperty;

public class BranchTask extends AbstractDocumentTask {

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
        if (branchId == null || (branchName == null && !"core".equals(branchId))) {
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
        if ("core".equals(branchId) && targetNode.isNodeType(HIPPO_MIXIN_BRANCH_INFO)) {
            // to be sure also remove the properties since hippo document is relaxed
            targetNode.setProperty(HIPPO_PROPERTY_BRANCH_ID, branchId);
            targetNode.setProperty(HIPPO_PROPERTY_BRANCH_NAME, branchName);
            // the 'core' branch just means removing the branch info
            targetNode.removeMixin(HIPPO_MIXIN_BRANCH_INFO);
        } else {
            if (!targetNode.isNodeType(HIPPO_MIXIN_BRANCH_INFO)) {
                targetNode.addMixin(HIPPO_MIXIN_BRANCH_INFO);
            }
            targetNode.setProperty(HIPPO_PROPERTY_BRANCH_ID, branchId);
            targetNode.setProperty(HIPPO_PROPERTY_BRANCH_NAME, branchName);
        }

        return new Document(targetNode);
    }

}
