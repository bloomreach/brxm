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
package org.hippoecm.repository.decorating.server;

import java.rmi.RemoteException;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.apache.jackrabbit.api.XASession;
import org.apache.jackrabbit.rmi.remote.RemoteIterator;
import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.apache.jackrabbit.rmi.remote.RemoteSession;
import org.apache.jackrabbit.rmi.server.ServerXASession;

import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.decorating.remote.RemoteServicingAdapterFactory;
import org.hippoecm.repository.decorating.remote.RemoteServicingNode;
import org.hippoecm.repository.decorating.remote.RemoteServicingXASession;

public class ServerServicingXASession extends ServerXASession implements RemoteServicingXASession {
    private HippoSession session;

    public ServerServicingXASession(XASession session, RemoteServicingAdapterFactory factory) throws RemoteException {
        super(session, factory);
        this.session = (HippoSession) session;
    }

    public RemoteNode copy(String originalPath, String absPath) throws RepositoryException, RemoteException {
        try {
            return getRemoteNode(session.copy(session.getRootNode().getNode(originalPath), absPath));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    public RemoteIterator pendingChanges(String absPath, String nodeType, boolean prune) throws NamespaceException,
                                                                 NoSuchNodeTypeException, RepositoryException, RemoteException {
        try {
            return getFactory().getRemoteNodeIterator(session.pendingChanges(session.getRootNode().getNode(absPath.substring(1)),
                                                                             nodeType, prune));
        } catch (NamespaceException ex) {
            throw getRepositoryException(ex);
        } catch (NoSuchNodeTypeException ex) {
            throw getRepositoryException(ex);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }
}
