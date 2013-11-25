/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.documentworkflow;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;

public class DocumentWorkflowImpl extends WorkflowImpl implements DocumentWorkflow {

    public DocumentWorkflowImpl() throws RemoteException {
    }

    // Workflow implementation / WorkflowImpl override

    @Override
    public Map<String,Serializable> hints() {
        return null;
    }

    // EditableWorkflow implementation

    @Override
    public Document obtainEditableInstance() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        return null;
    }

    @Override
    public Document commitEditableInstance() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        return null;
    }

    @Override
    public Document disposeEditableInstance() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        return null;
    }

    // BasicReviewedActionsWorkflow implementation

    @Override
    public void requestDepublication() throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }

    @Override
    public void requestDepublication(final Date publicationDate) throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }

    @Override
    public void requestPublication() throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }

    @Override
    public void requestPublication(final Date publicationDate) throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }

    @Override
    public void requestPublication(final Date publicationDate, final Date unpublicationDate) throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }

    // FullReviewedActionsWorkflow implementation

    @Override
    public void delete() throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }

    @Override
    public void rename(final String newName) throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }

    @Override
    public void copy(final Document target, final String newName) throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }

    @Override
    public void move(final Document target, final String newName) throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }

    @Override
    public void depublish() throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }

    @Override
    public void depublish(final Date depublicationDate) throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }

    @Override
    public void publish() throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }

    @Override
    public void publish(final Date publicationDate) throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }

    @Override
    public void publish(final Date publicationDate, final Date unpublicationDate) throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }

    // BasicRequestWorkflow implementation

    @Override
    public void cancelRequest() throws WorkflowException, RepositoryException, RemoteException {
    }

    @Override
    public void requestDeletion() throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }

    // FullRequestWorkflow implementation

    @Override
    public void acceptRequest() throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }

    @Override
    public void rejectRequest(final String reason) throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }

    // UnlockWorkflow implementation

    @Override
    public void unlock() throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }

    // VersionWorkflow implementation

    @Override
    public Document version() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        return null;
    }

    @Override
    public Document revert(final Calendar historic) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        return null;
    }

    @Override
    public Document restoreTo(final Document target) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        return null;
    }

    @Override
    public Document restore(final Calendar historic) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        return null;
    }

    @Override
    public Document restore(final Calendar historic, final Map<String, String[]> replacements) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        return null;
    }

    @Override
    public SortedMap<Calendar, Set<String>> list() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        return null;
    }

    @Override
    public Document retrieve(final Calendar historic) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        return null;
    }
}
