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
package org.hippoecm.repository.decorating.server;

import java.rmi.RemoteException;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import javax.jcr.Session;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.rmi.remote.RemoteRepositoryService;
import org.apache.jackrabbit.spi.rmi.server.ServerRepositoryService;
import org.apache.jackrabbit.spi2jcr.BatchReadConfig;
import org.apache.jackrabbit.spi2jcr.RepositoryServiceImpl;
import org.hippoecm.repository.decorating.remote.RemoteRepository;

public class ServerRepository extends org.apache.jackrabbit.rmi.server.ServerRepository implements RemoteRepository {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private Repository repository;
    private RemoteServicingAdapterFactory factory;

    public ServerRepository(Repository repository, RemoteServicingAdapterFactory factory) throws RemoteException {
        super(repository, factory);
        this.repository = repository;
        this.factory = factory;
    }

    public RemoteRepositoryService getRepositoryService() throws RepositoryException, RemoteException {
        final ServerRepositoryService serverService = new ServerRepositoryService();
        BatchReadConfig cfg = new BatchReadConfig();
        cfg.setDepth(NameFactoryImpl.getInstance().create("internal", "root"), 3);
        cfg.setDepth(NameFactoryImpl.getInstance().create("http://www.jcp.org/jcr/nt/1.0", "unstructured"), 3);
        cfg.setDepth(NameFactoryImpl.getInstance().create("http://www.onehippo.org/jcr/hippostd/nt/2.0", "folder"), 3);
        cfg.setDepth(NameFactoryImpl.getInstance().create("http://www.onehippo.org/jcr/hippostd/nt/2.0", "gallery"), 3);
        cfg.setDepth(NameFactoryImpl.getInstance().create("http://www.onehippo.org/jcr/hippostd/nt/2.0", "directory"), 3);
        cfg.setDepth(NameFactoryImpl.getInstance().create("http://www.onehippo.org/jcr/hippostd/nt/2.0", "fixeddirectory"), 3);
        cfg.setDepth(NameFactoryImpl.getInstance().create("http://www.hippoecm.org/defaultcontent/1.2", "article"), -1);
        cfg.setDepth(NameFactoryImpl.getInstance().create("http://www.hippoecm.org/defaultcontent/1.2", "news"), -1);
        cfg.setDepth(NameFactoryImpl.getInstance().create("http://www.hippoecm.org/defaultcontent/1.2", "event"), -1);
        cfg.setDepth(NameFactoryImpl.getInstance().create("http://www.hippoecm.org/defaultcontent/1.2", "overview"), -1);
        cfg.setDepth(NameFactoryImpl.getInstance().create("http://www.onehippo.org/jcr/hippo/nt/2.0", "document"), 3);
        cfg.setDepth(NameFactoryImpl.getInstance().create("http://www.onehippo.org/jcr/hippogallery/nt/2.0", "exampleImageSet"), 3);
        cfg.setDepth(NameFactoryImpl.getInstance().create("http://www.onehippo.org/jcr/hippogallery/nt/2.0", "exampleAssetSet"), 3);
        cfg.setDepth(NameFactoryImpl.getInstance().create("http://www.onehippo.org/jcr/hippogallery/nt/2.0", "stdgalleryset"), 3);
        cfg.setDepth(NameFactoryImpl.getInstance().create("http://www.onehippo.org/jcr/hippogallery/nt/2.0", "stdImageGallery"), 3);
        cfg.setDepth(NameFactoryImpl.getInstance().create("http://www.onehippo.org/jcr/hippogallery/nt/2.0", "stdAssetGallery"), 3);
        cfg.setDepth(NameFactoryImpl.getInstance().create("http://www.hippoecm.org/hippolog/nt/1.3", "item"), 3);
        cfg.setDepth(NameFactoryImpl.getInstance().create("http://www.hippoecm.org/frontend/nt/1.4", "plugincluster"), -1);
        cfg.setDepth(NameFactoryImpl.getInstance().create("http://www.hippoecm.org/frontend/nt/1.4", "plugin"), -1);
        cfg.setDepth(NameFactoryImpl.getInstance().create("http://www.hippoecm.org/frontend/nt/1.4", "pluginconfig"), -1);
        cfg.setDepth(NameFactoryImpl.getInstance().create("http://www.hippoecm.org/frontend/nt/1.4", "clusterfolder"), 1);
        cfg.setDepth(NameFactoryImpl.getInstance().create("http://www.hippoecm.org/frontend/nt/1.4", "application"), 1);

        Repository loginRepository = new Repository() {

            public Session login() throws LoginException, RepositoryException {
                Session newSession = ServerRepository.this.repository.login();
                try {
                    serverService.setRemoteSession(factory.getRemoteSession(newSession));
                } catch (RemoteException ex) {
                    throw new RepositoryException(ex);
                }

                return newSession;
            }

            public Session login(String workspace) throws LoginException, NoSuchWorkspaceException, RepositoryException {
                Session newSession = ServerRepository.this.repository.login(workspace);
                try {
                    serverService.setRemoteSession(factory.getRemoteSession(newSession));
                } catch (RemoteException ex) {
                    throw new RepositoryException(ex);
                }

                return newSession;
            }

            public Session login(Credentials credentials) throws LoginException, RepositoryException {
                Session newSession = ServerRepository.this.repository.login(credentials);
                try {
                    serverService.setRemoteSession(factory.getRemoteSession(newSession));
                } catch (RemoteException ex) {
                    throw new RepositoryException(ex);
                }

                return newSession;
            }

            public Session login(Credentials credentials, String workspace) throws LoginException, NoSuchWorkspaceException, RepositoryException {
                Session newSession = ServerRepository.this.repository.login(credentials, workspace);
                try {
                    serverService.setRemoteSession(factory.getRemoteSession(newSession));
                } catch (RemoteException ex) {
                    throw new RepositoryException(ex);
                }
                return newSession;
            }

            public String getDescriptor(String name) {
                return ServerRepository.this.repository.getDescriptor(name);
            }

            public String[] getDescriptorKeys() {
                return ServerRepository.this.repository.getDescriptorKeys();
            }
        };
        RepositoryService repositoryService = new RepositoryServiceImpl(loginRepository, cfg);
        serverService.setRepositoryService(repositoryService);
        return serverService;
    }
}
