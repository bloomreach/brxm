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

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.apache.jackrabbit.rmi.client.ClientAdapterFactory;
import org.apache.jackrabbit.rmi.client.ClientSession;
import org.apache.jackrabbit.rmi.client.ClientXASession;
import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.apache.jackrabbit.rmi.remote.RemoteSession;
import org.apache.jackrabbit.rmi.remote.RemoteWorkspace;
import org.apache.jackrabbit.rmi.remote.RemoteXASession;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.servicing.remote.RemoteDocumentManager;
import org.hippoecm.repository.servicing.remote.RemoteServicingNode;
import org.hippoecm.repository.servicing.remote.RemoteServicingWorkspace;
import org.hippoecm.repository.servicing.remote.RemoteWorkflowManager;

public class ClientServicesAdapterFactory extends ClientAdapterFactory implements LocalServicingAdapterFactory {
    
    private HippoRepository repository;
    
    public ClientServicesAdapterFactory(HippoRepository repository) {
        this.repository = repository;
    }
    
    public ClassLoader getClassLoader() {
        return repository.getClassLoader();
    }
    
    public Workspace getWorkspace(Session session, RemoteWorkspace remote) {
        if (remote instanceof RemoteServicingWorkspace)
            return new ClientServicingWorkspace(session, (RemoteServicingWorkspace) remote, this);
        else
            return super.getWorkspace(session, remote);
    }

    public Node getNode(Session session, RemoteNode remote) {
        if (remote instanceof RemoteServicingNode)
            return new ClientServicingNode(session, (RemoteServicingNode) remote, this);
        else
            return super.getNode(session, remote);
    }

    public DocumentManager getDocumentManager(Session session, RemoteDocumentManager remote) {
        return new ClientDocumentManager(session, remote, this);
    }

    public WorkflowManager getWorkflowManager(Session session, RemoteWorkflowManager remote) {
        return new ClientWorkflowManager(session, remote, this);
    }
}
