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

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.DocumentVariant;
import org.hippoecm.repository.util.JcrUtils;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME;

public class RestoreVersionToBranchTask extends AbstractDocumentTask {

    private Version version;
    private DocumentVariant target;

    public void setVersion(final Version version) {
        this.version = version;
    }

    public void setTarget(final DocumentVariant target) {
        this.target = target;
    }

    @Override
    protected Object doExecute() throws WorkflowException, RepositoryException, RemoteException {

        final Session workflowSession = getWorkflowContext().getInternalWorkflowSession();

        final Node targetNode = target.getNode(workflowSession);

        if (targetNode.isNodeType(HIPPO_MIXIN_BRANCH_INFO)) {
            final String branchId = targetNode.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString();
            final String branchName = targetNode.getProperty(HIPPO_PROPERTY_BRANCH_NAME).getString();
            workflowSession.getWorkspace().getVersionManager().restore(version, false);
            JcrUtils.ensureIsCheckedOut(targetNode);
            // branch info can have been removed as a result of the version restore, or can be of a different
            // branch, hence reset the right branch info
            targetNode.addMixin(HIPPO_MIXIN_BRANCH_INFO);
            targetNode.setProperty(HIPPO_PROPERTY_BRANCH_ID, branchId);
            targetNode.setProperty(HIPPO_PROPERTY_BRANCH_NAME, branchName);
        } else {

            workflowSession.getWorkspace().getVersionManager().restore(version, false);
            JcrUtils.ensureIsCheckedOut(targetNode);

            if (targetNode.isNodeType(HIPPO_MIXIN_BRANCH_INFO)) {

                // restore resulted in branch info but the original target didn't have branch info, hence remove it
                targetNode.getProperty(HIPPO_PROPERTY_BRANCH_ID).remove();
                targetNode.getProperty(HIPPO_PROPERTY_BRANCH_NAME).remove();
                targetNode.removeMixin(HIPPO_MIXIN_BRANCH_INFO);
            }
        }

        return new Document(targetNode);
    }
}
