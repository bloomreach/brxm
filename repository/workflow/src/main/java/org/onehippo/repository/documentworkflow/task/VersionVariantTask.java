/*
 * Copyright 2013-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.documentworkflow.DocumentVariant;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_VERSION_HISTORY_PROPERTY;
import static org.hippoecm.repository.api.HippoNodeType.NT_HIPPO_VERSION_INFO;
import static org.onehippo.repository.documentworkflow.DocumentVariant.CORE_BRANCH_ID;
import static org.onehippo.repository.util.JcrConstants.JCR_VERSION_HISTORY;

/**
 * Custom workflow task for creating a JCR version of a document variant node.
 */
public class VersionVariantTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private DocumentVariant variant;
    private String trigger;

    public DocumentVariant getVariant() {
        return variant;
    }

    public void setVariant(DocumentVariant variant) {
        this.variant = variant;
    }

    public void setTrigger(final String trigger) {
        this.trigger = trigger;
    }

    @Override
    protected Object doExecute() throws WorkflowException, RepositoryException, RemoteException {

        if (getVariant() == null || !getVariant().hasNode()) {
            throw new WorkflowException("No variant provided");
        }

        final Session workflowSession = getWorkflowContext().getInternalWorkflowSession();
        Node targetNode = getVariant().getNode(workflowSession);

        final Version checkedIn = createVersion(targetNode, trigger);
        return new Document(checkedIn);

    }

    protected Version createVersion(final Node targetNode, final String trigger) throws RepositoryException, WorkflowException {
        // ensure no pending changes which would fail the checkin
        final Session workflowSession = targetNode.getSession();
        workflowSession.save();
        final VersionManager versionManager = workflowSession.getWorkspace().getVersionManager();
        final Version checkedIn = versionManager.checkpoint(targetNode.getPath());

        final Node handle = targetNode.getParent();
        if (!handle.isNodeType(NT_HIPPO_VERSION_INFO)) {
            handle.addMixin(NT_HIPPO_VERSION_INFO);
        }
        final String versionHistoryIdentifier = targetNode.getProperty(JCR_VERSION_HISTORY).getNode().getIdentifier();
        if (handle.hasProperty(HIPPO_VERSION_HISTORY_PROPERTY)) {
            if (!versionHistoryIdentifier.equals(handle.getProperty(HIPPO_VERSION_HISTORY_PROPERTY).getString())) {
                // version identief has changed on preview...not normal but can be result of manually removing the versionable
                // mix. Setting the new value
                handle.setProperty(HIPPO_VERSION_HISTORY_PROPERTY, versionHistoryIdentifier);
            }
        } else {
            handle.setProperty(HIPPO_VERSION_HISTORY_PROPERTY, versionHistoryIdentifier);
        }

        final VersionHistory versionHistory = versionManager.getVersionHistory(targetNode.getPath());

        final String branchId;

        if (targetNode.isNodeType(HIPPO_MIXIN_BRANCH_INFO)) {
            branchId = targetNode.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString();
        } else {
            branchId = CORE_BRANCH_ID;
        }

        final String[] branchLabels;
        if ("publication".equals(trigger)) {
            branchLabels = new String[] {branchId + "-preview", branchId + "-live"};
        } else {
            branchLabels = new String[] {branchId + "-preview"};
        }

        for (String branchLabel : branchLabels) {
            versionHistory.addVersionLabel(checkedIn.getName(), branchLabel, true);
        }

        return checkedIn;
    }


}
