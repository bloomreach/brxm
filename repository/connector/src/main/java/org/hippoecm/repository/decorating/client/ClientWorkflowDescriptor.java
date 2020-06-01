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

import java.lang.reflect.Array;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.rmi.client.RemoteRepositoryException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.decorating.remote.RemoteWorkflowDescriptor;

public class ClientWorkflowDescriptor extends UnicastRemoteObject implements WorkflowDescriptor {

    RemoteWorkflowDescriptor remote;

    protected ClientWorkflowDescriptor(RemoteWorkflowDescriptor remote) throws RemoteException {
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

    public String getAttribute(String name) throws RepositoryException {
        try {
            return remote.getAttribute(name);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public Map<String,Serializable> hints() throws RepositoryException {
        try {
            return remote.hints();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public Class<Workflow>[] getInterfaces() throws ClassNotFoundException, RepositoryException {
        try {
            String[] classes = remote.getInterfaces();
            Class<Workflow>[] interfaces = (Class<Workflow>[]) Array.newInstance(Class.class, classes.length);
            for(int i=0; i<classes.length; i++) {
                interfaces[i] = (Class<Workflow>) Class.forName(classes[i]);
            }
            return interfaces;
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }
}
