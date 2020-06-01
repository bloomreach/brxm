/**
 * Copyright 2013-2020 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.onehippo.repository.util.JcrConstants;

import static org.hippoecm.repository.HippoStdNodeType.DRAFT;

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

        // a copy *to* or from draft should ignore direct children below the variant of nodetype hippo:skipdraft
        final boolean srcOrDestIsDraft = DRAFT.equals(getSourceState()) || DRAFT.equals(getTargetState());

        DocumentVariant sourceDoc = dm.getDocuments().get(getSourceState());
        DocumentVariant targetDoc = dm.getDocuments().get(getTargetState());

        if (sourceDoc == null || !sourceDoc.hasNode()) {
            throw new WorkflowException("Source document variant (node) is not available.");
        }

        final Session workflowSession = getWorkflowContext().getInternalWorkflowSession();
        final Node sourceNode = sourceDoc.getNode(workflowSession);
        Node targetNode;

        if (targetDoc == null) {

            final Node parent = sourceNode.getParent();
            JcrUtils.ensureIsCheckedOut(parent);

            targetNode = parent.addNode(sourceNode.getName(), sourceNode.getPrimaryNodeType().getName());

            targetNode.addMixin(HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT);

            if (sourceNode.isNodeType(HippoStdNodeType.NT_PUBLISHABLESUMMARY)) {
                targetNode.addMixin(HippoStdNodeType.NT_PUBLISHABLESUMMARY);
            }

            if (DRAFT.equals(getTargetState()) && targetNode.isNodeType(HippoNodeType.NT_HARDDOCUMENT)) {
                targetNode.removeMixin(HippoNodeType.NT_HARDDOCUMENT);
            }
            targetNode.addMixin(JcrConstants.MIX_REFERENCEABLE);

            copyTo(sourceNode, targetNode, srcOrDestIsDraft);

            targetDoc = new DocumentVariant(targetNode);
            targetDoc.setState(getTargetState());

            workflowSession.save();
            if (dm.hasMultipleDocumentVariants(getTargetState())) {
                deleteDuplicateVariant(workflowSession, dm, targetDoc, getTargetState());
            }
        }
        else {
            targetNode = targetDoc.getNode(workflowSession);

            if (sourceNode.isSame(targetNode)) {
                throw new WorkflowException(
                        "The target document variant (node) is the same as the source document variant (node).");
            }
            copyTo(sourceNode, targetNode, srcOrDestIsDraft);
        }

        dm.getDocuments().put(targetDoc.getState(), targetDoc);

        return null;
    }

    /**
     * Remove accidentally duplicated (or even more!) same state variant.
     * Method needs to be static synchronized as well as use a separate impersonated session to prevent repository
     * internal state corruption when two (or more!) threads do this concurrently for the same variant handle
     * (corruption likely occurring because the document variants being same-name-siblings).
     * For further reference see: REPO-1386
     * @throws WorkflowException when this thread still finds a duplicate same state variant, after having deleted the variant
     */
    private static synchronized void deleteDuplicateVariant(Session session, DocumentHandle dm, DocumentVariant variant, String state)
            throws RepositoryException, WorkflowException {
        boolean fail = false;
        final Session deleteSession = session.impersonate(new SimpleCredentials(session.getUserID(), new char[]{}));
        try {
            if (dm.hasMultipleDocumentVariants(state)) {
                fail = true;
                variant.getNode(deleteSession).remove();
                deleteSession.save();
            }
        } finally {
            deleteSession.logout();
        }
        if (fail) {
            throw new WorkflowException("Concurrent workflow action detected");
        }
    }
}
