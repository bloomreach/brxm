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
package org.hippoecm.repository.decorating.server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.jackrabbit.api.XASession;
import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.apache.jackrabbit.rmi.remote.RemoteQuery;
import org.apache.jackrabbit.rmi.remote.RemoteQueryManager;
import org.apache.jackrabbit.rmi.remote.RemoteSession;
import org.apache.jackrabbit.rmi.remote.RemoteWorkspace;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;
import org.hippoecm.repository.RepositoryUrl;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoQuery;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.decorating.remote.RemoteHierarchyResolver;
import org.hippoecm.repository.decorating.remote.RemoteRepository;
import org.hippoecm.repository.decorating.remote.RemoteWorkflowManager;

public class ServerServicingAdapterFactory extends ServerAdapterFactory implements RemoteServicingAdapterFactory {

    private RepositoryUrl location;

    public ServerServicingAdapterFactory(RepositoryUrl location) {
        this.location = location;
    }

    @Override
    public RemoteRepository getRemoteRepository(Repository repository) throws RemoteException {
        return new ServerRepository(repository, this);
    }

    @Override
    public RemoteSession getRemoteSession(Session session) throws RemoteException {
        if (session instanceof XASession) {
            return new ServerServicingXASession((XASession) session, ((XASession)session).getXAResource(), this);
        } else {
            return new ServerServicingSession(session, this);
        }
    }

    @Override
    public RemoteWorkspace getRemoteWorkspace(Workspace workspace) throws RemoteException {
        if (workspace instanceof HippoWorkspace)
            return new ServerServicingWorkspace((HippoWorkspace) workspace, this);
        else
            return super.getRemoteWorkspace(workspace);
    }

    @Override
    public RemoteNode getRemoteNode(Node node) throws RemoteException {
        if (node instanceof HippoNode)
            return new ServerServicingNode((HippoNode) node, this);
        else
            return super.getRemoteNode(node);
    }

    public RemoteWorkflowManager getRemoteWorkflowManager(WorkflowManager workflowManager) throws RemoteException {
        return new ServerWorkflowManager(workflowManager, this);
    }

    public RemoteQueryManager getRemoteQueryManager(QueryManager manager, Session session) throws RemoteException {
        return new ServerQueryManager(manager, this, session);
    }

    @Override
    public RemoteQuery getRemoteQuery(Query query) throws RemoteException {
        return new ServerQuery((HippoQuery)query, this);
    }

    public RemoteHierarchyResolver getRemoteHierarchyResolver(HierarchyResolver hierarchyResolver, Session session)
        throws RemoteException {
        return new ServerHierarchyResolver(hierarchyResolver, this, session);
    }

    void export(Workflow workflow) throws RepositoryException {
        /*
         * The following statement will fail under Java4, and requires Java5 and NO stub
         * generation (through rmic).
         *
         * This code here, where we use a proxy to wrap a workflow class, is to have control
         * before and after each call to a workflow.  This in order to automatically persist
         * changes made by the workflow, and let the workflow operate in a different session.
         * This requires intercepting each call to a workflow, which is exactly where auto-
         * generated proxy classes are good for.
         * However Proxy classes and RMI stub generated are not integrated in Java4.
         *
         * The reason for the failure is that the exportObject in Java4 will lookup the stub for
         * the proxy class generated above.  We cannot however beforehand generate the stub for
         * the proxy class, as these are generated on the fly.  We can also not use the stub of
         * the original workflow, as then we would bypass calling the proxy class.  This is
         * because the classname of the exported object must match the name of the stub class
         * being looked up.
         *
         * A labor-intensive solution, to be developed if really needed, is to perform an exportObject
         * on the original workflow (pre-wrapping it with a proxy).  But then modifying the stub
         * generated by rmic, not to call the workflow directly, but call the proxy class.
         * This solution is labor-intensive, hard to explain, and negates the easy to implement
         * workflows as they are now.  So if this route is the route to go, we would be better off
         * writing our own rmic, which performs this automatically.
         */
        try {
            UnicastRemoteObject.exportObject(workflow, location.getPort());
        } catch (RemoteException ex) {
            throw new RepositoryException("Problem creating workflow proxy", ex);
        }
    }
}
