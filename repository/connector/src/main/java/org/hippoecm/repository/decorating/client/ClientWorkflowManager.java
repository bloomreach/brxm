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

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.rmi.client.ClientObject;
import org.apache.jackrabbit.rmi.client.RemoteRuntimeException;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.decorating.remote.RemoteWorkflowDescriptor;
import org.hippoecm.repository.decorating.remote.RemoteWorkflowManager;

public class ClientWorkflowManager extends ClientObject implements WorkflowManager {

    private Session session;
    private RemoteWorkflowManager remote;

    protected ClientWorkflowManager(Session session, RemoteWorkflowManager remote, LocalServicingAdapterFactory factory) {
        super(factory);
        this.session = session;
        this.remote = remote;
    }

    public Session getSession() throws RepositoryException {
        return session;
    }

    public WorkflowDescriptor getWorkflowDescriptor(String category, Node item) throws RepositoryException {
        try {
            RemoteWorkflowDescriptor remoteDescriptor = remote.getWorkflowDescriptor(category, item.getUUID());
            if (remoteDescriptor != null) {
                return new ClientWorkflowDescriptor(remoteDescriptor);
            } else {
                return null;
            }
        } catch(RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    public WorkflowDescriptor getWorkflowDescriptor(String category, Document document) throws RepositoryException {
        try {
            RemoteWorkflowDescriptor remoteDescriptor = remote.getWorkflowDescriptor(category, document.getIdentity());
            if (remoteDescriptor != null) {
                return new ClientWorkflowDescriptor(remoteDescriptor);
            } else {
                return null;
            }
        } catch(RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    public Workflow getWorkflow(String category, Node item) throws RepositoryException {
        try {
            return remote.getWorkflow(category, item.getPath());
        } catch(RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    public Workflow getWorkflow(String category, Document document) throws RepositoryException {
        try {
            return remote.getWorkflow(category, document);
        } catch(RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    public Workflow getWorkflow(WorkflowDescriptor descriptor) throws RepositoryException {
        try {
            ClientWorkflowDescriptor remoteDescriptor = (ClientWorkflowDescriptor) descriptor;
            return remoteDescriptor.remote.getWorkflow();
        } catch(RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

}
