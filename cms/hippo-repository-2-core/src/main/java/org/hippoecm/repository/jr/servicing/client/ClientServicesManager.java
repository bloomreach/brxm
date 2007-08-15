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
package org.hippoecm.repository.jr.servicing.client;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.rmi.client.ClientObject;
import org.apache.jackrabbit.rmi.client.RemoteRuntimeException;
import org.hippoecm.repository.jr.servicing.Service;
import org.hippoecm.repository.jr.servicing.ServicesManager;
import org.hippoecm.repository.jr.servicing.remote.RemoteServicesManager;

public class ClientServicesManager extends ClientObject implements ServicesManager {
    private Session session;
    private RemoteServicesManager remote;

    public ClientServicesManager(Session session, RemoteServicesManager remote, LocalServicingAdapterFactory factory) {
        super(factory);
        this.session = session;
        this.remote = remote;
    }

    public Service getService(Node node) throws RepositoryException {
        try {
            return remote.getService(node.getPath());
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    public Service getService(Node node, String serviceName) throws RepositoryException {
        try {
            return remote.getService(node.getPath(), serviceName);
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    public Session getSession() throws RepositoryException {
        return session;
    }

    public void save() throws RepositoryException {
    }
}
