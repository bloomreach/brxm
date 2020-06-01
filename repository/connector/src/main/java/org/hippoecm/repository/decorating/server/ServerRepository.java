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
package org.hippoecm.repository.decorating.server;

import java.rmi.RemoteException;

import javax.jcr.Repository;

import org.hippoecm.repository.decorating.remote.RemoteRepository;

public class ServerRepository extends org.apache.jackrabbit.rmi.server.ServerRepository implements RemoteRepository {

    private Repository repository;
    private RemoteServicingAdapterFactory factory;

    public ServerRepository(Repository repository, RemoteServicingAdapterFactory factory) throws RemoteException {
        super(repository, factory);
        this.repository = repository;
        this.factory = factory;
    }
}
