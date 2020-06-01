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
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.onehippo.repository.branch.BranchConstants;
import org.onehippo.repository.documentworkflow.DocumentHandle;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.ArrayUtils.removeElement;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_BRANCHES_PROPERTY;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME;
import static org.hippoecm.repository.util.JcrUtils.getMultipleStringProperty;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.PUBLISHED;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.UNPUBLISHED;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;

/**
 * <p>
 *     Removes the branch for this {@link DocumentHandle#getBranchId()} for this workflow invocation. If the
 *     branchId is equal to {@link BranchConstants#MASTER_BRANCH_ID} a {@link WorkflowException} is thrown since the master
 *     branch is not allowed to be removed
 * </p>
 * <p>
 *     Removing a branch 'x' removes the branch info for 'x' from all variants below the handle if the variant is for
 *     branch 'x', it removes the branch 'x' label info from version history if present and it removes the branch 'x'
 *     from the {@link HippoNodeType#HIPPO_BRANCHES_PROPERTY} on the handle node
 * </p>
 *
 */
public class RemoveBranchTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private String branchId;

    private DocumentVariant unpublished;
    private DocumentVariant published;
    private DocumentVariant draft;

    public void setUnpublished(DocumentVariant unpublished) {
        this.unpublished = unpublished;
    }

    public void setPublished(final DocumentVariant published) {
        this.published = published;
    }

    public void setDraft(final DocumentVariant draft) {
        this.draft = draft;
    }

    public void setBranchId(final String branchId) {
        this.branchId = branchId;
    }

    private List<DocumentVariant> getVariants() {
        return Stream.of(unpublished, published, draft)
                .filter(Objects::nonNull)
                .collect(toList());
    }

    @Override
    protected Object doExecute() throws WorkflowException, RepositoryException, RemoteException {
        if (branchId == null) {
            throw new WorkflowException("branchId needs to be provided");
        }
        if (branchId.equals(MASTER_BRANCH_ID)) {
            throw new WorkflowException(String.format("Cannot remove '%s' branch.", MASTER_BRANCH_ID));
        }
        final List<DocumentVariant> variants = getVariants();
        if (variants.isEmpty()) {
            throw new WorkflowException("No variant provided or present");
        }

        final Session workflowSession = getWorkflowContext().getInternalWorkflowSession();

        Node handle = null;
        for (DocumentVariant variant : variants) {
            final Node targetNode = variant.getNode(workflowSession);
            if (handle == null) {
                handle = targetNode.getParent();
            }
            if (targetNode.isNodeType(HIPPO_MIXIN_BRANCH_INFO) &&
                    targetNode.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString().equals(branchId)) {
                // to be sure also remove the properties since hippo document is relaxed
                targetNode.getProperty(HIPPO_PROPERTY_BRANCH_ID).remove();
                targetNode.getProperty(HIPPO_PROPERTY_BRANCH_NAME).remove();
                // the 'master' branch just means removing the branch info
                targetNode.removeMixin(HIPPO_MIXIN_BRANCH_INFO);
            }
        }

        if (handle != null && handle.hasProperty(HIPPO_BRANCHES_PROPERTY)) {
            // remove the branchId from the hippo:branches property
            final String[] branches = getMultipleStringProperty(handle, HIPPO_BRANCHES_PROPERTY, new String[0]);
            final String[] newBranches = removeElement(branches, branchId);
            handle.setProperty(HIPPO_BRANCHES_PROPERTY, newBranches);
        }

        if (unpublished == null) {
            return null;
        }

        final Node previewVariant = unpublished.getNode(workflowSession);

        final VersionManager versionManager = workflowSession.getWorkspace().getVersionManager();
        final VersionHistory versionHistory = versionManager.getVersionHistory(previewVariant.getPath());

        final String[] versionLabelsToRemove = new String[]{branchId + "-" + UNPUBLISHED.getState(), branchId + "-" + PUBLISHED.getState()};

        for (String label : versionLabelsToRemove) {
            if (versionHistory.hasVersionLabel(label)) {
                versionHistory.removeVersionLabel(label);
            }
        }

        return null;
    }

}
