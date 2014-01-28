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
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;

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
    public boolean isModified() throws WorkflowException, RepositoryException, RemoteException {
        return handleDocumentWorkflow.isModified();
    }

    @Override
    public Document obtainEditableInstance() throws RepositoryException, WorkflowException, RemoteException {
        return handleDocumentWorkflow.obtainEditableInstance();
    }

    @Override
    public Document commitEditableInstance() throws WorkflowException, RepositoryException, RemoteException {
        return handleDocumentWorkflow.commitEditableInstance();
    }

    @Override
    public Document disposeEditableInstance() throws WorkflowException, RepositoryException, RemoteException {
        return handleDocumentWorkflow.disposeEditableInstance();
    }

    // BasicReviewedActionsWorkflow implementation

    @Override
    public void requestDeletion() throws WorkflowException, RemoteException, MappingException {
        handleDocumentWorkflow.requestDeletion();
    }

    @Override
    public void requestDepublication() throws WorkflowException, RepositoryException, RemoteException {
        handleDocumentWorkflow.requestDepublication();
    }

    @Override
    public void requestDepublication(final Date publicationDate) throws WorkflowException, RepositoryException, RemoteException {
        handleDocumentWorkflow.requestDepublication(publicationDate);
    }

    @Override
    public void requestPublication() throws WorkflowException, RepositoryException, RemoteException {
        handleDocumentWorkflow.requestPublication();
    }

    @Override
    public void requestPublication(final Date publicationDate) throws WorkflowException, RepositoryException, RemoteException {
        handleDocumentWorkflow.requestPublication(publicationDate);
    }

    @Override
    public void requestPublication(final Date publicationDate, final Date unpublicationDate) throws WorkflowException, RepositoryException, RemoteException {
        handleDocumentWorkflow.requestPublication(publicationDate, unpublicationDate);
    }

    // FullReviewedActionsWorkflow implementation

    @Override
    public void delete() throws WorkflowException, RepositoryException, RemoteException {
        handleDocumentWorkflow.delete();
    }

    @Override
    public void rename(final String newName) throws WorkflowException, RepositoryException, RemoteException {
        handleDocumentWorkflow.rename(newName);
    }

    @Override
    public void copy(final Document destination, final String newName) throws WorkflowException, RemoteException, MappingException {
        handleDocumentWorkflow.copy(destination, newName);
    }

    @Override
    public void move(final Document destination, final String newName) throws WorkflowException, RepositoryException, RemoteException {
        handleDocumentWorkflow.move(destination, newName);
    }

    @Override
    public void depublish() throws WorkflowException, RepositoryException, RemoteException {
        handleDocumentWorkflow.depublish();
    }

    @Override
    public void depublish(final Date depublicationDate) throws WorkflowException, RepositoryException, RemoteException {
        handleDocumentWorkflow.depublish(depublicationDate);
    }

    @Override
    public void publish() throws WorkflowException, RepositoryException, RemoteException {
        handleDocumentWorkflow.publish();
    }

    @Override
    public void publish(final Date publicationDate) throws WorkflowException, RepositoryException, RemoteException {
        handleDocumentWorkflow.publish(publicationDate);
    }

    @Override
    public void publish(final Date publicationDate, final Date unpublicationDate) throws WorkflowException, RepositoryException, RemoteException {
        handleDocumentWorkflow.publish(publicationDate, unpublicationDate);
    }

}
