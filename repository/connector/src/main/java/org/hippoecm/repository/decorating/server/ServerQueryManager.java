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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryManager;

import org.apache.jackrabbit.rmi.remote.RemoteQuery;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;

public class ServerQueryManager extends org.apache.jackrabbit.rmi.server.ServerQueryManager
{

    protected Session session;
    protected QueryManager manager;

    public ServerQueryManager(QueryManager manager, ServerAdapterFactory factory, Session session) throws RemoteException {
        super(session, manager, factory);
        this.session = session;
        this.manager = manager;
    }

    public RemoteQuery getQuery(String absPath) throws RepositoryException, RemoteException {
        try {
            Node node = session.getRootNode().getNode(absPath.substring(1));
            return getFactory().getRemoteQuery(manager.getQuery(node));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }
}
