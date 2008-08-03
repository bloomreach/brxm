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
package org.hippoecm.repository.decorating.spi;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.apache.jackrabbit.spi.rmi.client.ClientRepositoryService;

import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.decorating.client.ClientServicesAdapterFactory;

public class DecoratorFactoryImpl extends org.hippoecm.repository.decorating.DecoratorFactoryImpl {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    ClientRepositoryService remoteRepositoryService;
    ClientServicesAdapterFactory adapterFactory;
    Repository repository;

    public DecoratorFactoryImpl(ClientRepositoryService repositoryService, ClientServicesAdapterFactory adapterFactory, Repository repository) {
        this.remoteRepositoryService = repositoryService;
        this.adapterFactory = adapterFactory;
        this.repository = repository;
    }

    public Repository getRepositoryDecorator(Repository repository) {
        return new RepositoryDecorator(this, repository);
    }

    public Session getSessionDecorator(Repository repository, Session session) {
        HippoSession directSession = (HippoSession) adapterFactory.getSession(repository, remoteRepositoryService.getRemoteSession());
        return new SessionDecorator(this, repository, session, directSession);
    }

    public Workspace getWorkspaceDecorator(Session session, Workspace workspace) {
        return new WorkspaceDecorator(this, session, workspace);
    }

    protected Node getBasicNodeDecorator(Session session, Node node) {
        return new NodeDecorator(this, session, node);
    }

    public Version getVersionDecorator(Session session, Version version) {
        return new VersionDecorator(this, session, version);
    }

    public VersionHistory getVersionHistoryDecorator(Session session, VersionHistory versionHistory) {
        return new VersionHistoryDecorator(this, session, versionHistory);
    }

    public Query getQueryDecorator(Session session, Query query) {
        return new QueryDecorator(this, session, query);
    }
}
