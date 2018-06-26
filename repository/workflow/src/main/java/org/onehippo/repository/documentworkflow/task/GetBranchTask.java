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

import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.DocumentVariant;

import static org.hippoecm.repository.HippoStdNodeType.DRAFT;
import static org.hippoecm.repository.HippoStdNodeType.PUBLISHED;
import static org.hippoecm.repository.HippoStdNodeType.UNPUBLISHED;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.util.JcrUtils.getStringProperty;

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
            throw new WorkflowException(String.format("branchId and state are both required but branchId = %s and state = %s", branchId, state));
        }

        final DocumentVariant variant = getVariant();
        if (variant == null) {
            return null;
        }

        final Session workflowSession = getWorkflowContext().getInternalWorkflowSession();

        final String variantBranchId = getStringProperty(variant.getNode(workflowSession), HIPPO_PROPERTY_BRANCH_ID, DocumentVariant.MASTER_BRANCH_ID);
        if (branchId.equals(variantBranchId)) {
            // variant is the correct value
            return variant;
        }

        if (draft == variant) {
            // drafts are never stored in version history
            return null;
        }

        final VersionHistory versionHistory = workflowSession.getWorkspace().getVersionManager().getVersionHistory(unpublished.getNode(workflowSession).getPath());
        final String versionLabel = branchId + "-" + state;
        if (!versionHistory.hasVersionLabel(versionLabel)) {
            return null;
        }
        return new DocumentVariant(versionHistory.getVersionByLabel(versionLabel).getFrozenNode());
    }

    private DocumentVariant getVariant() throws WorkflowException {
        switch (state) {
            case DRAFT:
                return draft;
            case PUBLISHED:
                return published;
            case UNPUBLISHED:
                return unpublished;
            default:
                throw new WorkflowException(String.format("Invalid state '%s', valid states are %s, %s, %s", state, DRAFT, PUBLISHED, UNPUBLISHED));
        }
    }

}
