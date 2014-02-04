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
public class FullReviewedActionsWorkflowImpl extends BasicReviewedActionsWorkflowImpl implements FullReviewedActionsWorkflow {

    public FullReviewedActionsWorkflowImpl() throws RemoteException {
    }

    @Override
    public void delete() throws WorkflowException, RepositoryException, RemoteException {
        documentWorkflow.delete();
    }

    @Override
    public void rename(final String newName) throws WorkflowException, RepositoryException, RemoteException {
        documentWorkflow.rename(newName);
    }

    @Override
    public void copy(final Document destination, final String newName) throws WorkflowException, RepositoryException, RemoteException {
        documentWorkflow.copy(destination, newName);
    }

    @Override
    public void move(final Document destination, final String newName) throws WorkflowException, RepositoryException, RemoteException {
        documentWorkflow.move(destination, newName);
    }

    @Override
    public void depublish() throws WorkflowException, RepositoryException, RemoteException {
        documentWorkflow.depublish();
    }

    @Override
    public void depublish(final Date depublicationDate) throws WorkflowException, RepositoryException, RemoteException {
        documentWorkflow.depublish(depublicationDate);
    }

    @Override
    public void publish() throws WorkflowException, RepositoryException, RemoteException {
        documentWorkflow.publish();
    }

    @Override
    public void publish(final Date publicationDate) throws WorkflowException, RepositoryException, RemoteException {
        documentWorkflow.publish(publicationDate);
    }

    @Override
    public void publish(final Date publicationDate, final Date unpublicationDate) throws WorkflowException, RepositoryException, RemoteException {
        documentWorkflow.publish(publicationDate, unpublicationDate);
    }

}
