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
package org.hippoecm.repository.jr.servicing.server;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.rmi.server.ServerObject;

import org.hippoecm.repository.jr.servicing.Service;
import org.hippoecm.repository.jr.servicing.ServicesManager;
import org.hippoecm.repository.jr.servicing.remote.RemoteServicesManager;
import org.hippoecm.repository.jr.servicing.remote.RemoteServicingAdapterFactory;

public class ServerServicesManager extends ServerObject implements RemoteServicesManager {
    private ServicesManager servicesManager;

    public ServerServicesManager(ServicesManager manager, RemoteServicingAdapterFactory factory) throws RemoteException {
        super(factory);
        this.servicesManager = manager;
    }

    public Service getService(String absPath) throws RepositoryException, RemoteException {
        try {
            absPath = absPath.substring(1); // skip leading slash
            Node node = servicesManager.getSession().getRootNode().getNode(absPath);
            return servicesManager.getService(node);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    public Service getService(String absPath, String serviceName) throws RepositoryException, RemoteException {
        try {
            Node node = servicesManager.getSession().getRootNode().getNode(absPath);
            return servicesManager.getService(node, serviceName);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }
}
