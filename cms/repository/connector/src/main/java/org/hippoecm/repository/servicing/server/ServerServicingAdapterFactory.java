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

import javax.jcr.Node;
import javax.jcr.Workspace;
import javax.jcr.Session;

import org.apache.jackrabbit.api.XASession;
import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.apache.jackrabbit.rmi.remote.RemoteSession;
import org.apache.jackrabbit.rmi.remote.RemoteWorkspace;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;

import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.servicing.remote.RemoteDocumentManager;
import org.hippoecm.repository.servicing.remote.RemoteServicingAdapterFactory;
import org.hippoecm.repository.servicing.remote.RemoteWorkflowManager;

public class ServerServicingAdapterFactory extends ServerAdapterFactory implements RemoteServicingAdapterFactory {
    public ServerServicingAdapterFactory() {
    }

    public RemoteSession getRemoteSession(Session session) throws RemoteException {
        if (session instanceof XASession) {
            return new ServerServicingXASession((XASession) session, this);
        } else {
            return new ServerServicingSession(session, this);
        }
    }

    public RemoteWorkspace getRemoteWorkspace(Workspace workspace) throws RemoteException {
        if (workspace instanceof HippoWorkspace)
            return new ServerServicingWorkspace((HippoWorkspace) workspace, this);
        else
            return super.getRemoteWorkspace(workspace);
    }

    public RemoteNode getRemoteNode(Node node) throws RemoteException {
        if (node instanceof HippoNode)
            return new ServerServicingNode((HippoNode) node, this);
        else
            return super.getRemoteNode(node);
    }

    public RemoteDocumentManager getRemoteDocumentManager(DocumentManager documentManager) throws RemoteException {
        return new ServerDocumentManager(documentManager, this);
    }

    public RemoteWorkflowManager getRemoteWorkflowManager(WorkflowManager workflowManager) throws RemoteException {
        return new ServerWorkflowManager(workflowManager, this);
    }
}
