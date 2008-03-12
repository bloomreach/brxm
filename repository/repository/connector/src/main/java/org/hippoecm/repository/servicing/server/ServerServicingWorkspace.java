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
package org.hippoecm.repository.servicing.server;

import java.rmi.RemoteException;

import javax.jcr.RepositoryException;
import javax.jcr.query.QueryManager;

import org.apache.jackrabbit.rmi.remote.RemoteQueryManager;
import org.apache.jackrabbit.rmi.server.ServerWorkspace;

import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.servicing.remote.RemoteDocumentManager;
import org.hippoecm.repository.servicing.remote.RemoteServicingAdapterFactory;
import org.hippoecm.repository.servicing.remote.RemoteServicingWorkspace;
import org.hippoecm.repository.servicing.remote.RemoteWorkflowManager;

public class ServerServicingWorkspace extends ServerWorkspace implements RemoteServicingWorkspace {
    private HippoWorkspace workspace;

    public ServerServicingWorkspace(HippoWorkspace workspace, RemoteServicingAdapterFactory factory)
            throws RemoteException {
        super(workspace, factory);
        this.workspace = workspace;
    }

    public RemoteDocumentManager getDocumentManager() throws RemoteException, RepositoryException {
        try {
            DocumentManager documentManager = workspace.getDocumentManager();
            return ((RemoteServicingAdapterFactory) getFactory()).getRemoteDocumentManager(documentManager);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    public RemoteWorkflowManager getWorkflowManager() throws RemoteException, RepositoryException {
        try {
            WorkflowManager workflowManager = workspace.getWorkflowManager();
            return ((RemoteServicingAdapterFactory) getFactory()).getRemoteWorkflowManager(workflowManager);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    public RemoteQueryManager getQueryManager() throws RepositoryException, RemoteException {
        try {
            QueryManager queryManager = workspace.getQueryManager();
            return ((RemoteServicingAdapterFactory)getFactory()).getRemoteQueryManager(queryManager, workspace.getSession());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }
}
