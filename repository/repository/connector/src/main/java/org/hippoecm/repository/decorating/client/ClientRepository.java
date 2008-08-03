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

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.rmi.client.RemoteRepositoryException;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.rmi.client.ClientRepositoryService;
import org.hippoecm.repository.decorating.remote.RemoteRepository;

public class ClientRepository extends org.apache.jackrabbit.rmi.client.ClientRepository {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private RemoteRepository remote;

    public ClientRepository(RemoteRepository repository, LocalServicingAdapterFactory factory) {
        super(repository, factory);
        this.remote = repository;
    }

    public RepositoryService getRepositoryService() throws RepositoryException {
        try {
            return new ClientRepositoryService(remote.getRepositoryService());
        } catch(RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }
}
