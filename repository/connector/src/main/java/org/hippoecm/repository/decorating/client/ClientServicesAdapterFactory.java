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
package org.hippoecm.repository.decorating.client;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.query.Query;

import org.apache.jackrabbit.rmi.client.ClientAdapterFactory;
import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.apache.jackrabbit.rmi.remote.RemoteSession;
import org.apache.jackrabbit.rmi.remote.RemoteWorkspace;
import org.apache.jackrabbit.rmi.remote.RemoteXASession;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.decorating.remote.RemoteHierarchyResolver;
import org.hippoecm.repository.decorating.remote.RemoteRepository;
import org.hippoecm.repository.decorating.remote.RemoteServicingNode;
import org.hippoecm.repository.decorating.remote.RemoteServicingSession;
import org.hippoecm.repository.decorating.remote.RemoteServicingWorkspace;
import org.hippoecm.repository.decorating.remote.RemoteServicingXASession;
import org.hippoecm.repository.decorating.remote.RemoteWorkflowManager;

public class ClientServicesAdapterFactory extends ClientAdapterFactory implements LocalServicingAdapterFactory {

    public ClientServicesAdapterFactory() {
    }

    @Override
    public Repository getRepository(org.apache.jackrabbit.rmi.remote.RemoteRepository remote) {
        return new ClientRepository((RemoteRepository)remote, this);
    }

    @Override
    public Session getSession(Repository repository, RemoteSession remote) {
        if (remote instanceof RemoteXASession) {
            return new ClientServicingXASession(repository, (RemoteServicingXASession) remote, this);
        } else {
            return new ClientServicingSession(repository, (RemoteServicingSession) remote, this);
        }
     }

    @Override
    public Workspace getWorkspace(Session session, RemoteWorkspace remote) {
        if (remote instanceof RemoteServicingWorkspace)
            return new ClientServicingWorkspace(session, (RemoteServicingWorkspace) remote, this);
        else
            return super.getWorkspace(session, remote);
    }

    @Override
    public Node getNode(Session session, RemoteNode remote) {
        if (remote instanceof RemoteServicingNode)
            return new ClientServicingNode(session, (RemoteServicingNode) remote, this);
        else
            return super.getNode(session, remote);
    }

    public WorkflowManager getWorkflowManager(Session session, RemoteWorkflowManager remote) {
        return new ClientWorkflowManager(session, remote, this);
    }

    public HierarchyResolver getHierarchyResolver(Session session, RemoteHierarchyResolver remote) {
        return new ClientHierarchyResolver(session, remote, this);
    }

    @Override
    public Query getQuery(Session session, org.apache.jackrabbit.rmi.remote.RemoteQuery remote) {
        return new ClientQuery(session, (org.hippoecm.repository.decorating.remote.RemoteQuery) remote, this);
    }
}
