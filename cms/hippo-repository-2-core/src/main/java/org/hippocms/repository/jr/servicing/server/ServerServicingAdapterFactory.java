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
package org.hippocms.repository.jr.servicing.server;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.apache.jackrabbit.rmi.remote.RemoteSession;
import org.apache.jackrabbit.rmi.remote.RemoteWorkspace;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;
import org.hippocms.repository.jr.servicing.DocumentManager;
import org.hippocms.repository.jr.servicing.ServicesManager;
import org.hippocms.repository.jr.servicing.ServicingNodeImpl;
import org.hippocms.repository.jr.servicing.ServicingSessionImpl;
import org.hippocms.repository.jr.servicing.ServicingWorkspaceImpl;
import org.hippocms.repository.jr.servicing.WorkflowManager;
import org.hippocms.repository.jr.servicing.remote.RemoteDocumentManager;
import org.hippocms.repository.jr.servicing.remote.RemoteServicesManager;
import org.hippocms.repository.jr.servicing.remote.RemoteServicingAdapterFactory;
import org.hippocms.repository.jr.servicing.remote.RemoteWorkflowManager;

public class ServerServicingAdapterFactory extends ServerAdapterFactory implements RemoteServicingAdapterFactory {
    public ServerServicingAdapterFactory() {
    }

    public RemoteSession getRemoteSession(Session session) throws RemoteException {
        if (session instanceof ServicingSessionImpl)
            return new ServerServicingSession((ServicingSessionImpl) session, this);
        else
            return super.getRemoteSession(session);
    }

    public RemoteWorkspace getRemoteWorkspace(Workspace workspace) throws RemoteException {
        if (workspace instanceof ServicingWorkspaceImpl)
            return new ServerServicingWorkspace((ServicingWorkspaceImpl) workspace, this);
        else
            return super.getRemoteWorkspace(workspace);
    }

    public RemoteNode getRemoteNode(Node node) throws RemoteException {
        if (node instanceof ServicingNodeImpl)
            return new ServerServicingNode((ServicingNodeImpl) node, this);
        else
            return super.getRemoteNode(node);
    }

    public RemoteDocumentManager getRemoteDocumentManager(DocumentManager documentManager) throws RemoteException {
        return new ServerDocumentManager(documentManager, this);
    }

    public RemoteServicesManager getRemoteServicesManager(ServicesManager servicesManager) throws RemoteException {
        return new ServerServicesManager(servicesManager, this);
    }

    public RemoteWorkflowManager getRemoteWorkflowManager(WorkflowManager workflowManager) throws RemoteException {
        return new ServerWorkflowManager(workflowManager, this);
    }
}
