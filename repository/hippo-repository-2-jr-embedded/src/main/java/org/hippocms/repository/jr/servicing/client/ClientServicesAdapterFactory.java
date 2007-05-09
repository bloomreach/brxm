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
package org.hippocms.repository.jr.servicing.client;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.Node;

import org.apache.jackrabbit.rmi.client.ClientAdapterFactory;
import org.apache.jackrabbit.rmi.remote.RemoteSession;
import org.apache.jackrabbit.rmi.remote.RemoteWorkspace;
import org.apache.jackrabbit.rmi.remote.RemoteNode;

import org.hippocms.repository.jr.servicing.ServicesManager;
import org.hippocms.repository.jr.servicing.remote.RemoteServicesManager;
import org.hippocms.repository.jr.servicing.remote.RemoteServicingSession;
import org.hippocms.repository.jr.servicing.remote.RemoteServicingWorkspace;
import org.hippocms.repository.jr.servicing.remote.RemoteServicingNode;

public class ClientServicesAdapterFactory extends ClientAdapterFactory implements LocalServicingAdapterFactory {
    public Session getSession(Repository repository, RemoteSession remote) {
        if (remote instanceof RemoteServicingSession)
            return new ClientServicingSession(repository, (RemoteServicingSession) remote, this);
        else
            return super.getSession(repository, remote);
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

    public ServicesManager getServicesManager(Session session, RemoteServicesManager remote) {
        return new ClientServicesManager(session, remote, this);
    }
}
