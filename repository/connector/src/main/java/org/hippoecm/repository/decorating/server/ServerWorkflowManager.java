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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.rmi.server.ServerObject;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.decorating.remote.RemoteWorkflowDescriptor;
import org.hippoecm.repository.decorating.remote.RemoteWorkflowManager;

public class ServerWorkflowManager extends ServerObject implements RemoteWorkflowManager {

    private WorkflowManager workflowManager;

    private ServerServicingAdapterFactory factory;

    protected ServerWorkflowManager(WorkflowManager manager, ServerServicingAdapterFactory factory) throws RemoteException {
        super(factory);
        this.factory = factory;
        this.workflowManager = manager;
    }

    public RemoteWorkflowDescriptor getWorkflowDescriptor(String category, String uuid) throws RepositoryException,
            RemoteException {
        try {
            Node node = workflowManager.getSession().getNodeByUUID(uuid);
            WorkflowDescriptor descriptor = workflowManager.getWorkflowDescriptor(category, node);
            if (descriptor != null) {
                return new ServerWorkflowDescriptor(factory, descriptor, workflowManager);
            } else {
                return null;
            }
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    public Workflow getWorkflow(String category, String absPath) throws RepositoryException, RemoteException {
        try {
            String path = absPath;
            if (absPath.startsWith("/"))
                path = path.substring(1);
            Node node = workflowManager.getSession().getRootNode();
            if (!path.equals(""))
                node = node.getNode(path);
            Workflow workflow = workflowManager.getWorkflow(category, node);
            if (workflow != null) {
                factory.export(workflow);
            }
            return workflow;
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    public Workflow getWorkflow(String category, Document document) throws RepositoryException, RemoteException {
        try {
            Workflow workflow = workflowManager.getWorkflow(category, document);
            if (workflow != null) {
                factory.export(workflow);
            }
            return workflow;
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    public Workflow getWorkflow(RemoteWorkflowDescriptor descriptor) throws RepositoryException, RemoteException {
        try {
            ServerWorkflowDescriptor serverDescriptor = (ServerWorkflowDescriptor) descriptor;
            Workflow workflow = workflowManager.getWorkflow(serverDescriptor.descriptor);
            if (workflow != null) {
                factory.export(workflow);
            }
            return workflow;
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }
}
