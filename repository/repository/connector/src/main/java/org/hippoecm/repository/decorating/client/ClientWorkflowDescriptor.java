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
package org.hippoecm.repository.decorating.client;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.rmi.client.ClientObject;
import org.apache.jackrabbit.rmi.client.RemoteRepositoryException;

import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.decorating.remote.RemoteWorkflowDescriptor;

public class ClientWorkflowDescriptor extends UnicastRemoteObject implements WorkflowDescriptor {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    RemoteWorkflowDescriptor remote;

    public ClientWorkflowDescriptor(RemoteWorkflowDescriptor remote) throws RemoteException {
        super();
        this.remote = remote;
    }

    public String getDisplayName() throws RepositoryException {
        try {
            return remote.getDisplayName();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public String getRendererName() throws RepositoryException {
        try {
            return remote.getRendererName();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }
}
