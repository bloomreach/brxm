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

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.onehippo.repository.util.JcrConstants;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.onehippo.repository.documentworkflow.DocumentVariant.CORE_BRANCH_ID;

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

        final Set<String> branches = new HashSet<>();


        final Session workflowSession = getWorkflowContext().getInternalWorkflowSession();
        final Node targetNode = getVariant().getNode(workflowSession);

        if (targetNode.isNodeType(HIPPO_MIXIN_BRANCH_INFO)) {
            branches.add(targetNode.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
        } else {
            // current preview is for core
            branches.add(CORE_BRANCH_ID);
        }

        if (!targetNode.isNodeType(JcrConstants.MIX_VERSIONABLE)) {
            return branches;
        }

        final VersionManager versionManager = workflowSession.getWorkspace().getVersionManager();
        final VersionHistory versionHistory = versionManager.getVersionHistory(targetNode.getPath());

        if (versionHistory.hasVersionLabel(CORE_BRANCH_ID + "-preview")) {
            // core branch present
            branches.add(CORE_BRANCH_ID);
        }

       for (String label : versionHistory.getVersionLabels()) {
           if (label.endsWith("-preview")) {
               final Version version = versionHistory.getVersionByLabel(label);
               final Node frozenNode = version.getFrozenNode();
               if (frozenNode.hasProperty(HIPPO_PROPERTY_BRANCH_ID)) {
                   // found a real branch instead of a label for a non-branch
                   branches.add(frozenNode.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
               }
           }
       }
        return branches;
    }

}
