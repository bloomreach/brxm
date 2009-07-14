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
package org.hippoecm.repository;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;
import javax.jcr.Repository;

import org.apache.jackrabbit.jcr2spi.RepositoryImpl;
import org.apache.jackrabbit.jcr2spi.config.RepositoryConfig;
import org.apache.jackrabbit.rmi.client.ClientRepositoryFactory;
import org.apache.jackrabbit.rmi.client.SafeClientRepository;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.rmi.client.ClientRepositoryService;

import org.hippoecm.repository.decorating.client.ClientServicesAdapterFactory;
import org.hippoecm.repository.decorating.remote.RemoteRepository;
import org.hippoecm.repository.decorating.spi.DecoratorFactoryImpl;

class SPIHippoRepository extends HippoRepositoryImpl {
    RemoteRepository remoteRepository;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public SPIHippoRepository(String location) throws MalformedURLException, NotBoundException, RemoteException, RepositoryException {
        final ClientServicesAdapterFactory adapterFactory = new ClientServicesAdapterFactory();
        @SuppressWarnings("unused")
        ClientRepositoryFactory repositoryFactory = new ClientRepositoryFactory(adapterFactory) {
            @Override
            public Repository getRepository(final String url) throws MalformedURLException, NotBoundException, ClassCastException, RemoteException {
                return new SafeClientRepository(adapterFactory) {
                    protected org.apache.jackrabbit.rmi.remote.RemoteRepository getRemoteRepository()
                            throws RemoteException {
                        try {
                            remoteRepository = (RemoteRepository) Naming.lookup(url);
                            return remoteRepository;
                        } catch (MalformedURLException e) {
                            throw new RemoteException("Malformed URL: " + url, e);
                        } catch (NotBoundException e) {
                            throw new RemoteException("No target found: " + url, e);
                        } catch (ClassCastException e) {
                            throw new RemoteException("Unknown target: " + url, e);
                        }
                    }
                };
            }
        };
        @SuppressWarnings("unused")
        Repository clientRepository = repositoryFactory.getRepository(location);
        final ClientRepositoryService clientService = new ClientRepositoryService(remoteRepository.getRepositoryService());
        repository = org.apache.jackrabbit.jcr2spi.HippoRepositoryImpl.create(new RepositoryConfig() {

            public RepositoryService getRepositoryService() throws RepositoryException {
                return clientService;
            }

            public String getDefaultWorkspaceName() {
                return "default";
            }

            public org.apache.jackrabbit.jcr2spi.config.CacheBehaviour getCacheBehaviour() {
                //return org.apache.jackrabbit.jcr2spi.config.CacheBehaviour.OBSERVATION;
                return org.apache.jackrabbit.jcr2spi.config.CacheBehaviour.INVALIDATE;
            }

            public int getItemCacheSize() {
                return 10000;
            }
	    public int getPollTimeout() {
	        return 1000;
	    }
        });
        repository = new DecoratorFactoryImpl(clientService, adapterFactory, clientRepository).getRepositoryDecorator(repository);
    }

    public static HippoRepository create(String location) throws MalformedURLException, NotBoundException, RemoteException,
            RepositoryException {
        if (location.endsWith("/spi")) {
            location = location.substring(0, location.length() - "/spi".length());
        }
        return new SPIHippoRepository(location);
    }
}
