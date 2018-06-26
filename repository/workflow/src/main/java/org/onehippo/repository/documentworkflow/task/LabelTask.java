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

import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.DocumentVariant;


public class LabelTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private DocumentVariant unpublished;
    private String addLabel;
    private String onLabel;
    private String removeLabel;

    public DocumentVariant getUnpublished() {
        return unpublished;
    }

    public void setUnpublished(DocumentVariant variant) {
        this.unpublished = variant;
    }

    public void setAddLabel(final String addLabel) {
        this.addLabel = addLabel;
    }

    public void setOnLabel(final String onLabel) {
        this.onLabel = onLabel;
    }

    public void setRemoveLabel(final String removeLabel) {
        this.removeLabel = removeLabel;
    }

    @Override
    protected Object doExecute() throws WorkflowException, RepositoryException, RemoteException {
        if (getUnpublished() == null || !getUnpublished().hasNode()) {
            throw new WorkflowException("No unpublished variant provided");
        }

        if (addLabel == null && onLabel == null && removeLabel == null) {
            throw new WorkflowException("Specify what to do with labels");
        }

        final Session workflowSession = getWorkflowContext().getInternalWorkflowSession();
        Node targetNode = getUnpublished().getNode(workflowSession);

        final VersionManager versionManager = workflowSession.getWorkspace().getVersionManager();
        final VersionHistory versionHistory = versionManager.getVersionHistory(targetNode.getPath());

        if (removeLabel != null) {
            if (versionHistory.hasVersionLabel(removeLabel)) {
                versionHistory.removeVersionLabel(removeLabel);
            } else {
                throw new WorkflowException(String.format("Cannot remove label '%s' because it does not exist", removeLabel));
            }
        }

        if (addLabel == null && onLabel == null) {
            return null;
        }

        if (addLabel == null && onLabel != null) {
            throw new WorkflowException("It should be specified which label to add");
        }

        if (addLabel != null && onLabel == null) {
            throw new WorkflowException("It should be specified to which label the new label should be set");
        }


        if (!versionHistory.hasVersionLabel(onLabel)) {
            throw new WorkflowException(String.format("version label '%s' does not exist in version history so cannot " +
                    "add label '%s' to it.", onLabel, addLabel));
        }

        versionHistory.addVersionLabel(versionHistory.getVersionByLabel(onLabel).getName(), addLabel, true);
        return null;
    }
}
