/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.repository.reviewedactions;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.onehippo.repository.documentworkflow.HandleDocumentWorkflow;

/**
 * @deprecated since CMS 7.9, use/configure {@link org.onehippo.repository.handleworkflow.HandleDocumentWorkflowImpl} instead.
 */
@Deprecated
public class AbstractReviewedActionsWorkflow extends WorkflowImpl {

    protected HandleDocumentWorkflow handleDocumentWorkflow;

    /**
     * All implementations of a work-flow must provide a single, no-argument constructor.
     *
     * @throws java.rmi.RemoteException mandatory exception that must be thrown by all Remote objects
     */
    public AbstractReviewedActionsWorkflow() throws RemoteException {
    }

    // Workflow implementation / WorkflowImpl override

    @Override
    public void setNode(final Node node) throws RepositoryException {
        super.setNode(node);

        Node handleNode = getSubjectHandleNode();

        try {
            final Workflow handleWorkflow = getNonChainingWorkflowContext().getWorkflow("default", new Document(handleNode));
            if (!(handleWorkflow instanceof HandleDocumentWorkflow)) {
                throw new RepositoryException("Workflow on handle, in category 'document', is not a HandleDocumentWorkflow");
            }

            handleDocumentWorkflow = (HandleDocumentWorkflow) handleWorkflow;
        }
        catch (WorkflowException wfe) {
            if (wfe.getCause() != null && wfe.getCause() instanceof RepositoryException) {
                throw (RepositoryException)wfe.getCause();
            }
            throw new RepositoryException(wfe);
        }
    }

    protected Node getSubjectHandleNode() throws RepositoryException {
        Node parent = getNode().getParent();
        if (!parent.isNodeType(HippoNodeType.NT_HANDLE)) {
            throw new RepositoryException("Invalid workflow subject " + getNode().getPath() + ", does not have a handle as it's parent");
        }
        return parent;
    }

    @Override
    public Map<String, Serializable> hints() throws WorkflowException {
        Map<String, Serializable> hints = super.hints();
        hints.putAll(handleDocumentWorkflow.getInfo());
        hints.putAll(handleDocumentWorkflow.getActions());
        return hints;
    }

}
