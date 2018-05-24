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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionHistory;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.util.JcrUtils.getStringProperty;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.DRAFT;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.PUBLISHED;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.UNPUBLISHED;

public class GetBranchTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private String branchId;
    private String state;

    private DocumentVariant unpublished;
    private DocumentVariant published;
    private DocumentVariant draft;

    public void setBranchId(final String branchId) {
        this.branchId = branchId;
    }

    public void setState(final String state) {
        this.state = state;
    }

    public void setUnpublished(DocumentVariant unpublished) {
        this.unpublished = unpublished;
    }

    public void setPublished(final DocumentVariant published) {
        this.published = published;
    }

    public void setDraft(final DocumentVariant draft) {
        this.draft = draft;
    }

    @Override
    protected Object doExecute() throws WorkflowException, RepositoryException {
        if (branchId == null || state == null) {
            throw new WorkflowException("branchId and required state needs to be provided");
        }

        final Session workflowSession = getWorkflowContext().getInternalWorkflowSession();

        if (DRAFT.name().equals(state)) {
            // if current draft is for branchId, return draft, otherwise null
            if (draft == null) {
                return null;
            }
            final String draftBranchId = getStringProperty(draft.getNode(workflowSession), HIPPO_PROPERTY_BRANCH_ID, null);
            if (branchId.equals(draftBranchId)) {
                // draft is the correct value
                return draft;
            }
            return null;
        } else if (PUBLISHED.name().equals(state)) {
            // if current published is for branchId, return published, otherwise try to find in version history
            if (published == null) {
                return null;
            }
            final String publishedBranchId = getStringProperty(published.getNode(workflowSession), HIPPO_PROPERTY_BRANCH_ID, null);
            if (branchId.equals(publishedBranchId)) {
                // draft is the correct value
                return published;
            }
            if (unpublished == null) {
                return null;
            }
            final VersionHistory versionHistory = workflowSession.getWorkspace().getVersionManager().getVersionHistory(unpublished.getNode(workflowSession).getPath());
            return getDocumentVariant(versionHistory, branchId, state);
        } else if (UNPUBLISHED.name().equals(state)) {
            // if current unpublished is for branchId, return unpublished, otherwise try to find in version history
            if (unpublished == null) {
                return null;
            }
            final String unpublishedBranchId = getStringProperty(unpublished.getNode(workflowSession), HIPPO_PROPERTY_BRANCH_ID, null);
            if (branchId.equals(unpublishedBranchId)) {
                // draft is the correct value
                return unpublished;
            }
            final VersionHistory versionHistory = workflowSession.getWorkspace().getVersionManager().getVersionHistory(unpublished.getNode(workflowSession).getPath());
            return getDocumentVariant(versionHistory, branchId, state);
        } else {
            throw new WorkflowException(String.format("Unknown state '%s' request", state));
        }

    }

    private Document getDocumentVariant(final VersionHistory versionHistory, final String branchId, final String state) throws RepositoryException {
        final String versionLabel = branchId + "-" + state;

        if (!versionHistory.hasVersionLabel(versionLabel)) {
            return null;
        }

        return new DocumentVariant(versionHistory.getVersionByLabel(versionLabel).getFrozenNode());
    }


}
