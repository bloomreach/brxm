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
package org.hippoecm.repository.servicing.client;

import java.rmi.RemoteException;

import javax.jcr.Session;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.rmi.client.ClientWorkspace;
import org.apache.jackrabbit.rmi.client.RemoteRepositoryException;

import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.servicing.ServicesManager;
import org.hippoecm.repository.servicing.remote.RemoteServicingWorkspace;
import org.hippoecm.repository.servicing.remote.RemoteDocumentManager;
import org.hippoecm.repository.servicing.remote.RemoteServicesManager;
import org.hippoecm.repository.servicing.remote.RemoteWorkflowManager;

public class ClientServicingWorkspace extends ClientWorkspace implements HippoWorkspace {
    private Session session;
    private RemoteServicingWorkspace remote;

    public ClientServicingWorkspace(Session session, RemoteServicingWorkspace remote,
            LocalServicingAdapterFactory factory) {
        super(session, remote, factory);
        this.session = session;
        this.remote = remote;
    }

    public DocumentManager getDocumentManager() throws RepositoryException {
        try {
            RemoteDocumentManager manager = remote.getDocumentManager();
            return ((LocalServicingAdapterFactory) getFactory()).getDocumentManager(session, manager);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public ServicesManager getServicesManager() throws RepositoryException {
        try {
            RemoteServicesManager manager = remote.getServicesManager();
            return ((LocalServicingAdapterFactory) getFactory()).getServicesManager(session, manager);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public WorkflowManager getWorkflowManager() throws RepositoryException {
        try {
            RemoteWorkflowManager manager = remote.getWorkflowManager();
            return ((LocalServicingAdapterFactory) getFactory()).getWorkflowManager(session, manager);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }
}
