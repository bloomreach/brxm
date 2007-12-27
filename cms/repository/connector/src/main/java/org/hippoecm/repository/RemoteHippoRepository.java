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
package org.hippoecm.repository;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.rmi.client.ClientRepositoryFactory;
import org.hippoecm.repository.servicing.client.ClientServicesAdapterFactory;

class RemoteHippoRepository extends HippoRepositoryImpl {
    private final static String SVN_ID = "$Id$";

    private Session clSession;
    private ClassLoader loader;

    public RemoteHippoRepository(String location) throws MalformedURLException, NotBoundException, RemoteException,
            RepositoryException {
        ClientServicesAdapterFactory adapterFactory = new ClientServicesAdapterFactory(this);
        ClientRepositoryFactory repositoryFactory = new ClientRepositoryFactory(adapterFactory);
        repository = repositoryFactory.getRepository(location);

        // Create own classloader, it does not make any sense to use the one at
        // the other end of the wire.
        clSession = repository.login();
        loader = new HippoRepositoryClassLoader(clSession);
    }

    public ClassLoader getClassLoader() {
        return loader;
    }
}
