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
package org.hippoecm.hst.core.jcr.pool;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.apache.commons.beanutils.ConstructorUtils;
import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.lang.ClassUtils;
import org.hippoecm.repository.HippoRepositoryImpl;
import org.hippoecm.repository.decorating.client.ClientServicesAdapterFactory;

/**
 * JCR Repository implementation wrapping HippoRepository.
 * <P>
 * This implementation creates a HippoRepository connecting via RMI internally
 * because the current <CODE>HippoRepositoryFactory</CODE> does not support shared accesses
 * to a remote repository deployed in a separate web application.
 * </P>
 * 
 * @version $Id$
 */
public class JcrRemoteHippoRepositoryProvider extends JcrHippoRepositoryProvider {
    
    @Override
    public Repository getRepository(String repositoryURI) throws RepositoryException {
        if (repositoryURI != null && repositoryURI.startsWith("rmi://") && !repositoryURI.endsWith("/spi")) {
            try {
                return new JcrHippoRepository(new RemoteHippoRepository(repositoryURI));
            } catch (Exception e) {
                throw new RepositoryException(e);
            }
        } else {
            return super.getRepository(repositoryURI);
        }
    }
    
    private class RemoteHippoRepository extends HippoRepositoryImpl {
        @SuppressWarnings("unused")
        public RemoteHippoRepository(String location) throws MalformedURLException, NotBoundException, RemoteException, RepositoryException {
            try {
                ClientServicesAdapterFactory adapterFactory = new ClientServicesAdapterFactory();
                Class repositoryFactoryClass = ClassUtils.getClass("org.apache.jackrabbit.rmi.client.ClientRepositoryFactory");
                Object repositoryFactory = ConstructorUtils.invokeConstructor(repositoryFactoryClass, adapterFactory);
                repository = (Repository) MethodUtils.invokeMethod(repositoryFactory, "getRepository", location);
            } catch (Exception e) {
                throw new RepositoryException(e);
            }
        }
    }
}
