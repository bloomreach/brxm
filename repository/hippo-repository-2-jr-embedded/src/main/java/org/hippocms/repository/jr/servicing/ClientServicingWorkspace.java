/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippocms.repository.jr.servicing;

import java.rmi.RemoteException;

import javax.jcr.Session;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.rmi.client.ClientWorkspace;
import org.apache.jackrabbit.rmi.client.RemoteRepositoryException;

public class ClientServicingWorkspace extends ClientWorkspace
  implements ServicingWorkspace
{
    private Session session;
    private RemoteServicingWorkspace remote;
    public ClientServicingWorkspace(Session session, RemoteServicingWorkspace remote,
                                    LocalServicingAdapterFactory factory) {
        super(session, remote, factory);
        this.session = session;
        this.remote = remote;
    }
    public ServicesManager getServicesManager() throws RepositoryException {
        try {
            RemoteServicesManager manager = remote.getServicesManager();
            return ((LocalServicingAdapterFactory)getFactory()).getServicesManager(session, manager);
        } catch(RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }
}
