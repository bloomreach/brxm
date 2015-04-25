/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.onehippo.repository.util.JcrConstants;

/**
 * Custom workflow task for copying (creating if necessary) from one variant node to another variant node
 */
public class CopyVariantTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private String sourceState;
    private String targetState;

    public String getSourceState() {
        return sourceState;
    }

    public void setSourceState(String sourceState) {
        this.sourceState = sourceState;
    }

    public String getTargetState() {
        return targetState;
    }

    public void setTargetState(String targetState) {
        this.targetState = targetState;
    }

    @Override
    public Object doExecute() throws WorkflowException, RepositoryException, RemoteException {

        DocumentHandle dm = getDocumentHandle();

        DocumentVariant sourceDoc = dm.getDocuments().get(getSourceState());
        DocumentVariant targetDoc = dm.getDocuments().get(getTargetState());

        if (sourceDoc == null || !sourceDoc.hasNode()) {
            throw new WorkflowException("Source document variant (node) is not available.");
        }

        final Session workflowSession = getWorkflowContext().getInternalWorkflowSession();
        final Node sourceNode = sourceDoc.getNode(workflowSession);
        Node targetNode;

        boolean saveNeeded = false;

        if (targetDoc == null) {
            saveNeeded = true;
            targetNode = cloneDocumentNode(sourceNode);

            if (HippoStdNodeType.DRAFT.equals(getTargetState())) {
                if (targetNode.isNodeType(HippoNodeType.NT_HARDDOCUMENT)) {
                    targetNode.removeMixin(HippoNodeType.NT_HARDDOCUMENT);
                }
            }
            if (!targetNode.isNodeType(JcrConstants.MIX_REFERENCEABLE)) {
                targetNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
            }

            targetDoc = new DocumentVariant(targetNode);
            targetDoc.setState(getTargetState());
        }
        else {
            targetNode = targetDoc.getNode(workflowSession);

            if (sourceNode.isSame(targetNode)) {
                throw new WorkflowException(
                        "The target document variant (node) is the same as the source document variant (node).");
            }
            copyTo(sourceNode, targetNode);
        }

        if (saveNeeded) {
            workflowSession.save();
        }

        dm.getDocuments().put(targetDoc.getState(), targetDoc);

        return null;
    }
}
