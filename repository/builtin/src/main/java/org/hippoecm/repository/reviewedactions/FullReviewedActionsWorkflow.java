/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.CopyWorkflow;

/**
 * @deprecated since CMS 7.9, use/configure {@link org.onehippo.repository.documentworkflow.DocumentWorkflow} instead.
 */
@Deprecated
public interface FullReviewedActionsWorkflow extends BasicReviewedActionsWorkflow, CopyWorkflow {

    /**
     * Immediate unpublication and deletion of document.
     * The current user must have authorization for this.
     */
    public void delete()
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Rename document.
     * The current user must have authorization for this.
     */
    public void rename(String newName)
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Immediate unpublication and rename document.
     * The current user must have authorization for this.
     */
    public void copy(Document target, String newName)
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Immediate unpublication and rename document.
     * The current user must have authorization for this.
     */
    public void move(Document target, String newName)
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Request unpublication and deletion of document.
     */
    public void requestDeletion()
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Immediate unpublication.
     * The current user must have authorization for this.
     */
    public void depublish()
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Immediate unpublication.
     * The current user must have authorization for this.
     */
    public void depublish(Date depublicationDate)
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Immediate publication.
     * The current user must have authorization for this.
     */
    public void publish()
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Publish at the requested date.
     * The current user must have authorization for this.
     */
    public void publish(Date publicationDate)
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

    /**
     * Publish at the requested date, and depublish at the requested second date
     * The current user must have authorization for this.
     */
    public void publish(Date publicationDate, Date unpublicationDate)
        throws WorkflowException, MappingException, RepositoryException, RemoteException;
}
