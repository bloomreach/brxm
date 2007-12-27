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
import java.rmi.server.UnicastRemoteObject;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.rmi.server.ServerObject;

import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.servicing.remote.RemoteServicingAdapterFactory;
import org.hippoecm.repository.servicing.remote.RemoteWorkflowDescriptor;

public class ServerWorkflowDescriptor extends UnicastRemoteObject implements RemoteWorkflowDescriptor {
    WorkflowManager manager;
    WorkflowDescriptor descriptor;

    public ServerWorkflowDescriptor(WorkflowDescriptor descriptor, WorkflowManager manager) throws RemoteException {
        super();
        this.descriptor = descriptor;
        this.manager = manager;
    }

    public String getDisplayName() throws RepositoryException {
        return descriptor.getDisplayName();
    }

    public String getRendererName() throws RepositoryException {
        return descriptor.getRendererName();
    }

    public Workflow getWorkflow() throws RepositoryException, RemoteException {
        return manager.getWorkflow(descriptor);
    }
}
