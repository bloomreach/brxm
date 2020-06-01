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
package org.hippoecm.repository.decorating.client;

import java.rmi.RemoteException;
import java.util.Map;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.rmi.client.RemoteRepositoryException;

import org.hippoecm.repository.api.HippoQuery;
import org.hippoecm.repository.decorating.remote.RemoteQuery;

public class ClientQuery extends org.apache.jackrabbit.rmi.client.ClientQuery implements HippoQuery {

    private RemoteQuery remote;
    private Session session;

    protected ClientQuery(Session session, RemoteQuery remote, LocalServicingAdapterFactory factory) {
        super(session, remote, factory);
        this.session = session;
        this.remote = remote;
    }

    public Session getSession() {
        return session;
    }

    public Node storeAsNode(String absPath, String type) throws ItemExistsException, PathNotFoundException, VersionException,
            ConstraintViolationException, LockException, UnsupportedRepositoryOperationException, RepositoryException {
        try {
            String path = remote.storeAsNode(absPath, type);
            if(path != null && path.length() > 0) {
                if(path.equals("/"))
                    return session.getRootNode();
                else
                    return session.getRootNode().getNode(path.substring(1));
            } else
                return null;
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public String[] getArguments() throws RepositoryException {
        try {
            return remote.getArguments();
        } catch(RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public int getArgumentCount() throws RepositoryException {
        try {
            return remote.getArgumentCount();
        } catch(RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public QueryResult execute(Map<String,String> arguments) throws RepositoryException {
        try {
            return getFactory().getQueryResult(session, remote.execute(arguments));
        } catch(RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public void bindValue(String varName, Value value)
            throws RepositoryException {
        try {
            remote.bindValue(varName, value);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public String[] getBindVariableNames() throws RepositoryException {
        try {
            return remote.getBindVariableNames();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public void setLimit(long limit) {
        try {
            remote.setLimit(limit);
        } catch (RemoteException ex) {
            // this is in case of a connection problem, the actual call to execute() will fail next
        }
    }

    public void setOffset(long offset){
        try {
            remote.setOffset(offset);
        } catch (RemoteException ex) {
            // this is in case of a connection problem, the actual call to execute() will fail next
        }
    }
}
