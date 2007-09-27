/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.reviewedactions;

import java.util.Date;
import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowMappingException;

public interface ReviewedActionsWorkflow extends Workflow {
    /**
     * Request this editable copy of the document.
     */
    public void obtainEditableInstance()
        throws WorkflowException, WorkflowMappingException, RepositoryException, RemoteException;

    /**
     * Do away with the editable copy of the document which was previously
     * obtained.
     */
    public void disposeEditableInstance()
        throws WorkflowException, WorkflowMappingException, RepositoryException, RemoteException;

    /**
     * Immediate unpublication and deletion of document.
     * The current user must have authorization for this.
     */
    public void delete()
        throws WorkflowException, WorkflowMappingException, RepositoryException, RemoteException;

    /**
     * Request unpublication and deletion of document.
     */
    public void requestDeletion()
        throws WorkflowException, WorkflowMappingException, RepositoryException, RemoteException;

    /**
     * Immediate unpublication.
     * The current user must have authorization for this.
     */
    public void depublish()
        throws WorkflowException, WorkflowMappingException, RepositoryException, RemoteException;

    /**
     * Request unpublication.
     */
    public void requestDepublication()
        throws WorkflowException, WorkflowMappingException, RepositoryException, RemoteException;

    /**
     * Request unpublication at given date.
     */
    public void requestDepublication(Date publicationDate)
        throws WorkflowException, WorkflowMappingException, RepositoryException, RemoteException;

    /**
     * Immediate publication.
     * The current user must have authorization for this.
     */
    public void publish()
        throws WorkflowException, WorkflowMappingException, RepositoryException, RemoteException;

    /**
     * Publish at the requested date.
     * The current user must have authorization for this.
     */
    public void publish(Date publicationDate)
        throws WorkflowException, WorkflowMappingException, RepositoryException, RemoteException;

    /**
     * Publish at the requested date, and depublish at the requested second date
     * The current user must have authorization for this.
     */
    public void publish(Date publicationDate, Date unpublicationDate)
        throws WorkflowException, WorkflowMappingException, RepositoryException, RemoteException;

    /**
     * Request for this instance of the document to be published.
     */
    public void requestPublication()
        throws WorkflowException, WorkflowMappingException, RepositoryException, RemoteException;

    /**
     * Request for this instance of the document to be published at the given
     * date.
     */
    public void requestPublication(Date publicationDate)
        throws WorkflowException, WorkflowMappingException, RepositoryException, RemoteException;

    /**
     * Request for this instance of the document to be published at the given
     * date and to be scheduled for unpublication.
     */
    public void requestPublication(Date publicationDate, Date unpublicationDate)
        throws WorkflowException, WorkflowMappingException, RepositoryException, RemoteException;
}
