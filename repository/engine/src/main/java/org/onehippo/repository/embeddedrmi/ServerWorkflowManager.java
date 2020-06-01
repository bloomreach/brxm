/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.embeddedrmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import javax.jcr.RepositoryException;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;

public class ServerWorkflowManager extends UnicastRemoteObject implements RemoteWorkflowManager {
    WorkflowManager local;
    int port;

    public ServerWorkflowManager(WorkflowManager workflowManager, int port) throws RemoteException {
        local = workflowManager;
        this.port = port;
    }

    public RemoteWorkflowDescriptor getWorkflowDescriptor(String category, String identifier) throws MappingException, RepositoryException, RemoteException {
        return new RemoteWorkflowDescriptor(local.getWorkflowDescriptor(category, local.getSession().getNodeByIdentifier(identifier)), category, identifier);
    }

    @Override
    public Workflow getWorkflow(RemoteWorkflowDescriptor descriptor) throws MappingException, RepositoryException, RemoteException {
        Workflow workflow = local.getWorkflow(descriptor.category, local.getSession().getNodeByIdentifier(descriptor.identifier));
        try {
            UnicastRemoteObject.exportObject(workflow, port);
        } catch (RemoteException ex) {
            throw new RepositoryException("Problem creating workflow proxy", ex);
        }
        return workflow;
    }
    
}
