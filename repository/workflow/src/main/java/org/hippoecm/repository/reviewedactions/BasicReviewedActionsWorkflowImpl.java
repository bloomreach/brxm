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

import java.rmi.RemoteException;
import java.util.Date;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;

/**
 * @deprecated since CMS 7.9, use/configure {@link org.onehippo.repository.documentworkflow.DocumentWorkflowImpl} instead.
 */
@Deprecated
public class BasicReviewedActionsWorkflowImpl extends AbstractReviewedActionsWorkflow implements BasicReviewedActionsWorkflow {

    public BasicReviewedActionsWorkflowImpl() throws RemoteException {
    }

    // EditableWorkflow implementation

    @Override
    public boolean isModified() throws WorkflowException, RepositoryException, RemoteException {
        return documentWorkflow.isModified();
    }

    @Override
    public Document obtainEditableInstance() throws RepositoryException, WorkflowException, RemoteException {
        return documentWorkflow.obtainEditableInstance();
    }

    @Override
    public Document commitEditableInstance() throws WorkflowException, RepositoryException, RemoteException {
        return documentWorkflow.commitEditableInstance();
    }

    @Override
    public Document disposeEditableInstance() throws WorkflowException, RepositoryException, RemoteException {
        return documentWorkflow.disposeEditableInstance();
    }

    // BasicReviewedActionsWorkflow implementation

    @Override
    public void requestDeletion() throws WorkflowException, RepositoryException, RemoteException {
        documentWorkflow.requestDeletion();
    }

    @Override
    public void requestDepublication() throws WorkflowException, RepositoryException, RemoteException {
        documentWorkflow.requestDepublication();
    }

    @Override
    public void requestDepublication(final Date publicationDate) throws WorkflowException, RepositoryException, RemoteException {
        documentWorkflow.requestDepublication(publicationDate);
    }

    @Override
    public void requestPublication() throws WorkflowException, RepositoryException, RemoteException {
        documentWorkflow.requestPublication();
    }

    @Override
    public void requestPublication(final Date publicationDate) throws WorkflowException, RepositoryException, RemoteException {
        documentWorkflow.requestPublication(publicationDate);
    }

    @Override
    public void requestPublication(final Date publicationDate, final Date unpublicationDate) throws WorkflowException, RepositoryException, RemoteException {
        documentWorkflow.requestPublication(publicationDate, unpublicationDate);
    }

}
