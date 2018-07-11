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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.standardworkflow.DocumentVariant;
import org.onehippo.repository.util.JcrConstants;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_BRANCHES_PROPERTY;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.api.HippoNodeType.NT_HIPPO_VERSION_INFO;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.UNPUBLISHED;
import static org.hippoecm.repository.standardworkflow.DocumentVariant.MASTER_BRANCH_ID;
import static org.hippoecm.repository.standardworkflow.DocumentVariant.MASTER_BRANCH_LABEL_UNPUBLISHED;

public class ListBranchesTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

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

    private DocumentVariant getVariant() {
        if (unpublished != null) {
            return unpublished;
        }
        if (published != null) {
            return published;
        }
        if (draft != null) {
            return draft;
        }
        return null;
    }

    @Override
    protected Object doExecute() throws WorkflowException, RepositoryException {
        if (getVariant() == null || !getVariant().hasNode()) {
            throw new WorkflowException("No variant provided");
        }

        final Session workflowSession = getWorkflowContext().getInternalWorkflowSession();
        final Node variant = getVariant().getNode(workflowSession);
        final Node handle = variant.getParent();
        final Set<String> branches = new HashSet<>();
        if (handle.isNodeType(NT_HIPPO_VERSION_INFO)) {
            final String[] branchArray = JcrUtils.getMultipleStringProperty(handle, HIPPO_BRANCHES_PROPERTY, null);
            if (branchArray == null) {
                branches.add(MASTER_BRANCH_ID);
            } else {
                branches.addAll(Arrays.asList(branchArray));
            }
        } else {
            branches.add(MASTER_BRANCH_ID);
        }

        // validate all branches are available (either as preview below handle and otherwise in version history)
        final Set<String> realAvailableBranches = getRealAvailableBranches(workflowSession, variant);

        // only keep the branches that are really available (for example skip branches which are present on the
        // hippo:handle node but are not available any more in version history
        branches.retainAll(realAvailableBranches);

        return branches;


    }

    private Set<String> getRealAvailableBranches(final Session workflowSession, final Node variant) throws RepositoryException {
        final Set<String> realAvailableBranches = new HashSet<>();
        if (variant.isNodeType(HIPPO_MIXIN_BRANCH_INFO)) {
            realAvailableBranches.add(variant.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
        } else {
            // current preview is for master
            realAvailableBranches.add(MASTER_BRANCH_ID);
        }

        if (!variant.isNodeType(JcrConstants.MIX_VERSIONABLE)) {
            return realAvailableBranches;
        }

        final VersionManager versionManager = workflowSession.getWorkspace().getVersionManager();
        final VersionHistory versionHistory = versionManager.getVersionHistory(variant.getPath());

        if (versionHistory.hasVersionLabel(MASTER_BRANCH_LABEL_UNPUBLISHED)) {
            // master branch present
            realAvailableBranches.add(MASTER_BRANCH_ID);
        }

        for (String label : versionHistory.getVersionLabels()) {
            if (label.endsWith("-" + UNPUBLISHED.getState())) {
                final Version version = versionHistory.getVersionByLabel(label);
                final Node frozenNode = version.getFrozenNode();
                if (frozenNode.hasProperty(HIPPO_PROPERTY_BRANCH_ID)) {
                    // found a real branch instead of a label for a non-branch
                    realAvailableBranches.add(frozenNode.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
                }
            }
        }
        return realAvailableBranches;
    }
}
