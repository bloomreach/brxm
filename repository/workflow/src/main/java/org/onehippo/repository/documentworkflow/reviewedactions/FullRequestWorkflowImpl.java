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
package org.onehippo.repository.documentworkflow.reviewedactions;

import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.reviewedactions.FullRequestWorkflow;

public class FullRequestWorkflowImpl extends AbstractReviewedActionsWorkflow implements FullRequestWorkflow {

    /**
     * All implementations of a work-flow must provide a single, no-argument constructor.
     *
     * @throws java.rmi.RemoteException mandatory exception that must be thrown by all Remote objects
     */
    public FullRequestWorkflowImpl() throws RemoteException {
    }

    // BasicRequestWorkflow implementation

    @Override
    public void cancelRequest() throws WorkflowException, RemoteException {
        try {
            handleDocumentWorkflow.cancelRequest(getNode());
        } catch (RepositoryException e) {
            throw new WorkflowException("Unable to create PublicationRequest from subject", e);
        }
    }

    // FullRequestWorkflow implementation

    @Override
    public void acceptRequest() throws WorkflowException, RemoteException {
        try {
            handleDocumentWorkflow.acceptRequest(getNode());
        } catch (RepositoryException e) {
            throw new WorkflowException("Unable to create PublicationRequest from subject", e);
        }
    }

    @Override
    public void rejectRequest(final String reason) throws WorkflowException, RemoteException {
        try {
            handleDocumentWorkflow.rejectRequest(getNode(), reason);
        } catch (RepositoryException e) {
            throw new WorkflowException("Unable to create PublicationRequest from subject", e);
        }
    }

}
