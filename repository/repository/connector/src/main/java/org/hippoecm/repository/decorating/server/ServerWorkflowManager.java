/*
 *  Copyright 2008 Hippo.
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

import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.decorating.remote.RemoteWorkflowDescriptor;
import org.hippoecm.repository.decorating.remote.RemoteWorkflowManager;

public class ServerWorkflowManager extends ServerObject implements RemoteWorkflowManager {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private WorkflowManager workflowManager;

    public ServerWorkflowManager(WorkflowManager manager, RemoteServicingAdapterFactory factory) throws RemoteException {
        super(factory);
        this.workflowManager = manager;
    }

    public RemoteWorkflowDescriptor getWorkflowDescriptor(String category, String absPath) throws RepositoryException,
            RemoteException {
        try {
            String path = absPath;
            if (absPath.startsWith("/"))
                path = path.substring(1);
            Node node = workflowManager.getSession().getRootNode();
            if (!path.equals(""))
                node = node.getNode(path);
            WorkflowDescriptor descriptor = workflowManager.getWorkflowDescriptor(category, node);
            if (descriptor != null) {
                return new ServerWorkflowDescriptor(descriptor, workflowManager);
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
            return workflowManager.getWorkflow(category, node);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    public Workflow getWorkflow(RemoteWorkflowDescriptor descriptor) throws RepositoryException, RemoteException {
        try {
            ServerWorkflowDescriptor serverDescriptor = (ServerWorkflowDescriptor) descriptor;
            return workflowManager.getWorkflow(serverDescriptor.descriptor);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }
}
