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
import java.util.Date;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
import org.onehippo.repository.documentworkflow.DocumentCopyMovePayload;

public class FullReviewedActionsWorkflowImpl extends AbstractReviewedActionsWorkflow implements FullReviewedActionsWorkflow {

    /**
     * All implementations of a work-flow must provide a single, no-argument constructor.
     *
     * @throws java.rmi.RemoteException mandatory exception that must be thrown by all Remote objects
     */
    public FullReviewedActionsWorkflowImpl() throws RemoteException {
    }

    // EditableWorkflow implementation

    @Override
    public boolean isModified() throws WorkflowException, RepositoryException {
        return (Boolean) handleDocumentWorkflow.triggerAction("checkModified");
    }

    @Override
    public Document obtainEditableInstance() throws RepositoryException, WorkflowException {
        return workflowResultToUserDocument(handleDocumentWorkflow.triggerAction("obtainEditableInstance"));
    }

    @Override
    public Document commitEditableInstance() throws WorkflowException, RepositoryException {
        return workflowResultToUserDocument(handleDocumentWorkflow.triggerAction("commitEditableInstance"));
    }

    @Override
    public Document disposeEditableInstance() throws WorkflowException, RepositoryException {
        return workflowResultToUserDocument(handleDocumentWorkflow.triggerAction("disposeEditableInstance"));
    }

    // BasicReviewedActionsWorkflow implementation

    @Override
    public void requestDeletion() throws WorkflowException {
        handleDocumentWorkflow.triggerAction("requestDelete");
    }

    @Override
    public void requestDepublication() throws WorkflowException {
        handleDocumentWorkflow.triggerAction("requestDepublish");
    }

    @Override
    public void requestDepublication(final Date publicationDate) throws WorkflowException {
        handleDocumentWorkflow.triggerAction("requestDepublish", publicationDate);
    }

    @Override
    public void requestPublication() throws WorkflowException {
        handleDocumentWorkflow.triggerAction("publish");
    }

    @Override
    public void requestPublication(final Date publicationDate) throws WorkflowException {
        handleDocumentWorkflow.triggerAction("requestPublish", publicationDate);
    }

    @Override
    public void requestPublication(final Date publicationDate, final Date unpublicationDate) throws WorkflowException {
        throw new WorkflowException("unsupported");
    }

    // FullReviewedActionsWorkflow implementation

    @Override
    public void delete() throws WorkflowException {
        handleDocumentWorkflow.triggerAction("delete");
    }

    @Override
    public void rename(final String newName) throws WorkflowException {
        handleDocumentWorkflow.triggerAction("rename", newName);
    }

    @Override
    public void copy(final Document destination, final String newName) throws WorkflowException {
        handleDocumentWorkflow.triggerAction("copy", new DocumentCopyMovePayload(destination, newName));
    }

    @Override
    public void move(final Document destination, final String newName) throws WorkflowException {
        handleDocumentWorkflow.triggerAction("move", new DocumentCopyMovePayload(destination, newName));
    }

    @Override
    public void depublish() throws WorkflowException {
        handleDocumentWorkflow.triggerAction("depublish");
    }

    @Override
    public void depublish(final Date depublicationDate) throws WorkflowException {
        handleDocumentWorkflow.triggerAction("depublish", depublicationDate);
    }

    @Override
    public void publish() throws WorkflowException {
        handleDocumentWorkflow.triggerAction("publish");
    }

    @Override
    public void publish(final Date publicationDate) throws WorkflowException {
        handleDocumentWorkflow.triggerAction("publish", publicationDate);
    }

    @Override
    public void publish(final Date publicationDate, final Date unpublicationDate) throws WorkflowException {
        throw new WorkflowException("unsupported");
    }

}
